package dev.arcovia.mitigation.smt.constraints;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.VertexNameSelector;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;

/**
 * Selector translation logic for VertexNameSelector Only available as a destination selector
 */
public class VertexNameHandler extends AbstractSelectorHandler<VertexNameSelector> {

    @Override
    protected BoolExpr encode(VertexNameSelector selector, DFDVertex vertex, SMT smt) {

        var context = smt.getContext();

        // We can statically evaluate this at encoding time as vertex names are not modifiable
        String select = selector.getName();
        BoolExpr matches;
        if (vertex.getReferencedElement()
                .getEntityName()
                .equals(select)) {
            matches = context.mkTrue();
        } else {
            matches = context.mkFalse();
        }
        // Invert if selector is inverted
        return selector.isInverted() ? context.mkNot(matches) : matches;
    }

}
