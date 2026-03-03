package dev.arcovia.mitigation.smt.constraints;

import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.VariableNameSelector;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;
import dev.arcovia.mitigation.smt.TFGFlow;

/**
 * Selector translation logic for Data Name selectors
 */
final class DataNameHandler extends AbstractSelectorHandler<VariableNameSelector> {
    @Override
    protected BoolExpr encode(VariableNameSelector s, DFDVertex vertex, SMT smt) {

        var ctx = smt.getCtx();

        String selectorName = s.getVariableName();

        List<BoolExpr> flowsMatch = new ArrayList<>();
        for (TFGFlow flow : smt.getVertexIncomingFlows()
                .getOrDefault(vertex, List.of())) {
            // Because flow names are not modifiable, we can statically evaluate this at encoding time
            if (flow.getFlow()
                    .getEntityName()
                    .equals(selectorName)) {
                flowsMatch.add(ctx.mkTrue());
            } else {
                flowsMatch.add(ctx.mkFalse());
            }
        }

        // Selector never matches for no incoming flows
        if (flowsMatch.isEmpty()) {
            return ctx.mkFalse();
        }

        // Selector matches if any incoming flow matches. Selector is not invertable.
        BoolExpr anyFlowMatches = ctx.mkOr(flowsMatch.toArray(new BoolExpr[0]));
        return anyFlowMatches;
    }

}
