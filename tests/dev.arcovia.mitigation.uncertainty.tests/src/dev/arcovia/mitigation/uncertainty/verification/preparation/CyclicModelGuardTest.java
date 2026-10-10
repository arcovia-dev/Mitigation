package dev.arcovia.mitigation.uncertainty.verification.preparation;

import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelLoader;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelSpec;
import dev.arcovia.mitigation.uncertainty.preparation.RepairPreparationResult;
import dev.arcovia.mitigation.uncertainty.preparation.RepairPreparationService;
import dev.arcovia.mitigation.uncertainty.preparation.ViolationAnalysis;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins both halves of the cyclic-model contract.
 */
class CyclicModelGuardTest {

    private final Path current = Path.of(System.getProperty("user.dir"));

    @Test
    public void theDefaultRefusesACyclicModel() {
        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> new ViolationAnalysis().analyze(cyclicModel(), untrustedMustNotReachExternal()),
                "the default must not return a result derived from a heuristically unrolled cycle");
        assertTrue(refused.getMessage().contains("cyclic data flow"),
                "the refusal must name the reason: " + refused.getMessage());
    }

    @Test
    public void anOptedInCallerAnalysesTheCyclicModelAndIsToldItWasCyclic() {
        ViolationAnalysis.Reading reading =
                new ViolationAnalysis(true).analyze(cyclicModel(), untrustedMustNotReachExternal());

        assertTrue(reading.cyclic(),
                "an opted-in reading must carry the flag, otherwise the stratum cannot be "
                + "reconstructed from the recorded data");
    }

    @Test
    public void anAcyclicModelIsNotFlaggedUnderEitherSetting() {
        DataFlowDiagramAndDictionary model = acyclicModel();

        ViolationAnalysis.Reading strict = new ViolationAnalysis().analyze(model, personalAtNonEuMustBeEncrypted());
        ViolationAnalysis.Reading permissive = new ViolationAnalysis(true).analyze(model, personalAtNonEuMustBeEncrypted());

        assertFalse(strict.cyclic(), "the acyclic fixture must not be reported as cyclic");
        assertFalse(permissive.cyclic(), "allowing cycles must not change what is reported");
        assertEquals(strict.violatingNodes().size(), permissive.violatingNodes().size(),
                "allowing cycles must not change the reading of an acyclic model");
    }

    @Test
    public void thePreparationServiceCarriesTheFlagIntoItsResult() {
        RepairPreparationResult prepared = new RepairPreparationService(true)
                .prepare(cyclicModel(), untrustedMustNotReachExternal());

        assertTrue(prepared.analysedModelWasCyclic(),
                "the stratum has to be reconstructible from the preparation result alone");
    }

    private static List<Constraint> untrustedMustNotReachExternal() {
        return List.of(new Constraint(new ConstraintDSL().ofData()
                .withoutLabel("Stereotype", "trusted")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Stereotype", "external")
                .create()));
    }

    private static List<Constraint> personalAtNonEuMustBeEncrypted() {
        return List.of(new Constraint(new ConstraintDSL().ofData()
                .withLabel("Sensitivity", "Personal")
                .withoutLabel("Encryption", "Encrypted")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Location", "nonEU")
                .create()));
    }

    private DataFlowDiagramAndDictionary cyclicModel() {
        return load(current.getParent().resolve("dev.arcovia.mitigation.ranking.tests")
                .resolve("models").resolve("LoginFramework5"), "default");
    }

    private DataFlowDiagramAndDictionary acyclicModel() {
        return load(current.resolve("models").resolve("DFDExternalUncertaintyMitigation"), "ext");
    }

    private DataFlowDiagramAndDictionary load(Path folder, String prefix) {
        LoadedUncertaintyModel loaded = new UncertaintyModelLoader().load(new UncertaintyModelSpec(
                folder.resolve(prefix + ".dataflowdiagram"),
                folder.resolve(prefix + ".datadictionary"),
                folder.resolve(prefix + ".uncertainty")));
        return loaded.baseModel();
    }
}
