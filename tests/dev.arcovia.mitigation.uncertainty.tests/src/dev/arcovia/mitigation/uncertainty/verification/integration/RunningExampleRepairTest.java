package dev.arcovia.mitigation.uncertainty.verification.integration;

import dev.arcovia.mitigation.cost.ActionType;
import dev.arcovia.mitigation.cost.RepairCostSpecification;
import dev.arcovia.mitigation.ilp.ActionTerm;
import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.ilp.Mitigation;
import dev.arcovia.mitigation.ilp.OptimizationManager;
import dev.arcovia.mitigation.uncertainty.RobustRepairResult;
import dev.arcovia.mitigation.uncertainty.UncertaintyAwareOptimizationManager;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationConstraints;
import dev.arcovia.mitigation.uncertainty.evaluation.fixtures.RunningExampleFixture;
import dev.arcovia.mitigation.uncertainty.pipeline.ScenarioPreparationService;
import dev.arcovia.mitigation.uncertainty.validation.RobustRepairValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.arcovia.mitigation.uncertainty.evaluation.EvaluationConstraints.PERSONAL_OUTSIDE_EU_MUST_BE_ENCRYPTED;
import static dev.arcovia.mitigation.uncertainty.evaluation.fixtures.RunningExampleFixture.FRAUD;
import static dev.arcovia.mitigation.uncertainty.evaluation.fixtures.RunningExampleFixture.RECOMMENDATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RunningExampleRepairTest {

    @Test
    public void baseModelIsCleanAndThreeOfFourVariantsViolate() {
        var loaded = RunningExampleFixture.build();
        var preparations = new ScenarioPreparationService()
                .prepare(loaded.baseModel(), loaded.sources(), constraints());

        assertEquals(4, preparations.size(), "two binary sources span four scenarios");
        var violating = preparations.stream().collect(Collectors.toMap(
                preparation -> preparation.scenario().id(),
                preparation -> preparation.preparation().violatingNodes().stream()
                        .map(Object::toString).collect(Collectors.toSet())));
        assertEquals(1, violating.values().stream().filter(Set::isEmpty).count(),
                "exactly the EU/EU variant is clean");
        assertEquals(3, violating.values().stream().filter(nodes -> !nodes.isEmpty()).count(),
                "the other three variants violate the residency rule");
        assertTrue(violating.entrySet().stream()
                        .filter(entry -> entry.getKey().contains(FRAUD + "_nonEU") && !entry.getKey().contains(RECOMMENDATION + "_nonEU"))
                        .allMatch(entry -> entry.getValue().size() == 1),
                "nonEU/EU violates at one service only");
    }

    @Test
    public void nominalRepairSelectsNothingWhileRobustRepairAddsEncryptionOnce() throws Exception {
        var nominal = new OptimizationManager(RunningExampleFixture.build().baseModel(), constraints(), false);
        nominal.repair();
        assertEquals(0.0, nominal.getObjectiveValue(), "the base model has no violation to repair");

        var loaded = RunningExampleFixture.build();
        var robust = new UncertaintyAwareOptimizationManager(loaded, constraints(), false, loaded.sources());
        robust.repair();
        var result = robust.getRobustRepairResult().orElseThrow();

        assertEquals(RobustRepairResult.ValidationStatus.PASSED, result.validationStatus());
        assertEquals(1, result.selectedActions().size(), "one canonical action covers all three violations");
        assertEquals(ActionType.Adding, result.selectedActions().get(0).type());
        assertEquals("Encrypted", result.selectedActions().get(0).compositeLabels().get(0).label().value());

        var validation = new RobustRepairValidator(new ScenarioPreparationService())
                .validate(result.repairedModel(), loaded.sources(), constraints());
        assertEquals(4, validation.scenarioCount());
        assertEquals(0, validation.totalViolationCount(), "the repaired base holds in every variant");
    }

    @Test
    public void flowRemovalIsProposedForBothAnalyticsFlowsAndPricedOut() throws Exception {
        var loaded = RunningExampleFixture.build();
        var preparations = new ScenarioPreparationService()
                .prepare(loaded.baseModel(), loaded.sources(), constraints());

        var removedFlows = preparations.stream()
                .flatMap(preparation -> preparation.preparation().allMitigations().stream())
                .map(Mitigation::mitigation)
                .filter(action -> action.type() == ActionType.RemoveFlow)
                .map(ActionTerm::domain)
                .collect(Collectors.toSet());
        assertEquals(Set.of("fraud", "recommendation"), removedFlows,
                "flow removal targets the flows into the violating services, not the one into the adapter");

        var robust = new UncertaintyAwareOptimizationManager(loaded, constraints(), false, loaded.sources());
        robust.setRepairCostSpecification(RepairCostSpecification.uniform());
        robust.repair();
        var result = robust.getRobustRepairResult().orElseThrow();
        assertEquals(1, result.selectedActions().size(), "one addition beats two flow removals under uniform costs");
        assertEquals(ActionType.Adding, result.selectedActions().get(0).type());
    }

    private static List<Constraint> constraints() {
        return List.of(EvaluationConstraints.withFlowRemoval(PERSONAL_OUTSIDE_EU_MUST_BE_ENCRYPTED));
    }
}
