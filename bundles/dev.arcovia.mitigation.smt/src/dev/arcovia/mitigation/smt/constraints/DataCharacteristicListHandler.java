package dev.arcovia.mitigation.smt.constraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicListSelector;
import org.dataflowanalysis.dfd.datadictionary.Label;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;
import dev.arcovia.mitigation.smt.TFGFlow;
import dev.arcovia.mitigation.smt.util.Util;

/**
 * Selector translation logic for Data Characteristics List Selectors
 */
final class DataCharacteristicListHandler extends AbstractSelectorHandler<DataCharacteristicListSelector> {

    @Override
    protected BoolExpr encode(DataCharacteristicListSelector selector, DFDVertex vertex, SMT smt) {

        var context = smt.getContext();

        // Get labels for selectors
        Set<Label> selectorLabels = Util.getLabelsForCharacteristics(smt.getDataDictionary(), selector.getDataCharacteristics());

        List<BoolExpr> flowsMatch = new ArrayList<>();

        // Evaluate all incoming flows
        for (TFGFlow flow : smt.getVertexIncomingFlows()
                .getOrDefault(vertex, List.of())) {
            Map<Label, BoolExpr> flowLabelMap = smt.getFlowLabels()
                    .get(flow);

            // Check all labels
            List<BoolExpr> anySelectorLabelPresent = new ArrayList<>(selectorLabels.size());
            for (Label label : selectorLabels) {
                BoolExpr hasLabel = flowLabelMap.get(label);
                anySelectorLabelPresent.add(hasLabel);
            }

            // Flow matches, if any label is present
            BoolExpr thisFlowMatches = context.mkOr(anySelectorLabelPresent.toArray(new BoolExpr[0]));

            flowsMatch.add(thisFlowMatches);
        }

        // Selector never matches if vertex has no incoming flows
        if (flowsMatch.isEmpty()) {
            return context.mkFalse();
        }
        // Selector matches if any flow matches
        BoolExpr anyFlowMatches = context.mkOr(flowsMatch.toArray(new BoolExpr[0]));
        // Invert if selector is inverted
        return selector.isInverted() ? context.mkNot(anyFlowMatches) : anyFlowMatches;
    }
}
