package org.protempa;

/*
 * #%L
 * Protempa Framework
 * %%
 * Copyright (C) 2012 - 2015 Emory University
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.arp.javautil.collections.Iterators;
import org.protempa.dest.Destination;
import org.protempa.dest.QueryResultsHandler;
import org.protempa.dest.QueryResultsHandlerCloseException;
import org.protempa.dest.QueryResultsHandlerInitException;
import org.protempa.dest.QueryResultsHandlerProcessingException;
import org.protempa.dest.QueryResultsHandlerValidationFailedException;
import org.protempa.proposition.Proposition;
import org.protempa.proposition.UniqueId;
import org.protempa.query.Query;

/**
 *
 * @author Andrew Post
 */
abstract class ExecutorWithResultsHandler extends Executor {

    private final Destination destination;
    private QueryResultsHandler resultsHandler;
    private final AbstractionFinder abstractionFinder;
    private boolean failed;

    private final BlockingQueue<QueueObject> queue = new ArrayBlockingQueue<>(1000);
    private final QueueObject poisonPill = new QueueObject();
    private final HandleQueryResultsThread handleQueryResultsThread;

    public ExecutorWithResultsHandler(Query query, Destination resultsHandlerFactory, QuerySession querySession, ExecutorStrategy strategy, AbstractionFinder abstractionFinder) throws FinderException {
        super(query, querySession, strategy, abstractionFinder);
        assert resultsHandlerFactory != null : "resultsHandlerFactory cannot be null";
        this.destination = resultsHandlerFactory;
        this.abstractionFinder = abstractionFinder;
        this.handleQueryResultsThread = new HandleQueryResultsThread();
    }

    @Override
    DataStreamingEventIterator<Proposition> newDataIterator() throws KnowledgeSourceReadException, DataSourceReadException {
        log(Level.INFO, "Retrieving data for query {0}", getQuery().getId());
        Set<String> inDataSourcePropIds = new HashSet<>();
        for (PropositionDefinition pd : getAllNarrowerDescendants()) {
            if (pd.getInDataSource()) {
                inDataSourcePropIds.add(pd.getId());
            }
        }
        if (isLoggable(Level.FINER)) {
            log(Level.FINER, "Asking data source for {0} for query {1}", new Object[]{StringUtils.join(inDataSourcePropIds, ", "), getQuery().getId()});
        }
        DataStreamingEventIterator<Proposition> itr = this.abstractionFinder.getDataSource().readPropositions(getKeyIds(), inDataSourcePropIds, getFilters(), getQuerySession(), this.resultsHandler);
        return itr;
    }

    @Override
    void init() throws FinderException {
        String queryId = getQuery().getId();
        log(Level.FINE, "Initializing query results handler for query {0}", queryId);
        try {
            this.resultsHandler = this.destination.getQueryResultsHandler(getQuery(), this.abstractionFinder.getDataSource(), getKnowledgeSource());
            log(Level.FINE, "Done initalizing query results handler for query {0}", queryId);
            log(Level.FINE, "Validating query results handler for query {0}", queryId);
            try {
                this.resultsHandler.validate();
            } catch (QueryResultsHandlerValidationFailedException ex) {
                throw new QueryResultsHandlerInitException(ex);
            }
            log(Level.FINE, "Query results handler validated successfully for query {0}", queryId);
            addAllPropIds(resultsHandler.getPropositionIdsNeeded());
            try {
                super.init();
            } catch (FinderException fe) {
                this.failed = true;
                throw fe;
            }
            this.resultsHandler.start(getAllNarrowerDescendants());
            this.handleQueryResultsThread.start();
        } catch (QueryResultsHandlerInitException | QueryResultsHandlerProcessingException | Error | RuntimeException ex) {
            this.failed = true;
            throw new FinderException(getQuery().getId(), ex);
        }
    }

    @Override
    void execute() throws FinderException {
        try {
            super.execute();
        } catch (FinderException ex) {
            this.failed = true;
            throw ex;
        }
    }

    @Override
    public void close() throws FinderException {
        try {
            try {
                this.queue.put(this.poisonPill);
            } catch (InterruptedException ex) {
                log(Level.FINER, "Handle query results handler thread interrupted", ex);
            }
            try {
                this.handleQueryResultsThread.join();
            } catch (InterruptedException ex) {
                log(Level.FINER, "Handle query results handler thread interrupted", ex);
            }
            
            Throwable throwable = this.handleQueryResultsThread.getThrowable();
            if (throwable != null) {
                throw throwable;
            }
            
            // Might be null if init() fails.
            if (this.resultsHandler != null) {
                if (!this.failed) {
                    this.resultsHandler.finish();
                }
                this.resultsHandler.close();
                this.resultsHandler = null;
            }
        } catch (Throwable ex) {
            throw new FinderException(getQuery().getId(), ex);
        } finally {
            if (this.resultsHandler != null) {
                try {
                    this.resultsHandler.close();
                } catch (QueryResultsHandlerCloseException ignore) {

                }
            }
        }
    }

