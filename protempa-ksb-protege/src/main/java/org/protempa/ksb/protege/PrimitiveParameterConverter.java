/*
 * #%L
 * Protempa Protege Knowledge Source Backend
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
package org.protempa.ksb.protege;

import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Instance;
import org.protempa.*;
import org.protempa.proposition.value.ValueType;

/**
 * @author Andrew Post
 */
class PrimitiveParameterConverter implements PropositionConverter {

    /**
     *
     */
    PrimitiveParameterConverter() {
    }

    @Override
    public PrimitiveParameterDefinition convert(Instance instance,
            org.protempa.KnowledgeBase protempaKnowledgeBase,
            ProtegeKnowledgeSourceBackend backend)
            throws KnowledgeSourceReadException {
        PrimitiveParameterDefinition result = new PrimitiveParameterDefinition(
                protempaKnowledgeBase, instance.getName());
        ConnectionManager cm = backend.getConnectionManager();
        Util.setNames(instance, result, cm);
        Util.setInDataSource(instance, result, cm);
        Util.setInverseIsAs(instance, result, cm);
        Util.setProperties(instance, result, cm);
        Util.setTerms(instance, result, cm);
        Util.setReferences(instance, result, cm);
        Cls valueType = (Cls) cm.getOwnSlotValue(instance, cm.getSlot("valueType"));
        if (valueType != null) {
            result.setValueType(Util.parseValueType(valueType));
        }
        result.setSourceId(
                DefaultSourceId.getInstance(backend.getDisplayName()));
        return result;
    }

    @Override
    public PropositionDefinition readPropositionDefinition(
            Instance protegeProposition, KnowledgeBase protempaKnowledgeBase) {
        return protempaKnowledgeBase.getPrimitiveParameterDefinition(
                protegeProposition.getName());
    }
}
