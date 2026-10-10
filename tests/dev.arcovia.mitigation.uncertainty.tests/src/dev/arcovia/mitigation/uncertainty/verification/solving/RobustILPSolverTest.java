package dev.arcovia.mitigation.uncertainty.verification.solving;

import dev.arcovia.mitigation.cost.ActionType;
import dev.arcovia.mitigation.cost.RepairCostSpecification;
import dev.arcovia.mitigation.cost.SharedCostGroup;
import dev.arcovia.mitigation.ilp.*;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.uncertainty.preparation.RepairPreparationResult;
import dev.arcovia.mitigation.uncertainty.solving.NoRobustRepairExistsException;
import dev.arcovia.mitigation.uncertainty.solving.RobustILPSolver;
import dev.arcovia.mitigation.uncertainty.solving.RobustSolverResult;
import dev.arcovia.mitigation.uncertainty.solving.UncertaintyControlledLabelDetector.ControlledLabelKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RobustILPSolverTest {

    @Test
    public void sharesOneActionVariableAcrossScenarioCoverageConstraints() throws Exception {
        Mitigation sharedInScenarioA = mitigation("node-1", "Location", "EU", 1.0, List.of());
        Mitigation sharedInScenarioB = mitigation("node-1", "Location", "EU", 1.0, List.of());

        RepairPreparationResult scenarioA = new RepairPreparationResult(
                List.of(),
                Set.of(),
                List.of(List.of(sharedInScenarioA)),
                List.of(sharedInScenarioA),
                List.of());
        RepairPreparationResult scenarioB = new RepairPreparationResult(
                List.of(),
                Set.of(),
                List.of(List.of(sharedInScenarioB)),
                List.of(sharedInScenarioB),
                List.of());

        List<Mitigation> chosen = new RobustILPSolver().solveWithResult(List.of(scenarioA, scenarioB), Set.of()).selectedMitigations();

        assertEquals(1, chosen.size());
        assertEquals("node-1", chosen.get(0).mitigation().domain());
        assertEquals(ActionType.Adding, chosen.get(0).mitigation().type());
        assertEquals(1.0, chosen.stream().mapToDouble(Mitigation::cost).sum(), 0.0001);
    }

    @Test
    public void usesCanonicalActionCostForDuplicateSemanticActions() throws Exception {
        Mitigation duplicateHighCost = mitigation("node-1", "Location", "EU", 5.0, List.of());
        Mitigation duplicateLowCost = mitigation("node-1", "Location", "EU", 1.0, List.of());

        RepairPreparationResult scenarioA = new RepairPreparationResult(
                List.of(),
                Set.of(),
                List.of(List.of(duplicateHighCost)),
                List.of(duplicateHighCost),
                List.of());
        RepairPreparationResult scenarioB = new RepairPreparationResult(
                List.of(),
                Set.of(),
                List.of(List.of(duplicateLowCost)),
                List.of(duplicateLowCost),
                List.of());

        RepairCostSpecification specification = RepairCostSpecification.builder("canonical-cost", "1")
                .withBaseCost(ActionType.Adding, 7.0)
                .build();
        RobustSolverResult result = new RobustILPSolver()
                .solveWithResult(List.of(scenarioA, scenarioB), Set.of(), specification, null);
        List<Mitigation> chosen = result.selectedMitigations();

        assertEquals(1, chosen.size());
        assertEquals(7.0, result.objectiveValue(), 0.0001,
                "Canonical actions must use their declared descriptor cost, never a scenario candidate score");
    }

    @Test
    public void enforcesScenarioSpecificRequiredMitigationsForSharedActions() throws Exception {
        Mitigation requiredScenarioA = mitigation("node-2", "Guard", "ScenarioA", 1.0, List.of());
        Mitigation requiredScenarioB = mitigation("node-3", "Guard", "ScenarioB", 1.0, List.of());

        Mitigation sharedScenarioA = mitigation("node-1", "Location", "EU", 1.0, List.of(List.of(requiredScenarioA)));
        Mitigation sharedScenarioB = mitigation("node-1", "Location", "EU", 1.0, List.of(List.of(requiredScenarioB)));

        RepairPreparationResult scenarioA = new RepairPreparationResult(
                List.of(),
                Set.of(),
                List.of(List.of(sharedScenarioA)),
                List.of(sharedScenarioA, requiredScenarioA),
                List.of());
        RepairPreparationResult scenarioB = new RepairPreparationResult(
                List.of(),
                Set.of(),
                List.of(List.of(sharedScenarioB)),
                List.of(sharedScenarioB, requiredScenarioB),
                List.of());

        List<Mitigation> chosen = new RobustILPSolver().solveWithResult(List.of(scenarioA, scenarioB), Set.of()).selectedMitigations();

        assertEquals(3, chosen.size());
        assertEquals(3.0, chosen.stream().mapToDouble(Mitigation::cost).sum(), 0.0001);
        assertTrue(chosen.stream().anyMatch(mitigation -> mitigation.mitigation().domain().equals("node-1")));
        assertTrue(chosen.stream().anyMatch(mitigation -> mitigation.mitigation().domain().equals("node-2")));
        assertTrue(chosen.stream().anyMatch(mitigation -> mitigation.mitigation().domain().equals("node-3")));
    }

    @Test
    public void throwsDomainExceptionWhenNoRobustRepairExists() {
        RepairPreparationResult infeasibleScenario = new RepairPreparationResult(
                List.of(),
                Set.of(),
                List.of(List.of()),
                List.of(),
                List.of());

        NoRobustRepairExistsException exception = assertThrows(
                NoRobustRepairExistsException.class,
                () -> new RobustILPSolver().solveWithResult(List.of(infeasibleScenario), Set.of()).selectedMitigations());

        assertTrue(exception.getMessage().contains("No robust repair exists"));
        assertTrue(exception.solverStatus().isPresent());
    }

    @Test
    void rejectsCoverageRequirementsThatNeedContradictoryActions() {
        Mitigation addition = mitigation("node-1", "Location", "EU", ActionType.Adding, 1.0, List.of());
        Mitigation removal = mitigation("node-1", "Location", "EU", ActionType.Removing, 1.0, List.of());
        RepairPreparationResult scenario = new RepairPreparationResult(
                List.of(), Set.of(), List.of(List.of(addition), List.of(removal)),
                List.of(addition, removal), List.of(List.of(addition, removal)));

        assertThrows(NoRobustRepairExistsException.class,
                () -> new RobustILPSolver().solveWithResult(List.of(scenario), Set.of()));
    }

    @Test
    void forbidsControlledRemovalButAllowsStableAddition() throws Exception {
        Set<ControlledLabelKey> controlledLabels = Set.of(new ControlledLabelKey("node-1", "Location", "nonEU"));
        Mitigation removal = mitigation("node-1", "Location", "nonEU", ActionType.Removing, 1.0, List.of());
        RepairPreparationResult removalOnly = new RepairPreparationResult(
                List.of(), Set.of(), List.of(List.of(removal)), List.of(removal), List.of());

        assertThrows(NoRobustRepairExistsException.class,
                () -> new RobustILPSolver().solveWithResult(List.of(removalOnly), controlledLabels));

        Mitigation stableAddition = mitigation("node-1", "Encrypted", "true", ActionType.Adding, 1.0, List.of());
        RepairPreparationResult additionOnly = new RepairPreparationResult(
                List.of(), Set.of(), List.of(List.of(stableAddition)), List.of(stableAddition), List.of());

        assertEquals(List.of(stableAddition), new RobustILPSolver()
                .solveWithResult(List.of(additionOnly), controlledLabels)
                .selectedMitigations());
    }

    @Test
    public void selectsCheaperMitigationWhenAlternativesExist() throws Exception {
        Mitigation cheap = mitigation("node-cheap", "Location", "EU", 1.0, List.of());
        Mitigation expensive = mitigation("node-expensive", "Location", "EU", 5.0, List.of());

        RepairPreparationResult scenario = new RepairPreparationResult(
                List.of(),
                Set.of(),
                List.of(List.of(cheap, expensive)),
                List.of(cheap, expensive),
                List.of());

        RepairCostSpecification specification = RepairCostSpecification.builder("alternative-costs", "1")
                .withActionCostOverride(RepairActionKeys.from(cheap), 1.0)
                .withActionCostOverride(RepairActionKeys.from(expensive), 5.0)
                .build();
        List<Mitigation> chosen = new RobustILPSolver()
                .solveWithResult(List.of(scenario), Set.of(), specification, null)
                .selectedMitigations();

        assertEquals(1, chosen.size(), "Exactly one mitigation should be selected");
        assertEquals("node-cheap", chosen.get(0).mitigation().domain(),
                "Solver must prefer the cheaper mitigation (cost 1.0 over 5.0)");
        assertEquals(1.0, chosen.stream().mapToDouble(Mitigation::cost).sum(), 0.0001,
                "Total cost of chosen mitigations must equal the cheaper option");
    }

    @Test
    void chargesOneSharedEnablerForTwoSelectedActions() throws Exception {
        Mitigation first = mitigation("node-1", "Encrypted", "true", 99.0, List.of());
        Mitigation second = mitigation("node-2", "Encrypted", "true", 99.0, List.of());
        RepairCostSpecification specification = RepairCostSpecification.builder("shared-enabler", "1")
                .withBaseCost(ActionType.Adding, 1.0)
                .withSharedCostGroup(new SharedCostGroup("key-management", 5.0,
                        Set.of(RepairActionKeys.from(first), RepairActionKeys.from(second))))
                .build();
        RepairPreparationResult scenario = new RepairPreparationResult(
                List.of(), Set.of(), List.of(List.of(first), List.of(second)), List.of(first, second), List.of());

        RobustSolverResult result = new RobustILPSolver()
                .solveWithResult(List.of(scenario), Set.of(), specification, null);

        assertEquals(2, result.selectedMitigations().size());
        assertEquals(2.0, result.costBreakdown().directActionCost());
        assertEquals(5.0, result.costBreakdown().sharedEnablerCost());
        assertEquals(7.0, result.objectiveValue());
        assertEquals(List.of("key-management"), result.costBreakdown().selectedSharedCostGroupIds());
    }

    @Test
    void doesNotChargeAnUnusedSharedEnabler() throws Exception {
        Mitigation selected = mitigation("node-1", "Encrypted", "true", 99.0, List.of());
        Mitigation unavailableMember = mitigation("node-2", "Encrypted", "true", 99.0, List.of());
        RepairCostSpecification specification = RepairCostSpecification.builder("unused-enabler", "1")
                .withBaseCost(ActionType.Adding, 1.0)
                .withSharedCostGroup(new SharedCostGroup("key-management", 5.0,
                        Set.of(RepairActionKeys.from(unavailableMember))))
                .build();
        RepairPreparationResult scenario = new RepairPreparationResult(
                List.of(), Set.of(), List.of(List.of(selected)), List.of(selected), List.of());

        RobustSolverResult result = new RobustILPSolver()
                .solveWithResult(List.of(scenario), Set.of(), specification, null);

        assertEquals(1.0, result.objectiveValue());
        assertEquals(0.0, result.costBreakdown().sharedEnablerCost());
        assertTrue(result.costBreakdown().selectedSharedCostGroupIds().isEmpty());
    }

    @Test
    void selectsTheCheaperOfTwoCoveringActionsUnderEachProfile() throws Exception {
        Mitigation encrypt = mitigation("encrypt", "Encrypted", "true", 99.0, List.of());
        Mitigation remove = mitigation("remove", "Telemetry", "optional", ActionType.Removing, 99.0, List.of());
        RepairPreparationResult scenario = new RepairPreparationResult(
                List.of(), Set.of(), List.of(List.of(encrypt, remove)), List.of(encrypt, remove), List.of());

        assertProfileSelects(scenario, encrypt, remove, 1, 4, "encrypt", 1);
        assertProfileSelects(scenario, encrypt, remove, 20, 6, "remove", 6);
        assertProfileSelects(scenario, encrypt, remove, 5, 6, "encrypt", 5);
    }

    private void assertProfileSelects(RepairPreparationResult scenario, Mitigation encrypt, Mitigation remove,
                                      double encryptCost, double removeCost, String expectedDomain,
                                      double expectedObjective) throws Exception {
        RepairCostSpecification specification = RepairCostSpecification.builder("retail", "1")
                .withActionCostOverride(RepairActionKeys.from(encrypt), encryptCost)
                .withActionCostOverride(RepairActionKeys.from(remove), removeCost)
                .build();

        RobustSolverResult result = new RobustILPSolver()
                .solveWithResult(List.of(scenario), Set.of(), specification, null);

        assertEquals(expectedDomain, result.selectedMitigations().get(0).mitigation().domain());
        assertEquals(expectedObjective, result.objectiveValue());
    }

    @Test
    public void exposesSolverStatusInStructuredSolverResult() throws Exception {
        Mitigation mitigation = mitigation("node-1", "Location", "EU", 1.0, List.of());

        RepairPreparationResult scenario = new RepairPreparationResult(
                List.of(),
                Set.of(),
                List.of(List.of(mitigation)),
                List.of(mitigation),
                List.of());

        RobustSolverResult result = new RobustILPSolver().solveWithResult(List.of(scenario), Set.of());

        assertEquals(1, result.selectedMitigations().size());
        assertTrue(List.of("OPTIMAL", "FEASIBLE").contains(result.solverStatus()));
    }

    @Test
    public void prefersCheaperStructuralRepairFromActionCostModel() throws Exception {
        RepairActionCostModel costModel = RepairActionCostModel.standard();

        MitigationStrategy removeNodeStrategy = MitigationStrategy.withCostModel(
                List.of(new NodeLabel(new Label("Stereotype", "internal"))),
                MitigationType.DeleteNode,
                costModel);
        MitigationStrategy addNodeStrategy = MitigationStrategy.withCostModel(
                List.of(new NodeLabel(new Label("Stereotype", "firewall"))),
                MitigationType.AddNode,
                costModel);

        assertTrue(addNodeStrategy.cost() > removeNodeStrategy.cost(),
                "Thesis default cost model must make AddNode more expensive than DeleteNode");

        Mitigation removeNode = mitigation("node-remove", "Stereotype", "internal", ActionType.RemoveNode,
                removeNodeStrategy.cost(), List.of());
        Mitigation addNode = mitigation("flow-add", "Stereotype", "firewall", ActionType.AddNode,
                addNodeStrategy.cost(), List.of());

        RepairPreparationResult scenario = new RepairPreparationResult(
                List.of(),
                Set.of(),
                List.of(List.of(removeNode, addNode)),
                List.of(removeNode, addNode),
                List.of());

        List<Mitigation> chosen = new RobustILPSolver().solveWithResult(List.of(scenario), Set.of()).selectedMitigations();

        assertEquals(1, chosen.size(), "Exactly one structural repair should be selected");
        assertEquals(ActionType.RemoveNode, chosen.get(0).mitigation().type(),
                "Solver must prefer the cheaper DeleteNode action over AddNode");
        assertEquals(removeNodeStrategy.cost(), chosen.stream().mapToDouble(Mitigation::cost).sum(), 0.0001,
                "Chosen structural repair cost must match the cheaper DeleteNode action");
    }

    private Mitigation mitigation(String domain, String labelType, String labelValue, double cost,
                                  List<List<Mitigation>> required) {
        return mitigation(domain, labelType, labelValue, ActionType.Adding, cost, required);
    }

    private Mitigation mitigation(String domain, String labelType, String labelValue, ActionType actionType,
                                  double cost, List<List<Mitigation>> required) {
        return new Mitigation(
                new ActionTerm(domain, List.of(new NodeLabel(new Label(labelType, labelValue))), actionType),
                cost,
                required);
    }
}
