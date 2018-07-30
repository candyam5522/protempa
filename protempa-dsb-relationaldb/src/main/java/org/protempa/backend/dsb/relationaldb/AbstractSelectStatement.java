/*
 * #%L
 * Protempa Commons Backend Provider
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
package org.protempa.backend.dsb.relationaldb;

import java.util.ArrayList;
import org.protempa.backend.dsb.filter.Filter;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractSelectStatement implements SelectStatement {

    private final EntitySpec entitySpec;
    private final List<EntitySpec> entitySpecs;
    private final Map<String, ReferenceSpec> inboundReferenceSpecs;
    private final Set<Filter> filters;
    private final Set<String> propIds;
    private final Set<String> keyIds;
    private final SQLOrderBy order;
    private final SQLGenResultProcessor resultProcessor;
    private final boolean wrapKeyId;

    protected AbstractSelectStatement(EntitySpec entitySpec,
            List<EntitySpec> entitySpecs,
            Map<String, ReferenceSpec> inboundReferenceSpecs,
            Set<Filter> filters, Set<String> propIds, Set<String> keyIds,
            SQLOrderBy order, SQLGenResultProcessor resultProcessor,
            boolean wrapKeyId) {
        this.entitySpec = entitySpec;
        this.entitySpecs = Collections.unmodifiableList(entitySpecs);
        this.inboundReferenceSpecs = Collections.unmodifiableMap(inboundReferenceSpecs);
        this.filters = Collections.unmodifiableSet(filters);
        this.propIds = Collections.unmodifiableSet(propIds);
        this.keyIds = Collections.unmodifiableSet(keyIds);
        this.order = order;
        this.resultProcessor = resultProcessor;
        this.wrapKeyId = wrapKeyId;
    }

    protected EntitySpec getEntitySpec() {
        return entitySpec;
    }

    protected List<EntitySpec> getEntitySpecs() {
        return entitySpecs;
    }

    protected Map<String, ReferenceSpec> getInboundReferenceSpecs() {
        return inboundReferenceSpecs;
    }

    protected Set<Filter> getFilters() {
        return filters;
    }

    protected Set<String> getPropIds() {
        return propIds;
    }

    protected Set<String> getKeyIds() {
        return keyIds;
    }

    protected SQLOrderBy getOrder() {
        return order;
    }

    protected SQLGenResultProcessor getResultProcessor() {
        return resultProcessor;
    }

    protected abstract SelectClause getSelectClause(ColumnSpecInfo info,
            TableAliaser referenceIndices, EntitySpec entitySpec, boolean wrapKeyId);

    protected abstract FromClause getFromClause(List<ColumnSpec> columnSpecs,
            TableAliaser referenceIndices);

    protected abstract WhereClause getWhereClause(Set<String> propIds,
            ColumnSpecInfo info, List<EntitySpec> entitySpecs,
            Set<Filter> filters, TableAliaser referenceIndices,
            Set<String> keyIds, SQLOrderBy order,
            SQLGenResultProcessor resultProcessor, SelectClause selectClause);

    @Override
    public String generateStatement() {
        ColumnSpecInfo info = new ColumnSpecInfoFactory().newInstance(propIds,
                entitySpec, entitySpecs, inboundReferenceSpecs, filters);
        TableAliaser referenceIndices = new TableAliaser(info.getColumnSpecs(),
                "a");

        SelectClause select = getSelectClause(info, referenceIndices,
                this.entitySpec, wrapKeyId);
        FromClause from = getFromClause(toColumnSpecs(info.getColumnSpecs()),
                referenceIndices);
        List<EntitySpec> esCopy = new ArrayList<>(entitySpecs);
        for (Iterator<EntitySpec> itr = esCopy.iterator(); itr.hasNext();) {
            EntitySpec es = itr.next();
            ReferenceSpec[] referencesTo = es.referencesTo(entitySpec);
            for (ReferenceSpec rs : referencesTo) {
                if (!rs.isApplyConstraints()) {
                    itr.remove();
                    break;
                }
            }
        }
        WhereClause where = getWhereClause(propIds, info, esCopy,
                this.filters, referenceIndices, this.keyIds, this.order,
                this.resultProcessor, select);

        return select.generateClause() + 
                " " + from.generateClause() + 
                " " + where.generateClause();
    }
    
    protected final List<ColumnSpec> toColumnSpecs(List<IntColumnSpecWrapper> columnSpecWrappers) {
        List<ColumnSpec> result = new ArrayList<>();
        for (IntColumnSpecWrapper icsw : columnSpecWrappers) {
            result.add(icsw.getColumnSpec());
        }
        return result;
    }
}
