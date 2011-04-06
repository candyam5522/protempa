package org.protempa.query.handler.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.xml.parsers.ParserConfigurationException;

import org.protempa.KnowledgeSource;
import org.protempa.KnowledgeSourceReadException;
import org.protempa.PropertyDefinition;
import org.protempa.PropositionDefinition;
import org.protempa.proposition.AbstractParameter;
import org.protempa.proposition.AbstractPropositionCheckedVisitor;
import org.protempa.proposition.Constant;
import org.protempa.proposition.Event;
import org.protempa.proposition.Parameter;
import org.protempa.proposition.PrimitiveParameter;
import org.protempa.proposition.Proposition;
import org.protempa.proposition.TemporalProposition;
import org.protempa.proposition.value.Value;
import org.protempa.proposition.value.ValueSet;

public class XmlPropositionVisitor extends AbstractPropositionCheckedVisitor {

    private final KnowledgeSource knowledgeSource;
    private final Map<String, Map<String, String>> properties;
    private final Map<String, String> propositionValues;

    public XmlPropositionVisitor(KnowledgeSource ks)
            throws ParserConfigurationException {
        this.knowledgeSource = ks;
        this.propositionValues = new HashMap<String, String>();
        this.properties = new HashMap<String, Map<String, String>>();
    }

    public void clear() {
        this.properties.clear();
        this.propositionValues.clear();
    }

    public Map<String, String> getPropositionValues() {
        return this.propositionValues;
    }

    public Map<String, Map<String, String>> getPropositionProperties() {
        return this.properties;
    }

    @Override
    public void visit(AbstractParameter abstractParameter)
            throws XmlQueryResultsHandlerException {
        doName(abstractParameter);
        doValue(abstractParameter);
        doTime(abstractParameter);
        doProperties(abstractParameter);
    }

    @Override
    public void visit(Event event) throws XmlQueryResultsHandlerException {
        doName(event);
        doTime(event);
        doProperties(event);
    }

    @Override
    public void visit(PrimitiveParameter primitiveParameter)
            throws XmlQueryResultsHandlerException {
        doName(primitiveParameter);
        doValue(primitiveParameter);
        doTime(primitiveParameter);
        doProperties(primitiveParameter);
    }

    @Override
    public void visit(Constant constant) throws XmlQueryResultsHandlerException {
        doName(constant);
        doProperties(constant);
    }

    private void doProperties(Proposition proposition)
            throws XmlQueryResultsHandlerException {
        for (String propName : proposition.getPropertyNames()) {
            Map<String, String> propMap = new HashMap<String, String>();
            Value value = proposition.getProperty(propName);
            if (value != null) {
                String valueType = value.getType().toString();
                propMap.put("valueType", valueType);
                propMap.put("value", value.getFormatted());
                propMap.put("valueDisplayName",
                        getOutputPropertyValue(proposition, propName, value));
            } else {
                propMap.put("value", "(null)");
            }
            this.properties.put(propName, propMap);
        }
    }

    private void doName(Proposition proposition)
            throws XmlQueryResultsHandlerException {
        try {
            this.propositionValues.put("displayName",
                    getDisplayName(proposition));
        } catch (KnowledgeSourceReadException e) {
            throw new XmlQueryResultsHandlerException(e);
        }
    }

    private void doValue(Parameter parameter) {
        Value value = parameter.getValue();
        String valueType = "(null)";
        String valueFormatted = "(null)";
        if (value != null) {
            valueType = value.getType().toString();
            valueFormatted = value.getFormatted();
        }
        this.propositionValues.put("valueType", valueType);
        this.propositionValues.put("value", valueFormatted);
    }

    private void doTime(TemporalProposition proposition) {
        String start = proposition.getStartFormattedShort();
        String finish = proposition.getFinishFormattedShort();
        if (finish.isEmpty()) {
            this.propositionValues.put("date", start);
        } else {
            this.propositionValues.put("start", start);
            this.propositionValues.put("end", finish);
        }
    }

    private String getDisplayName(Proposition proposition)
            throws KnowledgeSourceReadException {
        PropositionDefinition propositionDefinition = this.knowledgeSource
                .readPropositionDefinition(proposition.getId());
        String name = "(null)";
        if (propositionDefinition != null) {
            name = propositionDefinition.getDisplayName();
        } else {
            Util.logger()
                    .log(Level.WARNING,
                            "Cannot write display name for {0} because it is not in the knowledge source",
                            proposition.getId());
        }
        return name;
    }

    private String getOutputPropertyValue(Proposition proposition,
            String propertyName, Value propertyValue) {
        String outputValue = propertyValue.getFormatted();
        try {
            PropositionDefinition propositionDef = this.knowledgeSource
                    .readPropositionDefinition(proposition.getId());
            if (propositionDef != null) {
                PropertyDefinition propertyDef = propositionDef
                        .propertyDefinition(propertyName);
                if (propertyDef != null) {
                    ValueSet valueSet = this.knowledgeSource
                            .readValueSet(propertyDef.getValueSetId());
                    if (valueSet != null) {
                        outputValue = valueSet.displayName(propertyValue);
                    }
                }
            }
        } catch (KnowledgeSourceReadException e) {
            Util.logger().log(Level.SEVERE, e.getMessage(), e);
        }
        return outputValue;
    }
}
