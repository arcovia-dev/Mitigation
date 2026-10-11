package dev.arcovia.mitigation.uncertainty.evaluation.support;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.ilp.OptimizationManager;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationArtifacts;
import dev.arcovia.mitigation.uncertainty.evaluation.RecordCsv;
import dev.arcovia.mitigation.uncertainty.helper.RecordTableRenderer;
import dev.arcovia.mitigation.uncertainty.helper.ReportColumn;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelLoader;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelSpec;
import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.Activator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Decides whether the confidentiality analysis reads the same way twice on a cyclic model.
 */
public class CyclicDeterminismEvaluationTest {

    private static final int REPETITIONS = 10;

    private final Path current = Path.of(System.getProperty("user.dir"));


    @Test
    @EnabledIfSystemProperty(named = "arcovia.evaluation", matches = "true",
            disabledReason = "opt-in evaluation run; set -Darcovia.evaluation=true")
    public void repeatedAnalysesOfACyclicModelAgree() throws Exception {
        EvaluationArtifacts.suppressRepeatedAnalysisWarnings();

        var cases = cases();
        var results = cases.stream().map(study -> readRepeatedly(study.name(), study.model(), study.constraint())).toList();
        var afterRepair = cases.stream().filter(Case::expectCycle).map(this::readAfterRepair).toList();

        var report = renderReport(results) + renderAfterRepair(afterRepair);
        System.out.println(report);
        Path directory = EvaluationArtifacts.createRunDirectory(current, "cyclic-determinism");
        EvaluationArtifacts.write(directory, "report.txt", report);
        EvaluationArtifacts.write(directory, "summary.csv", csv(results));
        EvaluationArtifacts.write(directory, "after-repair.csv", csv(afterRepair));

        assertTrue(results.stream().anyMatch(Readings::cyclic),
                "the experiment needs at least one cyclic case, otherwise it decides nothing");
        assertTrue(results.stream().filter(result -> !result.cyclic()).anyMatch(Readings::stable),
                "the acyclic control must be stable, otherwise instability elsewhere cannot be attributed to the cycle");
    }

    private Readings readAfterRepair(Case study) {
        try {
            var manager = new OptimizationManager(study.model(), List.of(study.constraint()), false);
            manager.setRepairCostSpecification(EvaluationArtifacts.costProfile());
            return readRepeatedly(study.name(), manager.repair(), study.constraint());
        } catch (Exception e) {
            return new Readings(study.name() + " [repair failed: " + e.getClass().getSimpleName() + "]", List.of());
        }
    }

    private static Readings readRepeatedly(String name, DataFlowDiagramAndDictionary model, Constraint constraint) {
        return new Readings(name, IntStream.range(0, REPETITIONS).mapToObj(run -> read(model, constraint)).toList());
    }

    private static Reading read(DataFlowDiagramAndDictionary model, Constraint constraint) {
        var analysis = new DFDDataFlowAnalysisBuilder().standalone()
                .useCustomResourceProvider(new DFDModelResourceProvider(model.dataDictionary(), model.dataFlowDiagram()))
                .build();
        analysis.initializeAnalysis();
        var flowGraph = analysis.findFlowGraphs();
        flowGraph.evaluate();

        var violating = constraint.determineViolations(flowGraph).stream().toList();
        var repairSites = violating.stream()
                .flatMap(node -> node.getPossibleMitigations(false).stream())
                .map(mitigation -> mitigation.mitigation().domain() + ":" + mitigation.mitigation().type())
                .collect(Collectors.toCollection(TreeSet::new));
        return new Reading(flowGraph.getTransposeFlowGraphs().size(), flowGraph.wasCyclic(), violating.size(), repairSites);
    }


    private List<Case> cases() {
        var cyclic = Stream.of("anilallewar", "apssouza22", "spring-petclinic", "sqshq")
                .map(model -> new Case("microSecEnD " + model + " (cyclic)", microSecEnd(model),
                        unauthenticatedMustNotReachInternal(), true));
        var loginFramework = current.getParent().resolve("dev.arcovia.mitigation.ranking.tests")
                .resolve("models").resolve("LoginFramework5");
        var login = new Case("LoginFramework5 (cyclic)", new UncertaintyModelLoader().load(new UncertaintyModelSpec(
                loginFramework.resolve("default.dataflowdiagram"), loginFramework.resolve("default.datadictionary"),
                loginFramework.resolve("default.uncertainty"))).baseModel(), untrustedMustNotReachExternal(), true);
        var control = new Case("microSecEnD koushikkothagal (acyclic control)", microSecEnd("koushikkothagal"),
                unauthenticatedMustNotReachInternal(), false);
        return Stream.concat(cyclic, Stream.of(login, control)).toList();
    }

    private static DataFlowDiagramAndDictionary microSecEnd(String model) {
        var directory = Paths.get("scenarios", "dfd", "TUHH-Models", model).toString();
        try {
            return new DataFlowDiagramAndDictionary("org.dataflowanalysis.examplemodels",
                    Paths.get(directory, model + "_0.dataflowdiagram").toString(),
                    Paths.get(directory, model + "_0.datadictionary").toString(), Activator.class);
        } catch (Exception e) {
            throw new IllegalStateException("cannot load microSecEnD model " + model, e);
        }
    }

    private static Constraint unauthenticatedMustNotReachInternal() {
        return new Constraint(new ConstraintDSL().ofData().withoutLabel("Stereotype", "authenticated_request")
                .neverFlows().toVertex().withCharacteristic("Stereotype", "internal").create());
    }

    private static Constraint untrustedMustNotReachExternal() {
        return new Constraint(new ConstraintDSL().ofData().withoutLabel("Stereotype", "trusted")
                .neverFlows().toVertex().withCharacteristic("Stereotype", "external").create());
    }

