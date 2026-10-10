package dev.arcovia.mitigation.uncertainty.evaluation.correctness;

import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.arcovia.mitigation.ilp.ActionTerm;
import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.ilp.OptimizationManager;
import dev.arcovia.mitigation.uncertainty.UncertaintyAwareOptimizationManager;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationArtifacts;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationRecord;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import dev.arcovia.mitigation.uncertainty.pipeline.ScenarioPreparation;
import dev.arcovia.mitigation.uncertainty.pipeline.ScenarioPreparationService;
import dev.arcovia.mitigation.uncertainty.solving.NoRobustRepairExistsException;
import dev.arcovia.mitigation.uncertainty.validation.RobustRepairValidationException;
import dev.arcovia.mitigation.uncertainty.validation.RobustRepairValidator;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Writes a robust-versus-baseline repair comparison report.
 */
public class RobustVsBaselineEvaluationTest {

    private final Path current = Path.of(System.getProperty("user.dir"));
    private final List<CaseStudy> caseStudies = RobustVsBaselineCaseStudies.create(current);
    private final Map<String, CaseExpectation> expectations = RobustVsBaselineCaseStudies.expectations(caseStudies);

    /**
     * Runs every case, verifies the frozen manifest, and writes the evaluation artifacts.
     *
     */
    @Test
    @EnabledIfSystemProperty(
            named = "arcovia.evaluation",
            matches = "true",
            disabledReason = "opt-in evaluation run; set -Darcovia.evaluation=true"
    )
    public void robustRepairCoversMoreScenariosThanBaselineAtBoundedCost() {

        EvaluationArtifacts.suppressRepeatedAnalysisWarnings();
        // discarded warm-up
        runCaseStudy(caseStudies.get(0));

        var rows = caseStudies.stream().map(this::runCaseStudy).collect(Collectors.toList());

        RobustVsBaselineReport.write(current, rows, expectations, measurements(rows, expectations));

        assertEquals(caseStudies.size(), rows.size(), "Every case study must be measured");
        rows.forEach(row -> {
            // Detects a change in what the repair produces for a case whose declaration did not change.
            var expected = expectations.get(row.context().name());
            assertEquals(expected.fullScenarioCount(), row.base().scenarioCount(),
                    "S_full must match the frozen case manifest for " + row.context().name());
            assertEquals(expected.verdict(), row.verdict(),
                    "The predeclared outcome must hold for " + row.context().name());
        });
    }

    /**
     * Executes a base model, nominal, and robust repair for one case and captures the outcomes.
     *
     * @param study the case definition to execute
     * @return the complete comparison measurement
     */
    private ComparisonRow runCaseStudy(CaseStudy study) {
        var loadedModel = study.model().get();
        var sources = selectSources(loadedModel, study.sourceFilter());
        var constraints = study.constraints().get();
        var pre = new ScenarioPreparationService(true).prepare(loadedModel.baseModel(), sources, constraints);
        int scenarioCount = pre.size();
        int preViolations = totalViolations(pre);
        long preViolatedScenarios = violatedScenarios(pre);
        String candidates = candidateActions(pre);
        final ComparisonRow.BaseResult baseResult = new ComparisonRow.BaseResult(scenarioCount, preViolations, preViolatedScenarios, candidates);

        boolean cyclic = pre.stream().anyMatch(p -> p.preparation().analysedModelWasCyclic());

        final ComparisonRow.NominalResult nominal = runBaselineEvaluation(study, scenarioCount);
        final ComparisonRow.RobustResult robustResult = runRobustEvaluation(study, scenarioCount);
        final ComparisonRow.CaseContext context = new ComparisonRow.CaseContext(study.name(), cyclic, sourceNames(sources));
        return new ComparisonRow(context, baseResult, nominal, robustResult);
    }

