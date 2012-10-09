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

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * A collection of primitive parameter definitions, abstract parameter
 * definitions, and key finding definitions. Primitive parameters are raw data
 * types. Abstract Parameters are abstractions inferred from raw data. Key
 * findings are aggregations of clinical data around found abstract parameters.
 * 
 * TODO add context types.
 * 
 * @author Andrew Post
 */
public final class PropositionDefinitionCache implements Serializable {

    private static final long serialVersionUID = 5988857805118255882L;

    /**
     * Map of abstract parameter id <code>String</code> objects to
     * <code>AbstractParameterDefinition</code> objects.
     */
    private Map<String, AbstractionDefinition> idToAbstractionDefinitionMap;
    private Map<String, PropositionDefinition> idToPropositionDefinitionMap;
    private Map<String, ValueSet> idtoValueSetMap;

    PropositionDefinitionCache() {
        initialize();
    }

    private void initialize() {
        this.idToAbstractionDefinitionMap = new HashMap<String, AbstractionDefinition>();
        this.idToPropositionDefinitionMap = new HashMap<String, PropositionDefinition>();
        this.idtoValueSetMap = new HashMap<String, ValueSet>();
    }

    /**
     * Overrides default serialization.
     * 
     * @param s
     *            an <code>ObjectOutputStream</code> object.
     * @throws IOException
     *             if serialization failed.
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeObject(this.idToPropositionDefinitionMap.values());
        s.writeObject(this.idToAbstractionDefinitionMap.values());
        s.writeObject(this.idtoValueSetMap.values());
    }

    /**
     * Overrides default de-serialization.
     * 
     * @param s
     *            an <code>ObjectInputStream</code> object.
     * @throws IOException
     *             if de-serialization failed.
     * @throws ClassNotFoundException
     *             if de-serialization failed.
     */
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream s) throws IOException,
            ClassNotFoundException {
        initialize();

        Collection<PropositionDefinition> propositionDefinitions = (Collection<PropositionDefinition>) s.readObject();
        Collection<AbstractionDefinition> abstractionDefinitions = (Collection<AbstractionDefinition>) s.readObject();
        Collection<ValueSet> valueSets = (Collection<ValueSet>) s.readObject();

        if (propositionDefinitions != null) {
            for (PropositionDefinition def : propositionDefinitions) {
                if (def == null) {
                    throw new InvalidObjectException("Null primitive parameter definition; can't restore");
                }
                try {
                    addPropositionDefinition(def);
                } catch (InvalidPropositionIdException ex) {
                    String msg = "Could not add de-serialized proposition definition " + def;
                    //InvalidObjectException doesn't support nested exceptions.
                    ProtempaUtil.logger().log(Level.SEVERE, msg, ex);
                    throw new InvalidObjectException(msg);
                }
            }
        } else {
            throw new InvalidObjectException("propositionDefinitions cannot be null");
        }

        if (abstractionDefinitions != null) {
            for (AbstractionDefinition def : abstractionDefinitions) {
                if (def == null) {
                    throw new InvalidObjectException("Null abstraction definition; can't restore");
                }
                try {
                    addAbstractionDefinition(def);
                } catch (InvalidPropositionIdException ex) {
                    String msg = "Could not add de-serialized abstract parameter definition " + def;
                    //InvalidObjectException doesn't support nested exceptions.
                    ProtempaUtil.logger().log(Level.SEVERE, msg, ex);
                    throw new InvalidObjectException(msg);
                }
            }
        } else {
            throw new InvalidObjectException("abstractionDefinitions cannot be null");
        }
        
        if (valueSets != null) {
            for (ValueSet valueSet : valueSets) {
                if (valueSet == null) {
                    throw new InvalidObjectException("Null value set; can't restore");
                }
                try {
                    addValueSet(valueSet);
                } catch (InvalidValueSetDefinitionException ex) {
                    String msg = "Could not add de-serialized value set " + valueSet;
                    //InvalidObjectException doesn't support nested exceptions.
                    ProtempaUtil.logger().log(Level.SEVERE, msg, ex);
                    throw new InvalidObjectException(msg);
                }
            }
        }
    }

    boolean isUniqueKnowledgeDefinitionObjectId(String id) {
        return !this.idToAbstractionDefinitionMap.containsKey(id)
                && !this.idToPropositionDefinitionMap.containsKey(id);
    }
    
    public boolean hasPropositionDefinition(String eventId) {
        return getPropositionDefinition(eventId) != null;
    }

    public PropositionDefinition getPropositionDefinition(String propId) {
        return idToPropositionDefinitionMap.get(propId);
    }

    public boolean hasValueSet(String valueSetId) {
        return getValueSet(valueSetId) != null;
    }

    public ValueSet getValueSet(String valueSetId) {
        return idtoValueSetMap.get(valueSetId);
    }

    public boolean hasAbstractionDefinition(String paramId) {
        return getAbstractionDefinition(paramId) != null;
    }

    public AbstractionDefinition getAbstractionDefinition(String paramId) {
        return idToAbstractionDefinitionMap.get(paramId);
    }

    public void addPropositionDefinition(PropositionDefinition def) throws InvalidPropositionIdException {
        assert def != null : "def cannot be null";
        String id = def.getId();
        if (this.idToPropositionDefinitionMap.containsKey(id)) {
            throw new InvalidPropositionIdException("Duplicate proposition id: " + id);
        } else {
            this.idToPropositionDefinitionMap.put(id, def);
        }

    }

    public void addAbstractionDefinition(AbstractionDefinition def) throws InvalidPropositionIdException {
        assert def != null : "def cannot be null";
        String id = def.getId();
        if (this.idToAbstractionDefinitionMap.containsKey(id)) {
            throw new InvalidPropositionIdException("Duplicate proposition id: " + id);
        } else {
            idToAbstractionDefinitionMap.put(id, def);
        }
    }

    public void addValueSet(ValueSet valueSet) throws InvalidValueSetDefinitionException {
        assert valueSet != null : "valueSet cannot be null";
        String id = valueSet.getId();
        if (this.idtoValueSetMap.containsKey(id)) {
            throw new InvalidValueSetDefinitionException("Duplicate value set id: " + id);
        } else {
            this.idtoValueSetMap.put(id, valueSet);
        }
    }

    void clear() {
        this.idToAbstractionDefinitionMap.clear();
        this.idToPropositionDefinitionMap.clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this).append(this.idToAbstractionDefinitionMap.values()).append(this.idToPropositionDefinitionMap.values()).toString();
    }
}