package dev.arcovia.mitigation.uncertainty.verification.solving;

import dev.arcovia.mitigation.cost.ActionCostEvaluator;
import dev.arcovia.mitigation.cost.ActionType;
import dev.arcovia.mitigation.cost.RepairCostSpecification;
import dev.arcovia.mitigation.ilp.ActionTerm;
import dev.arcovia.mitigation.ilp.MitigationType;
import dev.arcovia.mitigation.ilp.RepairActionCostModel;
import dev.arcovia.mitigation.ilp.RepairActionKeys;
import dev.arcovia.mitigation.sat.CompositeLabel;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.sat.OutgoingDataLabel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepairCostModelComparisonTest {

    @Test
    void standardPreferencePreservesEveryLegacyDirectScore() {
        RepairActionCostModel legacy = RepairActionCostModel.standard();
        RepairCostSpecification thesis = RepairCostSpecification.standardPreference();
        List<CompositeLabel> nodeLabels = List.of(new NodeLabel(new Label("Location", "EU")));
        List<CompositeLabel> dataLabels = List.of(new OutgoingDataLabel(new Label("Location", "EU")));
        List<CompositeLabel> noLabels = List.of();

        assertSameDirectScore(legacy, thesis, MitigationType.NodeLabel, ActionType.Adding, nodeLabels);
        assertSameDirectScore(legacy, thesis, MitigationType.DataLabel, ActionType.Adding, dataLabels);
        assertSameDirectScore(legacy, thesis, MitigationType.DeleteNodeLabel, ActionType.Removing, nodeLabels);
        assertSameDirectScore(legacy, thesis, MitigationType.DeleteDataLabel, ActionType.Removing, dataLabels);
        assertSameDirectScore(legacy, thesis, MitigationType.AddNode, ActionType.AddNode, noLabels);
        assertSameDirectScore(legacy, thesis, MitigationType.DeleteNode, ActionType.RemoveNode, noLabels);
        assertSameDirectScore(legacy, thesis, MitigationType.DeleteFlow, ActionType.RemoveFlow, noLabels);
        assertSameDirectScore(legacy, thesis, MitigationType.AddSink, ActionType.AddSink, noLabels);
    }

    private void assertSameDirectScore(RepairActionCostModel legacy, RepairCostSpecification thesis,
                                       MitigationType legacyType, ActionType thesisType,
                                       List<CompositeLabel> labels) {
        double legacyCost = legacy.costFor(legacyType, labels);
        double thesisCost = new ActionCostEvaluator()
                .evaluate(RepairActionKeys.from(new ActionTerm("element", labels, thesisType)), thesis, null)
                .directCost();
        assertEquals(legacyCost, thesisCost);
    }
}
