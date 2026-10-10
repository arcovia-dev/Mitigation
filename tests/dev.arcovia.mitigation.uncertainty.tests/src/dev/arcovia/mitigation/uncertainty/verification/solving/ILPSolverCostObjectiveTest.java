package dev.arcovia.mitigation.uncertainty.verification.solving;

import dev.arcovia.mitigation.cost.ActionType;
import dev.arcovia.mitigation.cost.RepairCostSpecification;
import dev.arcovia.mitigation.cost.SharedCostGroup;
import dev.arcovia.mitigation.ilp.ActionTerm;
import dev.arcovia.mitigation.ilp.ILPSolver;
import dev.arcovia.mitigation.ilp.Mitigation;
import dev.arcovia.mitigation.ilp.RepairActionKeys;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.NodeLabel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ILPSolverCostObjectiveTest {

    @Test
    void appliesTheSameSharedObjectiveToBaselineRepair() throws Exception {
        var first = mitigation("node-1");
        var second = mitigation("node-2");
        var specification = RepairCostSpecification.builder("shared-enabler", "1")
                .withBaseCost(ActionType.Adding, 1)
                .withSharedCostGroup(new SharedCostGroup("key-management", 5,
                        Set.of(RepairActionKeys.from(first), RepairActionKeys.from(second))))
                .build();

        var result = new ILPSolver().solveWithResult(
                List.of(List.of(first), List.of(second)), Set.of(first, second), List.of(), specification, null);

        assertEquals(2, result.selectedMitigations().size()); // each violation needs one
        assertEquals(2, result.costBreakdown().directActionCost()); // 1 + 1
        assertEquals(5, result.costBreakdown().sharedEnablerCost()); // group charged once
        assertEquals(7, result.costBreakdown().objectiveValue()); // 2 + 5
    }

    private Mitigation mitigation(String domain) {
        return new Mitigation(new ActionTerm(domain, List.of(new NodeLabel(new Label("Encrypted", "true"))), ActionType.Adding), 99 /* legacy cost */, List.of());
    }
}
