package dev.arcovia.mitigation.smt.constraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsSelector;
import org.dataflowanalysis.dfd.datadictionary.Label;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;
import dev.arcovia.mitigation.smt.utils.ParsingUtils;

/**
 * Selector translation logic for VertexCharacteristicsSelector
 */
final class VertexCharacteristicsHandler extends AbstractSelectorHandler<VertexCharacteristicsSelector> {
    @Override
    protected BoolExpr encode(VertexCharacteristicsSelector selector, DFDVertex vertex, SMT smt) {

        var context = smt.getContext();

        // Set only contains one label
        Set<Label> selectorLabels = ParsingUtils.getLabelsForCharacteristics(smt.getDataDictionary(), List.of(selector.getVertexCharacteristics()));

        // Get labels for node
        Map<Label, BoolExpr> present = smt.getNodeLabels()
                .get(vertex.getReferencedElement());

        List<BoolExpr> labelMatches = new ArrayList<>(selectorLabels.size());
        for (Label label : selectorLabels) {
            BoolExpr hasLabel = present.get(label);
            labelMatches.add(hasLabel);
        }

        BoolExpr matches = context.mkOr(labelMatches.toArray(new BoolExpr[0]));

        // Invert if selector is inverted
        BoolExpr result = selector.isInverted() ? context.mkNot(matches) : matches;

        if (selector.isRecursive()) {
            List<BoolExpr> anyMatches = new ArrayList<BoolExpr>();
            anyMatches.add(result);
            for (AbstractVertex<?> prevAbstractVertex : vertex.getPreviousElements()) {
                DFDVertex prevDfdVertex = (DFDVertex) prevAbstractVertex;
                anyMatches.add(encode(selector, prevDfdVertex, smt));
            }
            return context.mkOr(anyMatches.toArray(new BoolExpr[0]));
        } else {
            return result;
        }
    }
}
