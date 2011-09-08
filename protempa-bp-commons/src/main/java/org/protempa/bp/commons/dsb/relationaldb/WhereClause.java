package org.protempa.bp.commons.dsb.relationaldb;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.protempa.backend.dsb.filter.Filter;

final class WhereClause implements SQLClause {
    private final ColumnSpecInfo info;
    private final List<EntitySpec> entitySpecs;
    private final Set<Filter> filters;
    private final Map<ColumnSpec, Integer> referenceIndices;
    private final Set<String> keyIds;
    private final SQLOrderBy order;
    private final SQLGenResultProcessor resultProcessor;

    
    WhereClause(ColumnSpecInfo info, List<EntitySpec> entitySpecs,
            Set<Filter> filters, Map<ColumnSpec, Integer> referenceIndices,
            Set<String> keyIds, SQLOrderBy order,
            SQLGenResultProcessor resultProcessor) {
        this.info = info;
        this.entitySpecs = entitySpecs;
        this.filters = filters;
        this.referenceIndices = referenceIndices;
        this.keyIds = keyIds;
        this.order = order;
        this.resultProcessor = resultProcessor;
    }


    public String generateClause() {
        return "";
    }
}
