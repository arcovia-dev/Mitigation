package dev.arcovia.mitigation.uncertainty.verification.solving;

import dev.arcovia.mitigation.cost.*;
import dev.arcovia.mitigation.ilp.ActionTerm;
import dev.arcovia.mitigation.ilp.RepairActionKeys;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.NodeLabel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepairCostSpecificationTest {

    @Test
    void evaluatesEveryDirectCostComponent() {
        ActionTerm action = new ActionTerm("database",
                List.of(new NodeLabel(new Label("Location", "EU"))), ActionType.Adding);
        RepairCostSpecification specification = RepairCostSpecification.builder("locally-calibrated", "1")
                .withUnit("person-hours")
                .withBaseCost(ActionType.Adding, 2)
                .withCoefficients(3, 0, 4, 5)
                .withAssuranceActivities(ActionType.Adding, 2)
                .build();

        ActionCostDescriptor descriptor = new ActionCostEvaluator()
                .evaluate(RepairActionKeys.from(action), specification, null);

        assertEquals(1, descriptor.impact());
        assertEquals(0, descriptor.ripple());
        assertEquals(0, descriptor.structuralMagnitude());
        assertEquals(2, descriptor.assuranceActivities());
        assertEquals(2, descriptor.baseCost());
        assertEquals(3, descriptor.impactCost());
        assertEquals(0, descriptor.rippleCost());
        assertEquals(0, descriptor.structuralCost());
        assertEquals(10, descriptor.assuranceCost());
        assertEquals(15, descriptor.directCost());
    }

    @Test
    void usesExplicitActionOverridesWithoutChangingDescriptors() {
        ActionTerm action = new ActionTerm("database",
                List.of(new NodeLabel(new Label("Location", "EU"))), ActionType.Adding);
        RepairActionKey key = RepairActionKeys.from(action);
        RepairCostSpecification specification = RepairCostSpecification.builder("overrides", "1")
                .withBaseCost(ActionType.Adding, 2)
                .withActionCostOverride(key, 7.5)
                .build();

        ActionCostDescriptor descriptor = new ActionCostEvaluator()
                .evaluate(RepairActionKeys.from(action), specification, null);

        assertTrue(descriptor.overridden());
        assertEquals(7.5, descriptor.directCost());
        assertEquals(2, descriptor.baseCost());
    }

    @Test
    void rejectsNegativeAndNonFiniteInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> RepairCostSpecification.builder("invalid", "1").withBaseCost(ActionType.Adding, -1));
        assertThrows(IllegalArgumentException.class,
                () -> RepairCostSpecification.builder("invalid", "1")
                        .withCoefficients(Double.NaN, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> RepairCostSpecification.builder("invalid", "1")
                        .withAssuranceActivities(ActionType.Adding, Double.POSITIVE_INFINITY));
    }
}
