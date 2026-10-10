package dev.arcovia.mitigation.uncertainty.verification.integration;

import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.uncertainty.RobustRepairResult;
import dev.arcovia.mitigation.uncertainty.UncertaintyAwareOptimizationManager;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelLoader;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelSpec;
import dev.arcovia.mitigation.uncertainty.pruning.TopologyEffect;
import dev.arcovia.mitigation.uncertainty.validation.RobustRepairValidationException;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repairs the fixtures that isolate one uncertainty source type each.
 */
class PerTypeRepairTest {

    private final Path current = Path.of(System.getProperty("user.dir"));


    @Test
    public void behaviorUncertaintyIsRepairedAndStaysReducible() throws Exception {
        LoadedUncertaintyModel loaded = load("DFDBehaviorUncertaintyMitigation", "beh");

        assertFalse(TopologyEffect.anyChangesTopology(loaded.sources()),
                "behavior sources are primary, so the reductions must remain applicable");

        RobustRepairResult result = repair(loaded);

        assertEquals(RobustRepairResult.ValidationStatus.PASSED, result.validationStatus(),
                "the returned plan must hold in every declared behavior scenario");
        assertTrue(result.consideredScenarioCount() <= 16,
                "four binary sources bound the space at 16 scenarios; the reductions may lower it");
    }

    @Test
    public void componentUncertaintyIsCaughtByValidation() {
        var loaded = load("DFDComponentUncertaintyMitigation", "comp");

        assertTrue(loaded.sources().stream().allMatch(TopologyEffect::changesTopology),
                "component sources are secondary, so both reductions must be disabled");

        var rejected = assertThrows(RobustRepairValidationException.class,
                () -> repair(loaded),
                "a plan that materialization undoes must be rejected, never returned as valid");
        assertTrue(rejected.getMessage().contains("32 considered scenario"),
                "under secondary uncertainty the full product of five binary sources is solved, so the reductions must not have shrunk the space: " + rejected.getMessage());
    }

    private RobustRepairResult repair(LoadedUncertaintyModel loaded) throws Exception {
        var manager = new UncertaintyAwareOptimizationManager(
                loaded, List.of(personalAtNonEuMustBeEncrypted()), false, loaded.sources());
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

    private LoadedUncertaintyModel load(String folder, String prefix) {
        var models = current.resolve("models").resolve(folder);
        return new UncertaintyModelLoader().load(new UncertaintyModelSpec(
                models.resolve(prefix + ".dataflowdiagram"),
                models.resolve(prefix + ".datadictionary"),
                models.resolve(prefix + ".uncertainty")));
    }
}