    /**
     * Runs the robust repair for one case and captures whichever outcome it produced.
     *
     * @param study         the case definition
     * @param scenarioCount the declared scenario-product size
     * @return the robust measurement
     */
    private ComparisonRow.RobustResult runRobustEvaluation(CaseStudy study, int scenarioCount) {
        var constraints = study.constraints().get();
        var loadedModel = study.model().get();
        var filteredSources = selectSources(loadedModel, study.sourceFilter());
        var optimizationManager = new UncertaintyAwareOptimizationManager(loadedModel, constraints, false, filteredSources);
        optimizationManager.setRepairCostSpecification(EvaluationArtifacts.costProfile());
        optimizationManager.setCyclicModelsAllowed(true);

        long startTime = System.nanoTime();
        try {
            optimizationManager.repair();
            var result = optimizationManager.getRobustRepairResult().orElseThrow();
            var validation = fullSpaceMetrics(result.repairedModel(), filteredSources, constraints);
            RepairOutcome outcome = validationOutcome(validation, scenarioCount);
            long millis = elapsedMillis(startTime);
            int nominalViolation = nominalViolations(result.repairedModel(), constraints);
            return new ComparisonRow.RobustResult(
                    result.objectiveValue(), nominalViolation, validation, result.consideredScenarioCount(),
                    result.costBreakdown(), result.selectedActions().size(), result.solverStatus(),
                    result.validationStatus().name(), outcome, null, millis,
                    actionsToString(result.selectedActions()));
        } catch (NoRobustRepairExistsException e) {
            long millis = elapsedMillis(startTime);
            return new ComparisonRow.RobustResult(-1, -1, FullSpaceMetrics.notApplicable(), 0,
                    null, -1, "NO_ROBUST_REPAIR", "NOT_APPLICABLE", RepairOutcome.NO_ROBUST_REPAIR, null, millis, "(none — solver reported infeasible)");
        } catch (RobustRepairValidationException e) {
            var result = e.repairResult();
            var validation = fullSpaceMetrics(result.repairedModel(), filteredSources, constraints);
            long millis = elapsedMillis(startTime);
            int nominalViolation = nominalViolations(result.repairedModel(), constraints);
            return new ComparisonRow.RobustResult(
                    result.objectiveValue(), nominalViolation, validation, result.consideredScenarioCount(),
                    result.costBreakdown(), result.selectedActions().size(), result.solverStatus(),
                    result.validationStatus().name(), RepairOutcome.VALIDATION_FAILED, null, millis,
                    actionsToString(result.selectedActions()));
        } catch (Exception e) {
            long millis = elapsedMillis(startTime);
            return new ComparisonRow.RobustResult(-1, -1, FullSpaceMetrics.notApplicable(), 0,
                    null, -1, "EXECUTION_ERROR:" + e.getClass().getSimpleName(), "NOT_APPLICABLE", RepairOutcome.EXECUTION_ERROR, e.getClass().getSimpleName(), millis, "(none — execution failed)");
        }
    }

    /**
     * Runs the nominal repair for one case, which repairs the base model only.
     *
     * @param study         the case definition
     * @param scenarioCount the declared scenario-product size
     * @return the nominal measurement
     */
    private ComparisonRow.NominalResult runBaselineEvaluation(CaseStudy study, int scenarioCount) {
        var constraints = study.constraints().get();
        var loadedModel = study.model().get();
        var filteredSources = selectSources(loadedModel, study.sourceFilter());
        var optimizationManager = new OptimizationManager(loadedModel.baseModel(), constraints, false);
        optimizationManager.setRepairCostSpecification(EvaluationArtifacts.costProfile());

        long startTime = System.nanoTime();
        try {
            optimizationManager.repair();
            var costBreakdown = optimizationManager.getObjectiveCostBreakdown().orElseThrow();
            var validation = fullSpaceMetrics(loadedModel.baseModel(), filteredSources, constraints);
            return new ComparisonRow.NominalResult(
                    optimizationManager.getObjectiveValue(),
                    nominalViolations(loadedModel.baseModel(), constraints),
                    validation, costBreakdown, costBreakdown.selectedActionCosts().size(),
                    baselineOutcome(validation, scenarioCount), null, elapsedMillis(startTime));
        } catch (Exception e) {
            return new ComparisonRow.NominalResult(-1, -1, FullSpaceMetrics.notApplicable(), null, -1,
                    RepairOutcome.EXECUTION_ERROR, e.getClass().getSimpleName(), elapsedMillis(startTime));
        }
    }

