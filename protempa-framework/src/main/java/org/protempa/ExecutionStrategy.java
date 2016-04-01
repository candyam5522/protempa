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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eurekaclinical.datastore.DataStore;
import org.drools.RuleBase;
import org.drools.WorkingMemory;
import org.protempa.proposition.Proposition;

interface ExecutionStrategy {
    
    void initialize();

    Iterator<Proposition> execute(String keyIds,
            Set<String> propositionIds, List<?> objects,
            DataStore<String, WorkingMemory> wm);

    void cleanup();

    void createRuleBase(Collection<PropositionDefinition> allNarrowerDescendants, 
            DerivationsBuilder listener,
            QuerySession qs) throws CreateRuleBaseException;
    
    RuleBase getRuleBase();
}
