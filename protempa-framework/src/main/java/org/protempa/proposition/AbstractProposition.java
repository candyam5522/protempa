package org.protempa.proposition;

import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.protempa.DataSourceType;
import org.protempa.proposition.value.Value;

/**
 * Abstract class for implementing the various kinds of propositions.
 * 
 * @author Andrew Post
 */
public abstract class AbstractProposition implements Proposition {

    private static final long serialVersionUID = -6210974161591587259L;
    /**
     * An identification <code>String</code> for this proposition.
     */
    private final String id;
    private static volatile int nextHashCode = 17;
    protected volatile int hashCode;
    protected final PropertyChangeSupport changes;
    private final Map<String, Value> properties;
    private final Map<String, List<UniqueIdentifier>> references;
    private UniqueIdentifier key;
    private DataSourceType dataSourceType;

    /**
     * Creates a proposition with an id.
     *
     * @param id
     *            an identification <code>String</code> for this proposition.
     */
    AbstractProposition(String id) {
        if (id == null) {
            this.id = "";
        } else {
            this.id = id;
        }
        this.changes = new PropertyChangeSupport(this);
        this.properties = new HashMap<String, Value>();
        this.references = new HashMap<String,List<UniqueIdentifier>>();
    }
    
    @Override
    public String getId() {
        return this.id;
    }

    public final void setProperty(String name, Value value) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.properties.put(name, value);
    }

    @Override
    public final Value getProperty(String name) {
        return this.properties.get(name);
    }

    @Override
    public final String[] getPropertyNames() {
        Set<String> keys = this.properties.keySet();
        return keys.toArray(new String[keys.size()]);
    }

    public final void setUniqueIdentifier(UniqueIdentifier o) {
        this.key = o;
    }

    @Override
    public DataSourceType getDataSourceType() {
        return this.dataSourceType;
    }

    public void setDataSourceType(DataSourceType type) {
        this.dataSourceType = type;
    }

    @Override
    public final UniqueIdentifier getUniqueIdentifier() {
        return this.key;
    }

    public final void setReferences(String name, List<UniqueIdentifier> refs) {
        if (refs != null)
            refs = new ArrayList<UniqueIdentifier>(refs);
        this.references.put(name, refs);
    }

    @Override
    public final List<UniqueIdentifier> getReferences(String name) {
        List<UniqueIdentifier> result = this.references.get(name);
        if (result != null)
            return Collections.unmodifiableList(result);
        else
            return null;
    }
    
    @Override
    public int hashCode() {
        if (this.hashCode == 0) {
            this.hashCode = nextHashCode;
            nextHashCode *= 37;
        }
        return this.hashCode;
    }
    
    @Override
    public boolean isEqual(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof AbstractProposition)) {
            return false;
        }

        AbstractProposition p = (AbstractProposition) other;
        return (id == p.id || id.equals(p.id))
                && this.properties.equals(p.properties);

    }
    
    // The following code implements hashCode() and equals() using unique 
    // identifiers, as well as the datasource backend identifiers.
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    /*
    @Override
    public int hashCode() {
        if (this.hashCode == 0) {
            if (this.key == null
                    || this.datasourceBackendId == null) {
                this.hashCode = super.hashCode();
            } else {
                this.hashCode = 17;
                this.hashCode += 37 * this.key.hashCode();
                this.hashCode += 37 * this.datasourceBackendId.hashCode();
            }
        }
        return this.hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else {
            if (!(obj instanceof Proposition)) {
                return false;
            }
            Proposition prop = (Proposition) obj;
            if (prop.getUniqueIdentifier() == null
                    || this.key == null
                    || prop.getDataSourceBackendId() == null
                    || this.datasourceBackendId == null) {
                return false;
            } else {
                return prop.getUniqueIdentifier().equals(
                        this.key)
                        && prop.getDataSourceBackendId().equals(
                                this.datasourceBackendId);
            }
        }
    }
    */
}
