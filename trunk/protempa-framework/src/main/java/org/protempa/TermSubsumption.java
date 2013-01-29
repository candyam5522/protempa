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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class TermSubsumption {
    private Set<String> termIds;
    
    private TermSubsumption(HashSet<String> termIds) {
        this.termIds = termIds;
    }
    
    public TermSubsumption() {
        this.termIds = new HashSet<String>();
    }
    
    public static TermSubsumption fromTerms(Collection<String> termIds) {
        return new TermSubsumption(new HashSet<String>(termIds));
    }
    
    public static TermSubsumption fromTerms(String... termIds) {
        HashSet<String> set = new HashSet<String>();
        for (String termId : termIds) {
            set.add(termId);
        }
        return new TermSubsumption(set);
    }
    
    public boolean containsTerm(String termId) {
        return termIds.contains(termId);
    }
    
    public Set<String> getTerms() {
        return termIds;
    }
}
