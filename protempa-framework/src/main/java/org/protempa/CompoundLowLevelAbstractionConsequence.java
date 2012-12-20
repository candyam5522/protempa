/*
 * #%L
 * Protempa Framework
 * %%
 * Copyright (C) 2012 Emory University
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.drools.WorkingMemory;
import org.drools.spi.Consequence;
import org.drools.spi.KnowledgeHelper;
import org.protempa.CompoundLowLevelAbstractionDefinition.ValueDefinitionMatchOperator;
import org.protempa.proposition.AbstractParameter;
import org.protempa.proposition.CompoundValuedInterval;
import org.protempa.proposition.CompoundValuedIntervalFactory;
import org.protempa.proposition.DerivedSourceId;
import org.protempa.proposition.DerivedUniqueId;
import org.protempa.proposition.UniqueId;
import org.protempa.proposition.interval.Interval;
import org.protempa.proposition.interval.IntervalFactory;
import org.protempa.proposition.value.NominalValue;
import org.protempa.proposition.value.Value;

/**
 * Drools consequence for {@link CompoundLowLevelAbstractionDefinition}s.
 */
public final class CompoundLowLevelAbstractionConsequence implements
        Consequence {

    private static final long serialVersionUID = 6456351279290509422L;

    private static final IntervalFactory intervalFactory = new IntervalFactory();

    private final CompoundLowLevelAbstractionDefinition cllad;
    private final DerivationsBuilder derivationsBuilder;

    /**
     * Constructor.
     * 
     * @param def
     *            the {@link CompoundLowLevelAbstractionDefinition} this is a
     *            consequence for
     * @param derivationsBuilder
     *            the {@link DerivationsBuilder} to add asserted propositions to
     */
    public CompoundLowLevelAbstractionConsequence(
            CompoundLowLevelAbstractionDefinition def,
            DerivationsBuilder derivationsBuilder) {
        assert def != null : "def cannot be null";
        this.cllad = def;
        this.derivationsBuilder = derivationsBuilder;
    }

    private void assertDerivedProposition(WorkingMemory workingMemory,
            AbstractParameter derived, Set<AbstractParameter> sources) {
        workingMemory.insert(derived);
        for (AbstractParameter parameter : sources) {
            derivationsBuilder.propositionAsserted(parameter, derived);
        }
        ProtempaUtil.logger().log(Level.FINER,
                "Asserted derived proposition {0}", derived);
    }

    private final static class AbstractParameterWithSourceParameters {
        final AbstractParameter parameter;
        final Set<AbstractParameter> sourceParameters;

        public AbstractParameterWithSourceParameters(
                AbstractParameter parameter,
                Set<AbstractParameter> sourcePropositions) {
            this.parameter = parameter;
            this.sourceParameters = sourcePropositions;
        }
    }

    @Override
    public void evaluate(KnowledgeHelper knowledgeHelper,
            WorkingMemory workingMemory) throws Exception {
        @SuppressWarnings("unchecked")
        List<AbstractParameter> pl = (List<AbstractParameter>) knowledgeHelper
                .get(knowledgeHelper.getDeclaration("result"));

        List<CompoundValuedInterval> intervals = CompoundValuedIntervalFactory
                .buildIntervalList(pl);

        List<AbstractParameterWithSourceParameters> derivedProps = new ArrayList<AbstractParameterWithSourceParameters>();
        for (CompoundValuedInterval interval : intervals) {
            boolean match = false;
            String lastCheckedValue = null;
            for (Entry<String, Map<String, Value>> e : cllad
                    .getValueClassifications().entrySet()) {
                lastCheckedValue = e.getKey();
                switch (cllad.getValueDefinitionMatchOperator()) {
                    case ALL:
                        match = allMatch(interval, e.getValue());
                        break;
                    case ANY:
                        match = anyMatch(interval, e.getValue());
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "valueDefinitionMatchStrategy must by ALL or ANY");
                }
                if (match) {
                    AbstractParameter result = createAbstractParameter(
                            cllad.getId(),
                            NominalValue.getInstance(e.getKey()),
                            interval.getInterval());
                    derivedProps.add(new AbstractParameterWithSourceParameters(
                            result, interval.getParameters()));
                    // found a matching value, so don't look for any more and
                    // move on to the next interval
                    break;
                }
            }
            // if none of the classifications matched and this is an ALL
            // situation, then default to the last classification defined
            if (!match
                    && cllad.getValueDefinitionMatchOperator() == ValueDefinitionMatchOperator.ALL
                    && lastCheckedValue != null) {
                AbstractParameter result = createAbstractParameter(
                        cllad.getId(),
                        NominalValue.getInstance(lastCheckedValue),
                        interval.getInterval());
                derivedProps.add(new AbstractParameterWithSourceParameters(
                        result, interval.getParameters()));
            }
        }
        if (cllad.getMinimumNumberOfValues() <= 1) {
            for (AbstractParameterWithSourceParameters param : derivedProps) {
                assertDerivedProposition(workingMemory, param.parameter,
                        param.sourceParameters);
            }
        } else {
            // if we need to match multiple consecutive values, and if such a
            // match is present, then the new interval ranges from the start of
            // the first match to the end of the last match
            for (int i = 0; i < derivedProps.size(); i++) {
                if (i + cllad.getMinimumNumberOfValues() - 1 < derivedProps
                        .size()) {
                    if (rangeMatches(derivedProps, i,
                            i + cllad.getMinimumNumberOfValues() - 1,
                            cllad.getGapFunction())) {
                        AbstractParameter startParam = derivedProps.get(i).parameter;
                        AbstractParameter finishParam = derivedProps.get(i
                                + cllad.getMinimumNumberOfValues() - 1).parameter;
                        Interval interval = intervalFactory.getInstance(
                                startParam.getInterval().getMinStart(),
                                startParam.getInterval().getStartGranularity(),
                                finishParam.getInterval().getMaxFinish(),
                                finishParam.getInterval()
                                        .getFinishGranularity());
                        AbstractParameter result = createAbstractParameter(
                                cllad.getId(), startParam.getValue(), interval);
                        assertDerivedProposition(workingMemory, result,
                                derivedProps.get(i).sourceParameters);
                    }
                }
            }
        }
    }

    private boolean allMatch(CompoundValuedInterval multiInterval,
            Map<String, Value> lowLevelValueDefs) {
        for (Entry<String, Value> e : multiInterval.getValues().entrySet()) {
            String id = e.getKey();
            Value val = e.getValue();

            if (!val.equals(lowLevelValueDefs.get(id))) {
                return false;
            }
        }
        return true;
    }

    private boolean anyMatch(CompoundValuedInterval multiInterval,
            Map<String, Value> lowLevelValueDefs) {
        for (Entry<String, Value> e : multiInterval.getValues().entrySet()) {
            String id = e.getKey();
            Value val = e.getValue();

            if (val.equals(lowLevelValueDefs.get(id))) {
                return true;
            }
        }
        return false;
    }

    private static AbstractParameter createAbstractParameter(String propId,
            Value value, Interval interval) {
        AbstractParameter result = new AbstractParameter(propId, new UniqueId(
                DerivedSourceId.getInstance(), new DerivedUniqueId(UUID
                        .randomUUID().toString())));
        result.setInterval(interval);
        result.setValue(value);
        result.setDataSourceType(DerivedDataSourceType.getInstance());

        return result;
    }

    private boolean rangeMatches(
            List<AbstractParameterWithSourceParameters> propositions,
            int rangeStart, int rangeEnd, GapFunction gf) {
        Value value = propositions.get(rangeStart).parameter.getValue();
        for (int i = rangeStart + 1; i <= rangeEnd; i++) {
            AbstractParameter prop = propositions.get(i).parameter;
            if (!value.equals(prop.getValue())
                    || !gf.execute(propositions.get(i - 1).parameter, prop)) {
                return false;
            }
        }
        return true;
    }
}
