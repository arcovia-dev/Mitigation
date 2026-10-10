package dev.arcovia.mitigation.uncertainty.verification.integration;

import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.uncertainty.RobustRepairResult;
import dev.arcovia.mitigation.uncertainty.UncertaintyAwareOptimizationManager;
import dev.arcovia.mitigation.uncertainty.evaluation.fixtures.TwoSubsystemsFixture;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import dev.arcovia.mitigation.uncertainty.pruning.SourceImpactFilter;
import dev.arcovia.mitigation.uncertainty.pruning.SourcePartitioner;
import dev.arcovia.mitigation.uncertainty.pruning.TopologyEffect;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shows that independence decomposition reduces the considered scenario space when every source
 * is primary, that is, when no source rewires the diagram.
 */
class PrimaryOnlyDecompositionTest {


    @Test
    public void everySourceIsPrimarySoTheReductionsApply() {
        LoadedUncertaintyModel loaded = loadModel();

        assertEquals(4, loaded.sources().size(), "the fixture declares four uncertainty sources");
        assertTrue(loaded.sources().stream().noneMatch(TopologyEffect::changesTopology),
                "all four sources must be external, otherwise the reductions are disabled and this fixture cannot demonstrate decomposition");
    }

    @Test
    public void twoDisjointSubsystemsFormTwoClusters() {
        var loaded = loadModel();
        var relevant = new SourceImpactFilter().retainImpactful(loaded.baseModel(), loaded.sources());

        var clusters = new SourcePartitioner().partition(loaded.baseModel(), relevant);

        assertEquals(2, clusters.size(), "the subsystems share no transpose flow graph, so they must split");
        assertEquals(2, clusters.get(0).size(), "each cluster holds the two sources of one subsystem");
        assertEquals(2, clusters.get(1).size(), "each cluster holds the two sources of one subsystem");
    }

    @Test
    public void decompositionShrinksTheSpaceAndKeepsTheSameRepair() throws Exception {
        var decomposed = repair(true);
        var wholeSet = repair(false);

        assertEquals(16, wholeSet.consideredScenarioCount(),
                "without decomposition the four binary sources give the full product");
        assertEquals(8, decomposed.consideredScenarioCount(),
                "decomposition replaces the product 2^2 * 2^2 with the sum 2^2 + 2^2");

        assertEquals(wholeSet.objectiveValue(), decomposed.objectiveValue(), 1e-9,
                "a sound reduction must not change the cost-minimal objective");
        assertEquals(wholeSet.selectedActions().size(), decomposed.selectedActions().size(),
                "a sound reduction must not change the size of the selected plan");
        assertEquals(RobustRepairResult.ValidationStatus.PASSED, decomposed.validationStatus(),
                "the decomposed repair must still validate over the full declared space");
    }

    private RobustRepairResult repair(boolean partitioning) throws Exception {
        var loaded = loadModel();
        var manager = new UncertaintyAwareOptimizationManager(
                loaded, List.of(personalAtNonEuMustBeEncrypted()), false, loaded.sources());
        manager.setPartitioningEnabled(partitioning);
        manager.repair();
        return manager.getRobustRepairResult().orElseThrow();
    }

    private static Constraint personalAtNonEuMustBeEncrypted() {
        return new Constraint(new ConstraintDSL().ofData()
                .withLabel("Sensitivity", "Personal")
                .withoutLabel("Encryption", "Encrypted")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Location", "nonEU")
                .create());
    }

    private LoadedUncertaintyModel loadModel() {
        return TwoSubsystemsFixture.build();
    }
}
