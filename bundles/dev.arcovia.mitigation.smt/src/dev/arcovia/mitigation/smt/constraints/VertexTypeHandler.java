package dev.arcovia.mitigation.smt.constraints;

import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dfd.dsl.DFDVertexType;
import org.dataflowanalysis.analysis.dsl.selectors.VertexTypeSelector;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;
import dev.arcovia.mitigation.smt.util.Util;

/**
 * Selector translation logic for VertexTypeSelectors
 */
final class VertexTypeHandler extends AbstractSelectorHandler<VertexTypeSelector> {

    @Override
    protected BoolExpr encode(VertexTypeSelector selector, DFDVertex vertex, SMT smt) {
        var context = smt.getContext();

        DFDVertexType selectorType = (DFDVertexType) selector.getVertexType();

        // Can be statically evaluated at encoding time because types are not modifiable
        BoolExpr matches;
        if (selectorType.equals(Util.vertexToType(vertex))) {
            matches = context.mkTrue();
        } else {
            matches = context.mkFalse();
        }
        // Invert if selector is inverted
        BoolExpr result = selector.isInverted() ? context.mkNot(matches) : matches;

        if (selector.isRecursive()) {
            List<BoolExpr> anyMatches = new ArrayList<BoolExpr>();
            anyMatches.add(result);
            for (AbstractVertex<?> prevAbstract : vertex.getPreviousElements()) {
                DFDVertex prev = (DFDVertex) prevAbstract;
                anyMatches.add(encode(selector, prev, smt));
            }
            return context.mkOr(anyMatches.toArray(new BoolExpr[0]));
        } else {
            return result;
        }
    }
}