    final void processResults(Iterator<Proposition> propositions, DerivationsBuilder derivationsBuilder, String keyId) throws FinderException {
        Throwable throwable = this.handleQueryResultsThread.getThrowable();
        if (throwable != null) {
            throw new FinderException(getQuery().getId(), throwable);
        }
        if (derivationsBuilder == null) {
            derivationsBuilder = getDerivationsBuilder();
        }
        Map<Proposition, List<Proposition>> forwardDerivations = derivationsBuilder.toForwardDerivations();
        Map<Proposition, List<Proposition>> backwardDerivations = derivationsBuilder.toBackwardDerivations();
        Set<String> propositionIds = getPropIds();
        QuerySession qs = getQuerySession();
        if (qs.isCachingEnabled()) {
            List<Proposition> props = Iterators.asList(propositions);
            addToCache(qs, Collections.unmodifiableList(props), Collections.unmodifiableMap(forwardDerivations), Collections.unmodifiableMap(backwardDerivations));
            propositions = props.iterator();
        }
        Map<UniqueId, Proposition> refs = new HashMap<>();
        if (isLoggable(Level.FINER)) {
            log(Level.FINER, "References for query {0}: {1}", new Object[]{getQuery().getId(), refs});
        }
        try {
            List<Proposition> filteredPropositions = extractRequestedPropositions(propositions, propositionIds, refs);
            if (isLoggable(Level.FINER)) {
                String queryId = getQuery().getId();
                log(Level.FINER, "Proposition ids for query {0}: {1}", new Object[]{queryId, propositionIds});
                log(Level.FINER, "Filtered propositions for query {0}: {1}", new Object[]{queryId, filteredPropositions});
                log(Level.FINER, "Forward derivations for query {0}: {1}", new Object[]{queryId, forwardDerivations});
                log(Level.FINER, "Backward derivations for query {0}: {1}", new Object[]{queryId, backwardDerivations});
            }
            queue.put(new QueueObject(keyId, filteredPropositions, forwardDerivations, backwardDerivations, refs));
        } catch (InterruptedException ex) {
            log(Level.FINER, "Process results put on queue thread", ex);
        }
    }

    final void processResults(Iterator<Proposition> propositions, String keyId) throws FinderException {
        processResults(propositions, null, keyId);
    }

    private class HandleQueryResultsThread extends Thread {

        private volatile Throwable throwable;

        @Override
        public void run() {
            QueueObject qo;
            try {
                while (!isInterrupted() && ((qo = queue.take()) != poisonPill)) {
                    try {
                        resultsHandler.handleQueryResult(qo.keyId, qo.propositions, qo.forwardDerivations, qo.backwardDerivations, qo.refs);
                    } catch (QueryResultsHandlerProcessingException | Error | RuntimeException t) {
                        this.throwable = t;
                        break;
                    }
                }
            } catch (InterruptedException ex) {
                log(Level.FINER, "Handle query results thread interrupted", ex);
            }
        }

        public Throwable getThrowable() {
            Throwable result = this.throwable;
            this.throwable = null;
            return result;
        }

    };

    private static final class QueueObject {

        List<Proposition> propositions;
        Map<Proposition, List<Proposition>> forwardDerivations;
        Map<Proposition, List<Proposition>> backwardDerivations;
        String keyId;
        Map<UniqueId, Proposition> refs;

        QueueObject(String keyId, List<Proposition> propositions, Map<Proposition, List<Proposition>> forwardDerivations, Map<Proposition, List<Proposition>> backwardDerivations, Map<UniqueId, Proposition> refs) {
            this.propositions = propositions;
            this.forwardDerivations = forwardDerivations;
            this.backwardDerivations = backwardDerivations;
            this.keyId = keyId;
            this.refs = refs;
        }

        QueueObject() {

        }
    }

    private static List<Proposition> extractRequestedPropositions(
            Iterator<Proposition> propositions, Set<String> propositionIds,
            Map<UniqueId, Proposition> refs) {
        List<Proposition> result = new ArrayList<>();
        while (propositions.hasNext()) {
            Proposition prop = propositions.next();
            refs.put(prop.getUniqueId(), prop);
            if (propositionIds.contains(prop.getId())) {
                result.add(prop);
            }
        }
        return result;
    }

    private static void addToCache(QuerySession qs,
            List<Proposition> propositions,
            Map<Proposition, List<Proposition>> forwardDerivations,
            Map<Proposition, List<Proposition>> backwardDerivations) {
        qs.addPropositionsToCache(propositions);
        for (Map.Entry<Proposition, List<Proposition>> me
                : forwardDerivations.entrySet()) {
            qs.addDerivationsToCache(me.getKey(), me.getValue());
        }
        for (Map.Entry<Proposition, List<Proposition>> me
                : backwardDerivations.entrySet()) {
            qs.addDerivationsToCache(me.getKey(), me.getValue());
        }
    }

}