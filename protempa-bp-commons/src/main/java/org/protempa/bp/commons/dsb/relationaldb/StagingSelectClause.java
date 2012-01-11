package org.protempa.bp.commons.dsb.relationaldb;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.protempa.bp.commons.dsb.relationaldb.ColumnSpec.KnowledgeSourceIdToSqlCode;

final class StagingSelectClause implements SelectClause {

    private final StagingSpec stagingSpec;
    private final TableAliaser referenceIndices;
    
    public StagingSelectClause(StagingSpec stagingSpec, TableAliaser referenceIndices) {
        this.stagingSpec = stagingSpec;
        this.referenceIndices = referenceIndices;
    }
    
    @Override
    public String generateClause() {
        StringBuilder result = new StringBuilder();
        
        result.append("SELECT ");
        
        List<String> cols = new ArrayList<String>();
        for (ColumnSpec columnSpec : this.stagingSpec.getStagedColumns()) {
            cols.add(referenceIndices.generateColumnReference(columnSpec));
        }
        result.append(StringUtils.join(cols, ','));
                
        return result.toString();
    }

    @Override
    public void setCaseClause(Object[] sqlCodes,
            ColumnSpec columnSpec,
            KnowledgeSourceIdToSqlCode[] filteredConstraintValues) {
        throw new UnsupportedOperationException();
    }

}
