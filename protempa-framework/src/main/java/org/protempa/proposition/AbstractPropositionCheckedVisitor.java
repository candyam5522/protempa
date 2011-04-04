package org.protempa.proposition;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.protempa.ProtempaException;

/**
 *
 * @author Andrew Post
 */
public class AbstractPropositionCheckedVisitor
        implements PropositionCheckedVisitor {

    @Override
    public void visit(Map<String, List<Proposition>> finderResult)
            throws ProtempaException {
        for (List<Proposition> listOfProps : finderResult.values()) {
            visit(listOfProps);
        }

    }

    @Override
    public void visit(Collection<? extends Proposition> propositions)
            throws ProtempaException {
        for (Proposition proposition : propositions) {
            proposition.acceptChecked(this);
        }
    }

    @Override
    public void visit(PrimitiveParameter primitiveParameter)
            throws ProtempaException {
    }

    @Override
    public void visit(Event event)
            throws ProtempaException {
    }

    @Override
    public void visit(AbstractParameter abstractParameter)
            throws ProtempaException {
    }

    @Override
    public void visit(Constant constantParameter)
            throws ProtempaException {
    }

    @Override
    public void visit(Context context) throws ProtempaException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
