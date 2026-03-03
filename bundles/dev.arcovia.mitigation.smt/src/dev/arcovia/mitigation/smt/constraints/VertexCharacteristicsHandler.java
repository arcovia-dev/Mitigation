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
import dev.arcovia.mitigation.smt.util.Util;

/**
 * Selector translation logic for VertexCharacteristicsSelector
 */
final class VertexCharacteristicsHandler extends AbstractSelectorHandler<VertexCharacteristicsSelector> {
    @Override
    protected BoolExpr encode(VertexCharacteristicsSelector s, DFDVertex vertex, SMT smt) {

        var ctx = smt.getCtx();

        // Set only contains one label
        Set<Label> selectorLabels = Util.getLabelsForCharacteristics(smt.getDD(), List.of(s.getVertexCharacteristics()));

        // Get labels for node
        Map<Label, BoolExpr> present = smt.getNodeLabels()
                .get(vertex.getReferencedElement());

        List<BoolExpr> labelMatches = new ArrayList<>(selectorLabels.size());
        for (Label lbl : selectorLabels) {
            BoolExpr has = present.get(lbl);
            labelMatches.add(has);
        }

        BoolExpr matches = ctx.mkOr(labelMatches.toArray(new BoolExpr[0]));

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
