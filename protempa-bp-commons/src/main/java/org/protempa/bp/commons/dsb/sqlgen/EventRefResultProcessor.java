package org.protempa.bp.commons.dsb.sqlgen;

import org.protempa.proposition.Event;
import org.protempa.proposition.UniqueIdentifier;

/**
 *
 * @author Andrew Post
 */
final class EventRefResultProcessor extends RefResultProcessor<Event> {

    @Override
    void addReferenceForProposition(String referenceName, Event event,
            UniqueIdentifier uid) {
        event.addReference(referenceName, uid);
    }
}
