package dev.arcovia.mitigation.smt.constraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicsSelector;
import org.dataflowanalysis.dfd.datadictionary.Label;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;
import dev.arcovia.mitigation.smt.TFGFlow;
import dev.arcovia.mitigation.smt.util.Util;

/**
 * Selector translation logic for DataCharacteristicsSelector
 */
final class DataCharacteristicsHandler extends AbstractSelectorHandler<DataCharacteristicsSelector> {
    @Override
    protected BoolExpr encode(DataCharacteristicsSelector s, DFDVertex vertex, SMT smt) {

        var ctx = smt.getCtx();

        // Set only contains one label
        Set<Label> selectorLabels = Util.getLabelsForCharacteristics(smt.getDD(), List.of(s.getDataCharacteristic()));

        List<BoolExpr> flowsMatch = new ArrayList<>();

        for (TFGFlow flow : smt.getVertexIncomingFlows()
                .getOrDefault(vertex, List.of())) {
            Map<Label, BoolExpr> flowLabelMap = smt.getFlowLabels()
                    .get(flow);

            List<BoolExpr> anySelectorLabelPresent = new ArrayList<>(selectorLabels.size());
            for (Label lbl : selectorLabels) {
                BoolExpr has = flowLabelMap.get(lbl);
                anySelectorLabelPresent.add(has);
            }

            // Flow matches, if label is present
            BoolExpr thisFlowMatches = ctx.mkOr(anySelectorLabelPresent.toArray(new BoolExpr[0]));

            flowsMatch.add(thisFlowMatches);
        }

        // If vertex has no incoming flows it never matches
        if (flowsMatch.isEmpty()) {
            return ctx.mkFalse();
        }

        // It matches if any incoming flow matches
        BoolExpr anyFlowMatches = ctx.mkOr(flowsMatch.toArray(new BoolExpr[0]));
        // Maybe invert
        return s.isInverted() ? ctx.mkNot(anyFlowMatches) : anyFlowMatches;
    }
}
