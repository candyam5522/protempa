package org.protempa.bp.commons.dsb.sqlgen;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.arp.javautil.collections.Collections;
import org.protempa.DatabaseDataSourceType;
import org.protempa.DerivedDataSourceType;
import org.protempa.proposition.ConstantParameter;
import org.protempa.proposition.UniqueIdentifier;
import org.protempa.proposition.value.Value;
import org.protempa.proposition.value.ValueFactory;
import org.protempa.proposition.value.ValueType;

class ConstantParameterResultProcessor extends
        AbstractMainResultProcessor<ConstantParameter> {

    @Override
    public void process(ResultSet resultSet) throws SQLException {
        Map<String, List<ConstantParameter>> results = getResults();
        EntitySpec entitySpec = getEntitySpec();
        String[] propIds = entitySpec.getPropositionIds();
        Logger logger = SQLGenUtil.logger();
        while (resultSet.next()) {
            int i = 1;
            String keyId = resultSet.getString(i++);
            String[] uniqueIds = generateUniqueIdsArray(entitySpec);
            i = readUniqueIds(uniqueIds, resultSet, i);
            UniqueIdentifier uniqueIdentifer = generateUniqueIdentifier(
                    entitySpec, uniqueIds);
            ValueType vf = entitySpec.getValueType();
            Value cpVal = ValueFactory.get(vf).parseValue(
                    resultSet.getString(i++));
            String propId;
            if (propIds.length == 1) {
                propId = propIds[0];
            } else {
                propId = resultSet.getString(i++);
            }
            ConstantParameter cp = new ConstantParameter(propId);
            cp.setValue(cpVal);
            cp.setUniqueIdentifier(uniqueIdentifer);
            cp.setDataSourceType(new DerivedDataSourceType());
            PropertySpec[] propertySpecs = entitySpec.getPropertySpecs();
            for (PropertySpec propertySpec : propertySpecs) {
                ValueType vf2 = propertySpec.getValueType();
                Value value = ValueFactory.get(vf2).parseValue(
                        resultSet.getString(i++));
                cp.setProperty(propertySpec.getName(), value);
            }
            cp.setDataSourceType(
                    new DatabaseDataSourceType(getDataSourceBackendId()));
            logger.log(Level.FINEST, "Created constant parameter {0}", cp);
            Collections.putList(results, keyId, cp);
        }
    }
}