    private record DeterminismReportRow(
            @ReportColumn(value = "Case", alignment = ReportColumn.Alignment.LEFT) String caseStudy,
            @ReportColumn(value = "cyclic", alignment = ReportColumn.Alignment.LEFT) String cyclic,
            @ReportColumn(value = "TFG counts", alignment = ReportColumn.Alignment.LEFT) String graphCounts,
            @ReportColumn(value = "violations", alignment = ReportColumn.Alignment.LEFT) String violations,
            @ReportColumn(value = "verdict", alignment = ReportColumn.Alignment.LEFT) String verdict) {

        static DeterminismReportRow of(Readings result) {
            var violations = result.distinct(Reading::violatingNodes);
            var siteSets = result.distinct(Reading::repairSites).size();
            return new DeterminismReportRow(truncate(result.name()), result.cyclic() ? "yes" : "no",
                    result.distinct(Reading::transposeFlowGraphs).toString(),
                    violations.size() == 1 && siteSets == 1
                            ? violations.iterator().next().toString() : violations + "/" + siteSets + " distinct",
                    CyclicDeterminismEvaluationTest.verdict(result));
        }
    }

    private record AfterRepairRow(
            @ReportColumn(value = "Case", alignment = ReportColumn.Alignment.LEFT) String caseStudy,
            @ReportColumn(value = "TFG counts", alignment = ReportColumn.Alignment.LEFT) String graphCounts,
            @ReportColumn(value = "residual", alignment = ReportColumn.Alignment.LEFT) String residual,
            @ReportColumn(value = "verdict", alignment = ReportColumn.Alignment.LEFT) String verdict) {

        static AfterRepairRow of(Readings result) {
            if (result.readings().isEmpty()) {
                return new AfterRepairRow(truncate(result.name()), "-", "-", "NOT MEASURED");
            }
            var residual = result.distinct(Reading::violatingNodes);
            return new AfterRepairRow(truncate(result.name()), result.distinct(Reading::transposeFlowGraphs).toString(),
                    residual.size() == 1 ? residual.iterator().next().toString() : residual + " distinct",
                    CyclicDeterminismEvaluationTest.verdict(result));
        }
    }

    private static String renderReport(List<Readings> results) {
        var cyclic = results.stream().filter(Readings::cyclic).toList();
        var stable = cyclic.stream().filter(Readings::stable).count();
        var criterion = stable == cyclic.size()
                ? "Criterion met: repeated analyses agree, so the guard is not justified by non-determinism alone."
                : "Criterion not met: at least one cyclic model reads differently between identical runs, "
                  + "so a result measured on it cannot be trusted.";
        return """
                
                ============ Analysis determinism on cyclic models (E2) ============
                %d fresh analyses per model. A case is stable when every run
                reports the same transpose-flow-graph count and the same violating-node set.
                
                %sCyclic cases stable: %d of %d.
                %s
                """.formatted(REPETITIONS, table(results, DeterminismReportRow::of, DeterminismReportRow.class),
                stable, cyclic.size(), criterion);
    }

    private static String renderAfterRepair(List<Readings> results) {
        var measured = results.stream().filter(result -> !result.readings().isEmpty()).toList();
        return """
                
                ---- Post-repair reading, the measurement Q1 actually depends on ----
                Each cyclic model is repaired once by the uncertainty-unaware baseline, then
                re-analysed %d times.
                
                %sPost-repair readings stable: %d of %d measured.
                Determinism is not correctness. A stable reading can still be a wrong reading,
                because cycle unrolling is heuristic; this experiment bounds only reproducibility.
                """.formatted(REPETITIONS, table(results, AfterRepairRow::of, AfterRepairRow.class),
                measured.stream().filter(Readings::stable).count(), measured.size());
    }

    private static <R extends Record> String table(List<Readings> results, Function<Readings, R> row, Class<R> type) {
        return RecordTableRenderer.render(results.stream().map(row).toList(), type);
    }

    private static String verdict(Readings result) {
        return result.stable() ? "STABLE" : "UNSTABLE";
    }

    private static String truncate(String value) {
        return value.length() <= 46 ? value : value.substring(0, 45) + "…";
    }

    /**
     * One row per reading, because determinism is a claim about individual runs.
     */
    private static String csv(List<Readings> results) {
        return RecordCsv.table(CsvRow.class, results.stream()
                .flatMap(result -> IntStream.range(0, result.readings().size()).mapToObj(run -> {
                    Reading reading = result.readings().get(run);
                    return new CsvRow(result.name(), reading.cyclic(), run, reading.transposeFlowGraphs(),
                            reading.violatingNodes(), reading.repairSites().size());
                })).toList());
    }

    private record CsvRow(@JsonProperty("case") String caseName, boolean cyclic, int run, int transposeFlowGraphs,
                          int violatingNodes, int repairSites) {
    }

    private record Case(String name, DataFlowDiagramAndDictionary model, Constraint constraint, boolean expectCycle) {
    }

    private record Reading(int transposeFlowGraphs, boolean cyclic, int violatingNodes, Set<String> repairSites) {
    }

    private record Readings(String name, List<Reading> readings) {

        boolean cyclic() {
            return readings.stream().anyMatch(Reading::cyclic);
        }

        <T> Set<T> distinct(Function<Reading, T> field) {
            return readings.stream().map(field).collect(Collectors.toCollection(LinkedHashSet::new));
        }

        boolean stable() {
            return distinct(Reading::transposeFlowGraphs).size() == 1 && distinct(Reading::violatingNodes).size() == 1
                   && distinct(Reading::repairSites).size() == 1;
        }
    }
}
