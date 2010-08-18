package org.protempa.backend.test;

import org.protempa.*;
import org.protempa.backend.BackendInstanceSpec;

public final class MockKnowledgeSourceBackend
        extends AbstractKnowledgeSourceBackend {

    @Override
    public void initialize(BackendInstanceSpec config)
            throws BackendInitializationException {
    }

    /**
     * Make public so that tests can call it.
     * @see AbstractKnowledgeSourceBackend
     */
    @Override
    public void fireKnowledgeSourceBackendUpdated() {
        super.fireKnowledgeSourceBackendUpdated();
    }

    @Override
    public String getDisplayName() {
        return "Mock Knowledge Source Backend";
    }
}
