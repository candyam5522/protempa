package org.protempa.backend.ksb.bioportal;

/*
 * #%L
 * Protempa BioPortal Knowledge Source Backend
 * %%
 * Copyright (C) 2012 - 2014 Emory University
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

import org.arp.javautil.sql.DatabaseAPI;
import org.arp.javautil.sql.InvalidConnectionSpecArguments;
import org.protempa.AbstractionDefinition;
import org.protempa.ContextDefinition;
import org.protempa.EventDefinition;
import org.protempa.KnowledgeSourceReadException;
import org.protempa.PropositionDefinition;
import org.protempa.TemporalPropositionDefinition;
import org.protempa.backend.AbstractCommonsKnowledgeSourceBackend;
import org.protempa.backend.BackendInitializationException;
import org.protempa.backend.BackendInstanceSpec;
import org.protempa.backend.KnowledgeSourceBackendInitializationException;
import org.protempa.backend.annotations.BackendInfo;
import org.protempa.backend.annotations.BackendProperty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
@BackendInfo(displayName = "BioPortal Knowledge Source Backend")
public class BioportalKnowledgeSourceBackend extends AbstractCommonsKnowledgeSourceBackend {

    /* database API to use */
    private DatabaseAPI databaseApi;

    /* the ID of the database that holds the ontologies */
    private String databaseId;

    /* the database username */
    private String username;

    /* the database password */
    private String password;

    /* name of the table that holds the ontologies */
    private String ontologiesTable;

    /* connection to the database that holds the ontologies */
    private Connection conn;

    /* prepared statement for retrieving terms from the database */
    private PreparedStatement findStmt;

    /* prepared statement for retrieving the children of a term */
    private PreparedStatement childrenStmt;

    /* prepared statement for retrieving the parent of a term */
    private PreparedStatement parentStmt;

    /* prepared statement for searching the database by keyword */
    private PreparedStatement searchStmt;

    public BioportalKnowledgeSourceBackend() {
        this.databaseApi = DatabaseAPI.DRIVERMANAGER;
    }

    /**
     * Returns which Java database API this backend is configured to use.
     *
     * @return a {@link DatabaseAPI}. The default value is
     * {@link org.arp.javautil.sql.DatabaseAPI}<code>.DRIVERMANAGER</code>
     */
    public DatabaseAPI getDatabaseApi() {
        return databaseApi;
    }

    /**
     * Configures which Java database API to use ({@link java.sql.DriverManager}
     * or {@link javax.sql.DataSource}. If
     * <code>null</code>, the default is assigned
     * ({@link org.arp.javautil.sql.DatabaseAPI}<code>.DRIVERMANAGER</code>).
     *
     * @param databaseApi a {@link DatabaseAPI}.
     */
    public void setDatabaseApi(DatabaseAPI databaseApi) {
        if (databaseApi == null) {
            this.databaseApi = DatabaseAPI.DRIVERMANAGER;
        } else {
            this.databaseApi = databaseApi;
        }
    }

    /**
     * Configures which Java database API to use ({@link java.sql.DriverManager}
     * or {@link javax.sql.DataSource} by parsing a {@link DatabaseAPI}'s name.
     * Cannot be null.
     *
     * @param databaseApiString a {@link DatabaseAPI}'s name.
     */
    @BackendProperty(propertyName = "databaseAPI")
    public void parseDatabaseApi(String databaseApiString) {
        setDatabaseApi(DatabaseAPI.valueOf(databaseApiString));
    }

    public String getDatabaseId() {
        return databaseId;
    }

    @BackendProperty
    public void setDatabaseId(String databaseId) {
        this.databaseId = databaseId;
    }

    public String getUsername() {
        return username;
    }

    @BackendProperty
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    @BackendProperty
    public void setPassword(String password) {
        this.password = password;
    }

    public String getOntologiesTable() {
        return ontologiesTable;
    }

    @BackendProperty
    public void setOntologiesTable(String ontologiesTable) {
        this.ontologiesTable = ontologiesTable;
    }

    /**
     * Initializes this backend by attempting to create the database connection based on
     * the configuration parameters.
     *
     * @param config the backend instance specification to use
     * @throws BackendInitializationException if the database connection cannot be established
     */
    @Override
    public void initialize(BackendInstanceSpec config) throws BackendInitializationException {
        super.initialize(config);
        if (this.conn == null) {
            try {
                this.conn = this.databaseApi.newConnectionSpecInstance(this.databaseId, this.username, this.password).getOrCreate();
                this.findStmt = conn.prepareStatement("SELECT display_name, code, ontology FROM " + this.ontologiesTable + " WHERE term_id = ?");
                this.childrenStmt = conn.prepareStatement("SELECT term_id FROM " + this.ontologiesTable + " WHERE parent_id = ?");
                this.parentStmt = conn.prepareStatement("SELECT parent_id FROM " + this.ontologiesTable + " WHERE term_id = ?");
                this.searchStmt = conn.prepareStatement("SELECT term_id FROM " + this.ontologiesTable + " WHERE UPPER(display_name) LIKE UPPER(?)");
            } catch (SQLException | InvalidConnectionSpecArguments e) {
                throw new KnowledgeSourceBackendInitializationException("Failed to initialize BioPortal knowledge source backend", e);
            }
        }
    }

    @Override
    public void close() {
        try {
            this.conn.close();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to close BioPortal database", e);
        }
        super.close();
    }

    private static class BioportalTerm {
        String id;
        String displayName;
        String code;
        String ontology;
    }

    private BioportalTerm readFromDatabase(String id) throws KnowledgeSourceReadException {
        try {
            this.findStmt.setString(1, id);
            ResultSet rs = this.findStmt.executeQuery();
            if (rs.next()) {
                BioportalTerm result = new BioportalTerm();
                result.id = id;
                result.displayName = rs.getString(1);
                result.code = rs.getString(2);
                result.ontology = rs.getString(3);

                return result;
            }
        } catch (SQLException e) {
            throw new KnowledgeSourceReadException(e);
        }

        return null;
    }

    private Set<String> readChildrenFromDatabase(String id) throws KnowledgeSourceReadException {
        Set<String> result = new HashSet<>();
        try {
            this.childrenStmt.setString(1, id);
            ResultSet rs = this.childrenStmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new KnowledgeSourceReadException(e);
        }

        return result;
    }

    @Override
    public PropositionDefinition readPropositionDefinition(String id) throws KnowledgeSourceReadException {
        BioportalTerm term = readFromDatabase(id);
        if (term != null) {
            EventDefinition result = new EventDefinition(id);
            result.setDisplayName(term.displayName);
            result.setAbbreviatedDisplayName(term.code);
            result.setInDataSource(true);

            Set<String> children = readChildrenFromDatabase(id);
            String[] iia = new String[children.size()];

            int i = 0;
            for (String child : children) {
                iia[i] = child;
                i++;
            }
            result.setInverseIsA(iia);

            return result;
        }
        return null;
    }

    @Override
    public String[] readIsA(String propId) throws KnowledgeSourceReadException {
        try {
            List<String> result = new ArrayList<>();
            this.parentStmt.setString(1, propId);
            ResultSet rs = this.parentStmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getString(1));
            }
            return result.toArray(new String[result.size()]);
        } catch (SQLException e) {
            throw new KnowledgeSourceReadException(e);
        }
    }

    @Override
    public List<String> getKnowledgeSourceSearchResults(String searchKey) throws KnowledgeSourceReadException {
        try {
            List<String> result = new ArrayList<>();
            this.searchStmt.setString(1, "%" + searchKey + "%");
            ResultSet rs = this.searchStmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getString(1));
            }
            return result;
        } catch (SQLException e) {
            throw new KnowledgeSourceReadException(e);
        }
    }

    @Override
    public AbstractionDefinition readAbstractionDefinition(String id) throws KnowledgeSourceReadException {
        return null;
    }

    @Override
    public ContextDefinition readContextDefinition(String id) throws KnowledgeSourceReadException {
        return null;
    }

    @Override
    public TemporalPropositionDefinition readTemporalPropositionDefinition(String id) throws KnowledgeSourceReadException {
        return null;
    }

    @Override
    public String[] readAbstractedInto(String propId) throws KnowledgeSourceReadException {
        return new String[0];
    }

    @Override
    public String[] readInduces(String propId) throws KnowledgeSourceReadException {
        return new String[0];
    }

    @Override
    public String[] readSubContextOfs(String propId) throws KnowledgeSourceReadException {
        return new String[0];
    }
}
