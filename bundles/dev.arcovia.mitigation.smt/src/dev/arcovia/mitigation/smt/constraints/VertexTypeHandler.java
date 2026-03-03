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
    protected BoolExpr encode(VertexTypeSelector s, DFDVertex vertex, SMT smt) {
        var ctx = smt.getCtx();

        DFDVertexType selectorType = (DFDVertexType) s.getVertexType();

        // Can be statically evaluated at encoding time because types are not modifiable
        BoolExpr matches;
        if (selectorType.equals(Util.vertexToType(vertex))) {
            matches = ctx.mkTrue();
        } else {
            matches = ctx.mkFalse();
        }
        // Maybe invert
        BoolExpr result = s.isInverted() ? ctx.mkNot(matches) : matches;

        if (s.isRecursive()) {
            List<BoolExpr> anyMatches = new ArrayList<BoolExpr>();
            anyMatches.add(result);
            for (AbstractVertex<?> prevAbstract : vertex.getPreviousElements()) {
                DFDVertex prev = (DFDVertex) prevAbstract;
                anyMatches.add(encode(s, prev, smt));
            }
            return ctx.mkOr(anyMatches.toArray(new BoolExpr[0]));
        } else {
            return result;
        }
    }
}
