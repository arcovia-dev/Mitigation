package dev.arcovia.mitigation.uncertainty.evaluation.support;

import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationArtifacts;
import dev.arcovia.mitigation.uncertainty.evaluation.RecordCsv;
import dev.arcovia.mitigation.uncertainty.helper.RecordTableRenderer;
import dev.arcovia.mitigation.uncertainty.helper.ReportColumn;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelLoader;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelSpec;
import dev.arcovia.mitigation.uncertainty.preparation.ViolationAnalysis;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.Activator;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Counts how much of the available model population the acyclicity precondition excludes.
 */
public class CorpusCyclicityTest {

    private final Path uncertaintyModels = Path.of(System.getProperty("user.dir")).resolve("models");
    private final Path rankingModels = Path.of(System.getProperty("user.dir")).getParent()
            .resolve("dev.arcovia.mitigation.ranking.tests").resolve("models");

    @Test
    @EnabledIfSystemProperty(named = "arcovia.evaluation", matches = "true",
            disabledReason = "opt-in evaluation run; set -Darcovia.evaluation=true")
    public void reportsHowManyAvailableModelsTheAcyclicityPreconditionExcludes() throws Exception {
        EvaluationArtifacts.suppressRepeatedAnalysisWarnings();

        var readings = Stream.concat(
                microSecEndBaseModels().map(model -> new Reading(model, "microSecEnD", cyclic(microSecEnd(model)))),
                annotatedModels().map(Annotated::read)).toList();
        long cyclic = readings.stream().filter(Reading::cyclic).count();

        var directory = EvaluationArtifacts.createRunDirectory(Path.of(System.getProperty("user.dir")), "corpus-cyclicity");
        EvaluationArtifacts.write(directory, "report.txt", "Cyclicity of the available models, as reported by the analysis\n\n"
                                                           + RecordTableRenderer.render(readings, Reading.class)
                                                           + cyclic + " of " + readings.size() + " models are cyclic.\n");
        EvaluationArtifacts.write(directory, "summary.csv", RecordCsv.table(Reading.class, readings));

        assertTrue(cyclic > 0, "at least one model must be cyclic, otherwise the precondition excludes nothing");
    }

    private static Stream<String> microSecEndBaseModels() {
        return TuhhModels.getTuhhModels().entrySet().stream()
                .filter(entry -> entry.getValue().contains(0))
                .map(java.util.Map.Entry::getKey)
                .sorted();
    }

    private Stream<Annotated> annotatedModels() {
        return Stream.of(
                new Annotated(uncertaintyModels.resolve("DFDExternalUncertaintyMitigation"), "ext", "constructed"),
                new Annotated(uncertaintyModels.resolve("UncertainOnlineShopDFD"), "onlineshop", "ABUNAI"),
                new Annotated(uncertaintyModels.resolve("mitigation_example"), "mitigation_example", "reused code base"),
                new Annotated(rankingModels.resolve("jferrater"), "jferrater", "microSecEnD"),
                new Annotated(rankingModels.resolve("OBM"), "OBM", "ranking line"),
                new Annotated(rankingModels.resolve("LoginFramework5"), "default", "ranking line"),
                new Annotated(rankingModels.resolve("LoginFramework10"), "default", "ranking line"),
                new Annotated(rankingModels.resolve("koushikkothagal"), "koushikkothagal", "microSecEnD, ranking line"),
                new Annotated(rankingModels.resolve("jferrater10"), "jferrater", "microSecEnD, ranking line"),
                new Annotated(uncertaintyModels.resolve("DFDBehaviorUncertaintyMitigation"), "beh", "ranking line"),
                new Annotated(uncertaintyModels.resolve("DFDComponentUncertaintyMitigation"), "comp", "ranking line"),
                new Annotated(uncertaintyModels.resolve("DFDInterfaceUncertaintyMitigation"), "int", "ranking line"),
                new Annotated(uncertaintyModels.resolve("DFDExternalUncertainty"), "default", "ABUNAI"),
                new Annotated(uncertaintyModels.resolve("DFDBehaviorUncertainty"), "default", "ABUNAI"),
                new Annotated(uncertaintyModels.resolve("DFDComponentUncertainty"), "default", "ABUNAI"),
                new Annotated(uncertaintyModels.resolve("DFDConnectorUncertainty"), "default", "ABUNAI"),
                new Annotated(uncertaintyModels.resolve("DFDInterfaceUncertainty"), "default", "ABUNAI"));
    }

    private static boolean cyclic(DataFlowDiagramAndDictionary model) {
        var anyInternalFlow = new Constraint(new ConstraintDSL().ofData()
                .withLabel("Stereotype", "entrypoint").neverFlows().toVertex()
                .withCharacteristic("Stereotype", "internal").create());
        return new ViolationAnalysis(true).analyze(model, List.of(anyInternalFlow)).cyclic();
    }

    private static DataFlowDiagramAndDictionary microSecEnd(String model) {
        String directory = Paths.get("scenarios", "dfd", "TUHH-Models", model).toString();
        try {
            return new DataFlowDiagramAndDictionary("org.dataflowanalysis.examplemodels",
                    Paths.get(directory, model + "_0.dataflowdiagram").toString(),
                    Paths.get(directory, model + "_0.datadictionary").toString(), Activator.class);
        } catch (Exception e) {
            throw new IllegalStateException("cannot load microSecEnD model " + model, e);
        }
    }

    private record Annotated(Path folder, String prefix, String origin) {
        Reading read() {
            var model = new UncertaintyModelLoader().load(new UncertaintyModelSpec(
                    folder.resolve(prefix + ".dataflowdiagram"),
                    folder.resolve(prefix + ".datadictionary"),
                    folder.resolve(prefix + ".uncertainty"))).baseModel();
            return new Reading(folder.getFileName() + "/" + prefix, origin, cyclic(model));
        }
    }

    private record Reading(
            @ReportColumn(value = "model", alignment = ReportColumn.Alignment.LEFT) String model,
            @ReportColumn(value = "origin", alignment = ReportColumn.Alignment.LEFT) String origin,
            @ReportColumn(value = "cyclic", alignment = ReportColumn.Alignment.LEFT) boolean cyclic) {
    }
}
