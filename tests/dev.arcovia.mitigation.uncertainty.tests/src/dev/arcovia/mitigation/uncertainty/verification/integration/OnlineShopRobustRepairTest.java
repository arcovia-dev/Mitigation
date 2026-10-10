package dev.arcovia.mitigation.uncertainty.verification.integration;

import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.uncertainty.RobustRepairResult;
import dev.arcovia.mitigation.uncertainty.UncertaintyAwareOptimizationManager;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelLoader;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelSpec;
import dev.arcovia.mitigation.uncertainty.output.RepairOutput;
import dev.arcovia.mitigation.uncertainty.pipeline.ScenarioPreparationService;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OnlineShopRobustRepairTest {
    private final Path current = Path.of(System.getProperty("user.dir"));

    @Test
    public void repairsOnlineShopDfdAcrossAllScenarios(@TempDir Path outputDirectory) throws Exception {
        var loadedModel = loadOnlineShopModel();
        var sources = loadedModel.sources();
        var constraint = personalDataMustNotFlowToNonEuNode();

        var initialPreparations = new ScenarioPreparationService().prepare(loadedModel, sources, List.of(constraint));
        assertEquals(4, initialPreparations.size(),
                "Two binary ABUNAI uncertainty sources must yield four materialized scenarios");
        assertTrue(initialPreparations.stream()
                        .anyMatch(preparation -> !preparation.preparation().violatingNodes().isEmpty()),
                "The unresolved online-shop model must contain at least one violating scenario");

        var optimization = new UncertaintyAwareOptimizationManager(loadedModel, List.of(constraint), false, sources);
        var output = RepairOutput.writingTo(outputDirectory, "online-shop-robust-repair");
        optimization.enableRepairOutput(output);
        var repairedModel = optimization.repair();
        var repairResult = optimization.getRobustRepairResult().orElseThrow();

        assertEquals(4, repairResult.consideredScenarioCount());
        assertTrue(repairResult.preRepairViolationCount() > 0);
        assertEquals(0, repairResult.postRepairViolationCount());
        assertEquals(RobustRepairResult.ValidationStatus.PASSED, repairResult.validationStatus());
        assertTrue(Files.exists(output.dataFlowDiagramPath().orElseThrow()));
        assertTrue(Files.exists(output.dataDictionaryPath().orElseThrow()));
        assertTrue(Files.exists(output.repairReportPath().orElseThrow()));
        assertTrue(Files.readString(output.repairReportPath().orElseThrow()).contains("\"trace\""));

        assertFalse(hasNonEUDatabaseLocation(repairedModel),
                "Robust repair should remove the concrete nonEU location that causes the default scenario violation");

        var repairedPreparations = new ScenarioPreparationService().prepare(repairedModel, sources, List.of(constraint));
        assertEquals(4, repairedPreparations.size(),
                "Validation must re-check the complete Cartesian product after repair");
        assertTrue(repairedPreparations.stream()
                        .allMatch(preparation -> preparation.preparation().violatingNodes().isEmpty()),
                "Robust repair must produce a violation-free result in every materialized online-shop scenario");
    }

    private Constraint personalDataMustNotFlowToNonEuNode() {
        return new Constraint(new ConstraintDSL().ofData()
                .withLabel("Sensitivity", "Personal")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Location", "nonEU")
                .create());
    }

    private LoadedUncertaintyModel loadOnlineShopModel() {
        var modelFolder = current.resolve("models")
                .resolve("UncertainOnlineShopDFD");
        return new UncertaintyModelLoader().load(new UncertaintyModelSpec(
                modelFolder.resolve("onlineshop.dataflowdiagram"),
                modelFolder.resolve("onlineshop.datadictionary"),
                modelFolder.resolve("onlineshop.uncertainty")));
    }

    private boolean hasNonEUDatabaseLocation(DataFlowDiagramAndDictionary model) {
        return getNodeLabelNames(model).contains("nonEU");
    }

    private List<String> getNodeLabelNames(DataFlowDiagramAndDictionary model) {
        return model.dataFlowDiagram().getNodes().stream()
                .filter(node -> node.getEntityName().equals("Database"))
                .findFirst()
                .orElseThrow()
                .getProperties().stream()
                .filter(label -> ((LabelType) label.eContainer()).getEntityName().equals("Location"))
                .map(Label::getEntityName)
                .toList();
    }
}
