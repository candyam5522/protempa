package org.protempa.bp.commons.dsb.relationaldb;

interface SqlStatement {
    String generateStatement();
    
    String generateTableReference(int tableNumber);
}
