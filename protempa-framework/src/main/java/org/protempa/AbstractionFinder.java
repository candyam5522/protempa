/*
 * #%L
 * Protempa Framework
 * %%
 * Copyright (C) 2012 - 2013 Emory University
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
package org.protempa;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.drools.StatefulSession;
import org.protempa.dest.Destination;
import org.protempa.query.Query;
import org.protempa.query.QueryBuildException;
import org.protempa.query.QueryBuilder;

/**
 * Class that actually does the abstraction finding.
 *
 * @author Andrew Post
 */
final class AbstractionFinder {

    private final Map<String, StatefulSession> workingMemoryCache;
    private final DataSource dataSource;
    private final KnowledgeSource knowledgeSource;
    private final TermSource termSource;
    private final AlgorithmSource algorithmSource;
    // private final Map<String, List<String>> termToPropDefMap;
    private boolean closed;
    private Executor executor;
    private final List<ProtempaEventListener> eventListeners;

    AbstractionFinder(DataSource dataSource, KnowledgeSource knowledgeSource,
            AlgorithmSource algorithmSource, TermSource termSource,
            boolean cacheFoundAbstractParameters, 
            List<ProtempaEventListener> eventListeners)
            throws KnowledgeSourceReadException {
        assert dataSource != null : "dataSource cannot be null";
        assert knowledgeSource != null : "knowledgeSource cannot be null";
        assert algorithmSource != null : "algorithmSource cannot be null";
        assert termSource != null : "termSource cannot be null";
        assert eventListeners != null : "eventListeners cannot be null";

        this.dataSource = dataSource;
        this.knowledgeSource = knowledgeSource;
        this.termSource = termSource;
        this.algorithmSource = algorithmSource;
        
        this.eventListeners = eventListeners;
        
        this.dataSource.setEventListeners(eventListeners);
        this.knowledgeSource.setEventListeners(eventListeners);
        this.termSource.setEventListeners(eventListeners);
        this.algorithmSource.setEventListeners(eventListeners);

        this.dataSource.addSourceListener(
                new SourceListener<DataSourceUpdatedEvent>() {
            @Override
            public void sourceUpdated(DataSourceUpdatedEvent event) {
            }

            @Override
            public void closedUnexpectedly(
                    SourceClosedUnexpectedlyEvent e) {
                throw new UnsupportedOperationException(
                        "Not supported yet.");
            }
        });

        this.knowledgeSource.addSourceListener(
                new SourceListener<KnowledgeSourceUpdatedEvent>() {
            @Override
            public void sourceUpdated(KnowledgeSourceUpdatedEvent event) {
            }

            @Override
            public void closedUnexpectedly(
                    SourceClosedUnexpectedlyEvent e) {
                throw new UnsupportedOperationException(
                        "Not supported yet.");
            }
        });

        this.termSource.addSourceListener(
                new SourceListener<TermSourceUpdatedEvent>() {
            @Override
            public void sourceUpdated(TermSourceUpdatedEvent event) {
            }

            @Override
            public void closedUnexpectedly(
                    SourceClosedUnexpectedlyEvent e) {
                throw new UnsupportedOperationException(
                        "Not supported yet");
            }
        });

        this.algorithmSource.addSourceListener(
                new SourceListener<AlgorithmSourceUpdatedEvent>() {
            @Override
            public void sourceUpdated(AlgorithmSourceUpdatedEvent event) {
            }

            @Override
            public void closedUnexpectedly(
                    SourceClosedUnexpectedlyEvent e) {
                throw new UnsupportedOperationException(
                        "Not supported yet.");
            }
        });

        if (cacheFoundAbstractParameters) {
            this.workingMemoryCache = new HashMap<>();
        } else {
            this.workingMemoryCache = null;
        }
    }

    List<ProtempaEventListener> getEventListeners() {
        return eventListeners;
    }
    
    DataSource getDataSource() {
        return this.dataSource;
    }

    KnowledgeSource getKnowledgeSource() {
        return this.knowledgeSource;
    }

    AlgorithmSource getAlgorithmSource() {
        return this.algorithmSource;
    }

