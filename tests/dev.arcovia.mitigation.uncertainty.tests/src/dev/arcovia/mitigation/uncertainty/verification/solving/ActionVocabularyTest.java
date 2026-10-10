package dev.arcovia.mitigation.uncertainty.verification.solving;

import dev.arcovia.mitigation.cost.ActionType;
import dev.arcovia.mitigation.cost.RepairCostSpecification;
import dev.arcovia.mitigation.ilp.ActionTerm;
import dev.arcovia.mitigation.ilp.Mitigation;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.uncertainty.preparation.RepairPreparationResult;
import dev.arcovia.mitigation.uncertainty.solving.NoRobustRepairExistsException;
import dev.arcovia.mitigation.uncertainty.solving.RobustILPSolver;
import dev.arcovia.mitigation.uncertainty.solving.RobustSolverResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActionVocabularyTest {

    private static final Set<ActionType> ADD = Set.of(ActionType.Adding);
    private static final Set<ActionType> LABEL = Set.of(ActionType.Adding, ActionType.Removing);
    private static final Set<ActionType> FULL = Set.of();

    @Test
    public void anAdditiveVocabularyCannotCoverAViolationOnlyARemovalCovers() {
        var removalOnly = scenario(
                mitigation("store-1", "Location", "nonEU", ActionType.Removing, 1.0));

        assertThrows(NoRobustRepairExistsException.class,
                () -> solve(removalOnly, ADD),
                "an additive-only vocabulary must report infeasibility, never fall back to a "
                + "removal it was told not to use");
    }

    @Test
    public void addingRemovalToTheVocabularyMakesTheSameProblemSolvable() throws Exception {
        var removalOnly = scenario(
                mitigation("store-1", "Location", "nonEU", ActionType.Removing, 1.0));

        var chosen = solve(removalOnly, LABEL).selectedMitigations();

        assertEquals(1, chosen.size(), "the removal is the only covering action");
        assertEquals(ActionType.Removing, chosen.get(0).mitigation().type());
    }

    @Test
    public void aLabelVocabularyNeverSelectsAStructuralAction() throws Exception {
        var choice = scenario(
                mitigation("service-1", "Stereotype", "encrypted", ActionType.Adding, 1.0),
                mitigation("service-1", "Stereotype", "internal", ActionType.RemoveNode, 1.0));

        var chosen = solve(choice, LABEL).selectedMitigations();

        assertEquals(1, chosen.size(), "one action suffices to cover the single violation");
        assertEquals(ActionType.Adding, chosen.get(0).mitigation().type(),
                "the structural alternative is outside the declared vocabulary and must not be "
                + "selected even when it costs the same");
    }

    @Test
    public void onlyTheFullVocabularyCanRepairAStructurallyCoveredViolation() throws Exception {
        var structuralOnly = scenario(
                mitigation("service-1", "Stereotype", "internal", ActionType.RemoveNode, 1.0));

        assertThrows(NoRobustRepairExistsException.class,
                () -> solve(structuralOnly, ADD),
                "an additive vocabulary cannot express node removal");
        assertThrows(NoRobustRepairExistsException.class,
                () -> solve(structuralOnly, LABEL),
                "a label vocabulary cannot express node removal either");

        var chosen = solve(structuralOnly, FULL).selectedMitigations();

        assertEquals(ActionType.RemoveNode, chosen.get(0).mitigation().type(),
                "the broader vocabulary of this thesis repairs what the other two cannot");
    }

    @Test
    public void anEmptyVocabularyLeavesEveryActionAvailable() throws Exception {
        var choice = scenario(
                mitigation("service-1", "Stereotype", "encrypted", ActionType.Adding, 5.0),
                mitigation("service-1", "Stereotype", "internal", ActionType.RemoveNode, 1.0));

        var declaredEmpty = solve(choice, FULL).objectiveValue();
        var noVocabularyArgument = new RobustILPSolver()
                .solveWithResult(List.of(choice), Set.of(), uniform(), null).objectiveValue();

        assertEquals(noVocabularyArgument, declaredEmpty, 0.0001,
                "an empty vocabulary must mean no restriction, so the comparison arms and the "
                + "ordinary repair path stay the same computation");
    }

    @Test
    public void theVocabularyDoesNotLeakIntoTheNextSolve() throws Exception {
        var removalOnly = scenario(
                mitigation("store-1", "Location", "nonEU", ActionType.Removing, 1.0));
        var solver = new RobustILPSolver();

        assertThrows(NoRobustRepairExistsException.class,
                () -> solver.solveWithResult(List.of(removalOnly), Set.of(), uniform(), null, ADD));
        var chosen = solver
                .solveWithResult(List.of(removalOnly), Set.of(), uniform(), null)
                .selectedMitigations();

        assertEquals(1, chosen.size(), "a restricted solve must not leave the solver restricted; the arms of the "
                                       + "comparison run on one instance and would otherwise contaminate each other");
    }

    private RobustSolverResult solve(RepairPreparationResult scenario, Set<ActionType> vocabulary)
            throws NoRobustRepairExistsException {
        return new RobustILPSolver()
                .solveWithResult(List.of(scenario), Set.of(), uniform(), null, vocabulary);
    }

    private static RepairPreparationResult scenario(Mitigation... alternatives) {
        List<Mitigation> options = List.of(alternatives);
        return new RepairPreparationResult(List.of(), Set.of(), List.of(options), options, List.of());
    }

    private static Mitigation mitigation(String domain, String labelType, String labelValue,
                                         ActionType actionType, double cost) {
        return new Mitigation(
                new ActionTerm(domain, List.of(new NodeLabel(new Label(labelType, labelValue))), actionType),
                cost, List.of());
    }

    private static RepairCostSpecification uniform() {
        return RepairCostSpecification.uniform();
    }
}
