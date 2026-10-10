package dev.arcovia.mitigation.uncertainty.evaluation.support;

import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.ilp.OptimizationManager;
import dev.arcovia.mitigation.uncertainty.UncertaintyAwareOptimizationManager;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationArtifacts;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationConstraints;
import dev.arcovia.mitigation.uncertainty.evaluation.RecordCsv;
import dev.arcovia.mitigation.uncertainty.helper.RecordTableRenderer;
import dev.arcovia.mitigation.uncertainty.helper.ReportColumn;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import dev.arcovia.mitigation.uncertainty.pipeline.ScenarioPreparationService;
import dev.arcovia.mitigation.uncertainty.preparation.RepairPreparationResult;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.Activator;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dev.arcovia.mitigation.uncertainty.evaluation.EvaluationConstraints.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MicroSecEndReferenceEvaluationTest {

    /**
     * The predecessor's rule profile per variant.
     */
    private static final Map<Integer, List<AnalysisConstraint>> PROFILES = Map.of(
            1, List.of(ENTRYPOINT_ONLY_VIA_GATEWAY, GATEWAY_IS_NEVER_INTERNAL),
            2, List.of(UNAUTHENTICATED_NEVER_REACHES_INTERNAL),
            4, List.of(ENTRY_MUST_TRANSFORM_IDENTITY),
            5, List.of(ENTRY_MUST_VALIDATE_TOKEN),
            7, List.of(ENTRYPOINT_ONLY_VIA_GATEWAY, GATEWAY_IS_NEVER_INTERNAL, ENTRY_MUST_BE_ENCRYPTED),
            8, List.of(INTERNAL_MUST_BE_ENCRYPTED),
            10, List.of(INTERNAL_MUST_LOG_LOCALLY),
            11, List.of(INTERNAL_MUST_LOG_LOCALLY, LOGS_MUST_BE_SANITIZED));


    private final Path current = Path.of(System.getProperty("user.dir"));

    @Test
    @EnabledIfSystemProperty(named = "arcovia.evaluation", matches = "true",
            disabledReason = "opt-in evaluation run; set -Darcovia.evaluation=true")
    void evaluatesEveryEligibleMicroSecEndReferencePair() throws Exception {
        var pairs = catalogue().stream()
                .filter(variant -> variant.classification() == Support.SUPPORTED_DSL_PROFILE).toList();
        assertEquals(51, pairs.size(), "the predecessor corpus contains 51 eligible pairs");

        var rows = pairs.stream().map(this::evaluate).toList();

        var directory = EvaluationArtifacts.createRunDirectory(current, "microsecend-reference");
        EvaluationArtifacts.write(directory, "report.txt", report(rows));
        EvaluationArtifacts.write(directory, "summary.csv", RecordCsv.table(Row.class, rows));
        EvaluationArtifacts.write(directory, "corpus-support.csv", RecordCsv.table(Variant.class, catalogue()));
    }

    private Row evaluate(Variant pair) {
        var constraints = constraints(pair);
        var base = analyse(load(pair.model(), 0), constraints);
        var reference = analyse(load(pair.model(), pair.variant()), constraints);
        boolean cyclic = base.analysedModelWasCyclic() || reference.analysedModelWasCyclic();
        var baseline = repairBaseline(pair, constraints);
        var robust = repairRobust(pair, constraints);
        return new Row(pair.model(), pair.variant(), pair.profile(), cyclic ? "ANALYSED_CYCLIC" : "ANALYSED",
                base.violatingNodes().size(), reference.violatingNodes().size(),
                baseline.outcome(), baseline.violations(), baseline.objective(),
                robust.outcome(), robust.violations(), robust.objective());
    }

    private Arm repairBaseline(Variant pair, List<Constraint> constraints) {
        try {
            var manager = new OptimizationManager(load(pair.model(), 0), constraints, false);
            var repaired = manager.repair();
            return new Arm("VALID_REPAIR", analyse(repaired, constraints).violatingNodes().size(), manager.getObjectiveValue());
        } catch (Exception e) {
            return Arm.failed(e);
        }
    }

    private Arm repairRobust(Variant pair, List<Constraint> constraints) {
        try {
            var manager = new UncertaintyAwareOptimizationManager(
                    new LoadedUncertaintyModel(load(pair.model(), 0), List.of()), constraints, false, List.of());
            manager.setCyclicModelsAllowed(true);
            manager.repair();
            var result = manager.getRobustRepairResult().orElseThrow();
            return new Arm("VALID_REPAIR", analyse(result.repairedModel(), constraints).violatingNodes().size(),
                    result.objectiveValue());
        } catch (Exception e) {
            return Arm.failed(e);
        }
    }

    private static RepairPreparationResult analyse(DataFlowDiagramAndDictionary model, List<Constraint> constraints) {
        return new ScenarioPreparationService(true).prepare(model, List.of(), constraints).get(0).preparation();
    }

    private static List<Constraint> constraints(Variant pair) {
        return EvaluationConstraints.of(PROFILES.get(pair.variant()).toArray(AnalysisConstraint[]::new));
    }

    private static DataFlowDiagramAndDictionary load(String model, int variant) {
        String name = model + "_" + variant;
        String directory = Paths.get("scenarios", "dfd", "TUHH-Models", model).toString();
        try {
            return new DataFlowDiagramAndDictionary("org.dataflowanalysis.examplemodels",
                    Paths.get(directory, name + ".dataflowdiagram").toString(),
                    Paths.get(directory, name + ".datadictionary").toString(), Activator.class);
        } catch (Exception e) {
            throw new IllegalStateException("cannot load microSecEnD model " + name, e);
        }
    }

    private static List<Variant> catalogue() {
        var models = TuhhModels.getTuhhModels();
        return models.keySet().stream().sorted()
                .flatMap(model -> models.get(model).stream().sorted()
                        .map(variant -> new Variant(model, variant, support(models.get(model), variant))))
                .toList();
    }

    private static Support support(List<Integer> variantsOfModel, int variant) {
        if (!variantsOfModel.contains(0)) {
            return Support.NO_BASE_MODEL;
        }
        if (variant == 0) {
            return Support.BASE_MODEL;
        }
        if (PROFILES.containsKey(variant)) {
            return Support.SUPPORTED_DSL_PROFILE;
        }
        return variant == 6 || variant == 12
                ? Support.CUSTOM_EVALUATION_FUNCTION_UNSUPPORTED : Support.NO_PREDECESSOR_CONSTRAINT_PROFILE;
    }

    private static Map<Support, Long> supportCounts() {
        return catalogue().stream().collect(Collectors.groupingBy(Variant::classification, Collectors.counting()));
    }

    private static long pairedVariants(Map<Support, Long> counts) {
        return catalogue().size() - counts.get(Support.BASE_MODEL) - counts.get(Support.NO_BASE_MODEL);
    }

    private static String report(List<Row> rows) {
        var counts = supportCounts();
        var corpus = Arrays.stream(Support.values())
                .map(support -> support + ": " + counts.getOrDefault(support, 0L))
                .collect(Collectors.joining("\n", "microSecEnD published-variant evaluation\n"
                                                  + "Published catalogue variants: " + catalogue().size() + "\n"
                                                  + "Paired non-base variants: " + pairedVariants(counts) + "\n", "\n"));
        var table = List.of(
                CleanPairs.of("acyclic", rows.stream().filter(row -> !row.cyclic()).toList()),
                CleanPairs.of("cyclic (unrolled)", rows.stream().filter(Row::cyclic).toList()));
        return corpus + "\n" + RecordTableRenderer.render(table, CleanPairs.class)
               + "Clean = no residual violation. S_full = 1, because the published pairs carry no uncertainty annotation.\n";
    }

    private record Variant(String model, int variant, Support classification) {
        String profile() {
            return PROFILES.get(variant).stream().map(EvaluationConstraints::idOf).collect(Collectors.joining("-"));
        }
    }

    private record Arm(String outcome, int violations, double objective) {
        static Arm failed(Exception e) {
            return new Arm("EXECUTION_ERROR:" + e.getClass().getSimpleName(), -1, -1);
        }
    }

    private record Row(String model, int variant, String constraintProfile, String applicability,
                       int b0Violations, int referenceViolations,
                       String baselineOutcome, int baselineViolations, double baselineObjective,
                       String robustOutcome, int robustViolations, double robustObjective) {
        boolean cyclic() {
            return applicability.equals("ANALYSED_CYCLIC");
        }
    }

    private record CleanPairs(
            @ReportColumn(value = "data flow", alignment = ReportColumn.Alignment.LEFT) String dataFlow,
            @ReportColumn("pairs") long pairs,
            @ReportColumn("published variant clean") long referenceClean,
            @ReportColumn("baseline clean") long baselineClean,
            @ReportColumn("robust clean") long robustClean) {
        static CleanPairs of(String dataFlow, List<Row> rows) {
            return new CleanPairs(dataFlow, rows.size(),
                    rows.stream().filter(row -> row.referenceViolations() == 0).count(),
                    rows.stream().filter(row -> row.baselineViolations() == 0).count(),
                    rows.stream().filter(row -> row.robustViolations() == 0).count());
        }
    }

    private enum Support {
        BASE_MODEL, SUPPORTED_DSL_PROFILE, CUSTOM_EVALUATION_FUNCTION_UNSUPPORTED,
        NO_PREDECESSOR_CONSTRAINT_PROFILE, NO_BASE_MODEL
    }
}