    TermSource getTermSource() {
        return this.termSource;
    }

    Set<String> getKnownKeys() {
        if (workingMemoryCache != null) {
            return Collections.unmodifiableSet(workingMemoryCache.keySet());
        } else {
            return Collections.emptySet();
        }
    }

    void doFind(Query query, Destination destination)
            throws QueryException {
        assert destination != null : "destination cannot be null";
        ExecutorStrategy strategy;
        try {
            if (!hasSomethingToAbstract(query)) {
                strategy = null;
            } else if (workingMemoryCache != null) {
                strategy = ExecutorStrategy.STATEFUL;
            } else {
                strategy = ExecutorStrategy.STATELESS;
            }
        } catch (QueryValidationException ex) {
            throw new QueryException(query.getName(), ex);
        }
        try {
            this.executor = new Executor(query, destination, strategy, this);
            this.executor.init();
            this.executor.execute();
            this.executor.close();
            this.executor = null;
        } catch (CloseException ex) {
            this.executor = null; //Don't try closing it again below
            throw new QueryException(query.getName(), ex);
        } finally {
            if (this.executor != null) {
                try {
                    this.executor.close();
                } catch (CloseException ignored) {
                }
            }
        }
    }

    void cancel() {
        if (this.executor != null) {
            this.executor.cancel();
        }
    }

    private boolean hasSomethingToAbstract(Query query) throws QueryValidationException {
        try {
            if (!this.knowledgeSource.readAbstractionDefinitions(query.getPropositionIds()).isEmpty()
                    || !this.knowledgeSource.readContextDefinitions(query.getPropositionIds()).isEmpty()) {
                return true;
            }
            for (PropositionDefinition propDef : query.getPropositionDefinitions()) {
                if (propDef instanceof AbstractionDefinition || propDef instanceof ContextDefinition) {
                    return true;
                }
            }
            return false;
        } catch (KnowledgeSourceReadException ex) {
            throw new QueryValidationException("Invalid proposition id(s) " + StringUtils.join(query.getPropositionIds(), ", "), ex);
        }
    }

    Query buildQuery(QueryBuilder queryBuilder) throws QueryBuildException {
        return queryBuilder.build(this.knowledgeSource, this.algorithmSource);
    }

    /**
     * Clears the working memory cache. Only needs to be called in caching mode.
     */
    void clear() {
        if (workingMemoryCache != null) {
            for (Iterator<StatefulSession> itr
                    = workingMemoryCache.values().iterator(); itr.hasNext();) {
                try {
                    itr.next().dispose();
                    itr.remove();
                } catch (Exception e) {
                    ProtempaUtil.logger().log(Level.SEVERE,
                            "Could not dispose stateful rule session", e);
                }
            }
        }
    }

    void close() throws CloseException {
        CloseException exception = null;
        clear();
        boolean algorithmSourceClosed = false;
        boolean knowledgeSourceClosed = false;
        boolean termSourceClosed = false;

        try {
            this.algorithmSource.close();
            algorithmSourceClosed = true;
            this.knowledgeSource.close();
            knowledgeSourceClosed = true;
            this.termSource.close();
            termSourceClosed = true;
        } catch (SourceCloseException e) {
            exception = new CloseException(e);
        } finally {
            if (!algorithmSourceClosed) {
                try {
                    this.algorithmSource.close();
                } catch (SourceCloseException ignored) {
                }
            }
            if (!knowledgeSourceClosed) {
                try {
                    this.knowledgeSource.close();
                } catch (SourceCloseException ignored) {
                }
            }
            if (!termSourceClosed) {
                try {
                    this.termSource.close();
                } catch (SourceCloseException ignored) {
                }
            }
        }
        try {
            this.dataSource.close();
        } catch (SourceCloseException ex) {
            if (exception == null) {
                exception = new CloseException(ex);
            } else {
                exception.addSuppressed(ex);
            }
        }
        if (exception != null) {
            throw exception;
        }
        this.closed = true;
    }

    boolean isClosed() {
        return this.closed;
    }

}