    /**
     * Returns the milliseconds elapsed since a nanosecond start mark.
     *
     * @param startNanos the start mark
     * @return the elapsed time in milliseconds
     */
    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /**
     * Formats selected repair actions for the mechanism diagnostics.
     *
     * @param actions selected repair actions
     * @return a semicolon-separated action description, or {@code (none)}
     */
    private static String actionsToString(List<ActionTerm> actions) {
        if (actions.isEmpty()) {
            return "(none)";
        }
        return actions.stream().map(Object::toString).collect(Collectors.joining("; "));
    }

    /**
     * Formats the distinct candidate actions exposed during scenario preparation.
     *
     * @param preps prepared uncertainty scenarios
     * @return a sorted candidate-action description, or {@code (none)}
     */
    private static String candidateActions(List<ScenarioPreparation> preps) {
        return preps.stream()
                .flatMap(p -> p.preparation().allMitigations().stream())
                .map(m -> m.mitigation() + " [legacy candidate score " + m.cost() + "]")
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(TreeSet::new),
                        distinct -> distinct.isEmpty() ? "(none)" : String.join(" | ", distinct)
                ));
    }


    /**
     * Projects each comparison onto the three shared measurement rows, one per compared arm.
     *
     * @param rows         completed comparison rows
     * @param expectations the frozen case expectations
     * @return immutable base, nominal, and robust measurement records
     */
    private static List<EvaluationRecord> measurements(List<ComparisonRow> rows, Map<String, CaseExpectation> expectations) {
        String profile = EvaluationArtifacts.costProfile().id();
        return rows.stream().flatMap(r -> {
            long fullSpace = expectations.get(r.context().name()).fullScenarioCount();
            return Stream.of(
                    EvaluationRecord.of(r.context().name(), "B0")
                            .costProfile(profile)
                            .cyclic(r.context().cyclic())
                            .sources(r.context().selectedSourceIds().size(), EvaluationRecord.UNSET_COUNT)
                            .scenarios(fullSpace, r.base().scenarioCount(), r.base().scenarioCount())
                            .violations(r.base().violations(), r.base().violations(), r.base().violations())
                            .scenariosWithViolation(r.base().violatedScenarios())
                            .build(),
                    EvaluationRecord.of(r.context().name(), "B1")
                            .costProfile(profile)
                            .cyclic(r.context().cyclic())
                            .sources(r.context().selectedSourceIds().size(), EvaluationRecord.UNSET_COUNT)
                            .scenarios(fullSpace, 1, r.nominal().validation().scenarioIds().size())
                            .violations(r.base().violations(), r.nominal().validation().totalViolations(),
                                    r.nominal().nominalViolations())
                            .scenariosWithViolation(r.nominal().validation().violatedScenarios())
                            .plan(r.nominal().actionCount(), r.nominal().objective(), r.nominal().outcome().name())
                            .timings(EvaluationRecord.UNSET_VALUE, EvaluationRecord.UNSET_VALUE,
                                    EvaluationRecord.UNSET_VALUE, EvaluationRecord.UNSET_VALUE,
                                    EvaluationRecord.UNSET_VALUE, r.nominal().millis())
                            .build(),
                    EvaluationRecord.of(r.context().name(), "R")
                            .costProfile(profile)
                            .cyclic(r.context().cyclic())
                            .sources(r.context().selectedSourceIds().size(), EvaluationRecord.UNSET_COUNT)
                            .scenarios(fullSpace, r.robust().consideredScenarioCount(),
                                    r.robust().validation().scenarioIds().size())
                            .violations(r.base().violations(), r.robust().validation().totalViolations(),
                                    r.robust().nominalViolations())
                            .scenariosWithViolation(r.robust().validation().violatedScenarios())
                            .plan(r.robust().actionCount(), r.robust().objective(), r.robust().solverStatus())
                            .timings(EvaluationRecord.UNSET_VALUE, EvaluationRecord.UNSET_VALUE,
                                    EvaluationRecord.UNSET_VALUE, EvaluationRecord.UNSET_VALUE,
                                    EvaluationRecord.UNSET_VALUE, r.robust().millis())
                            .stopReason(r.robust().outcome().name())
                            .build()
            );
        }).toList();
    }

    /**
     * Sums violations over prepared uncertainty scenarios.
     *
     * @param preps prepared uncertainty scenarios
     * @return the accumulated violation count
     */
    private static int totalViolations(List<ScenarioPreparation> preps) {
        return preps.stream().mapToInt(p -> p.preparation().violatingNodes().size()).sum();
    }

    /**
     * Counts prepared uncertainty scenarios that contain a violation.
     *
     * @param preps prepared uncertainty scenarios
     * @return the number of violating scenarios
     */
    private static long violatedScenarios(List<ScenarioPreparation> preps) {
        return preps.stream().filter(p -> !p.preparation().violatingNodes().isEmpty()).count();
    }

    /**
     * Validates a model against the nominal predicate only.
     *
     * @param model       the repaired model to check
     * @param constraints the constraints to evaluate
     * @return the residual violation count in the unmaterialized model
     */
    private static int nominalViolations(DataFlowDiagramAndDictionary model, List<Constraint> constraints) {
        return fullSpaceMetrics(model, List.of(), constraints).totalViolations();
    }

    /**
     * Independently validates a model across the complete selected scenario product.
     *
     * @param model       the model to validate
     * @param sources     the explicitly selected uncertainty sources
     * @param constraints the constraints to check
     * @return the full-space violation metrics
     */
    private static FullSpaceMetrics fullSpaceMetrics(
            DataFlowDiagramAndDictionary model,
            List<UncertaintySource> sources,
            List<Constraint> constraints
    ) {
        var result = new RobustRepairValidator(new ScenarioPreparationService(true))
                .validate(model, sources, constraints);
        long violated = result.scenarios().stream()
                .filter(scenario -> scenario.violationCount() > 0)
                .count();
        return new FullSpaceMetrics(result.totalViolationCount(), violated, result.scenarioIds(), result.passed());
    }

    /**
     * Returns the stable identifiers of selected sources.
     *
     * @param sources the selected sources
     * @return their ordered identifiers
     */
    private static List<String> sourceNames(List<UncertaintySource> sources) {
        return sources.stream().map(UncertaintySource::getEntityName).toList();
    }

    /**
     * Classifies an independent validation result against the complete scenario product.
     *
     * @param validation        the validation metrics
     * @param fullScenarioCount the expected selected-source product size
     * @return the recorded validation outcome
     */
    private static RepairOutcome validationOutcome(FullSpaceMetrics validation, int fullScenarioCount) {
        if (validation.scenarioIds().size() != fullScenarioCount) {
            return RepairOutcome.VALIDATION_INCOMPLETE;
        }
        return validation.passed() ? RepairOutcome.VALID_REPAIR : RepairOutcome.VALIDATION_FAILED;
    }

    /**
     * Classifies a base-only repair without treating residual violations as execution failure.
     *
     * @param validation        the validation metrics
     * @param fullScenarioCount the expected selected-source product size
     * @return the recorded baseline outcome
     */
    private static RepairOutcome baselineOutcome(FullSpaceMetrics validation, int fullScenarioCount) {
        if (validation.scenarioIds().size() != fullScenarioCount) {
            return RepairOutcome.VALIDATION_INCOMPLETE;
        }
        return validation.passed() ? RepairOutcome.VALID_REPAIR : RepairOutcome.RESIDUAL_VIOLATIONS;
    }

    /**
     * Selects the sources to include in one case's scenario product.
     *
     * @param model  loaded model containing all declared sources
     * @param filter selection predicate, or {@code null} for all sources
     * @return the selected sources in model order
     */
    private List<UncertaintySource> selectSources(LoadedUncertaintyModel model,
                                                  java.util.function.Predicate<UncertaintySource> filter) {
        if (filter == null) {
            return model.sources();
        }
        return model.sources().stream().filter(filter).toList();
    }
}
