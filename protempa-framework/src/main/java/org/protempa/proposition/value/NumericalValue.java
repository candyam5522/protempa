package org.protempa.proposition.value;

import java.math.BigDecimal;

/**
 * Represents numerical values.
 * 
 * @author Andrew Post
 */
public interface NumericalValue extends OrderedValue {
    
    /**
     * Gets this value as a number.
     * 
     * @return a {@link Number}.
     */
    Number getNumber();

    /**
     * Gets this value as a {@link BigDecimal}.
     * 
     * @return a {@link BigDecimal}.
     */
    BigDecimal getBigDecimal();

    /**
     * Gets this value as a double.
     * 
     * @return a {@link double}.
     */
    double doubleValue();
}
