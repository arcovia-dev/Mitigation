package dev.arcovia.mitigation.uncertainty.evaluation.encoding;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.arcovia.mitigation.ilp.ActionTerm;
import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.uncertainty.RobustRepairResult;
import dev.arcovia.mitigation.uncertainty.UncertaintyAwareOptimizationManager;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationArtifacts;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationConstraints;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationRecord;
import dev.arcovia.mitigation.uncertainty.evaluation.RecordCsv;
import dev.arcovia.mitigation.uncertainty.helper.RecordTableRenderer;
import dev.arcovia.mitigation.uncertainty.helper.ReportColumn;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelLoader;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelSpec;
import dev.arcovia.mitigation.uncertainty.pipeline.ScenarioPreparationService;
import dev.arcovia.mitigation.uncertainty.validation.RobustRepairValidator;
import org.apache.log4j.Logger;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import static dev.arcovia.mitigation.uncertainty.evaluation.EvaluationConstraints.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Measures whether a returned repair plan contains an action it does not need.
 */
public class IrredundancyEvaluationTest {

    private static final Logger LOGGER = Logger.getLogger(IrredundancyEvaluationTest.class);

    private final Path current = Path.of(System.getProperty("user.dir"));


    private List<Case> cases() {
        var models = current.resolve("models");
        var external = models.resolve("DFDExternalUncertaintyMitigation");
        var mitigationExample = models.resolve("mitigation_example");
        var rankingKoushikkothagal = current.getParent().resolve("dev.arcovia.mitigation.ranking.tests")
                .resolve("models").resolve("koushikkothagal");
        return List.of(
                new Case("Banking/ext (single-action control)", external, "ext",
                        EvaluationConstraints.of(PERSONAL_NEVER_LEAVES_EU),
                        source -> source.getEntityName().equals("Banking_Data_Location_Uncertain")),
                new Case("Transactions/ext + ENCRYPTION-escape", external, "ext",
                        EvaluationConstraints.of(PERSONAL_OUTSIDE_EU_MUST_BE_ENCRYPTED),
                        source -> source.getEntityName().equals("Transactions_DB_Location_Uncertain")),
                new Case("Banking+Transactions/ext + ENCRYPTION-escape", external, "ext",
                        EvaluationConstraints.of(PERSONAL_OUTSIDE_EU_MUST_BE_ENCRYPTED),
                        source -> source.getEntityName().equals("Banking_Data_Location_Uncertain")
                                  || source.getEntityName().equals("Transactions_DB_Location_Uncertain")),
                new Case("mitigation_example (Personal)", mitigationExample, "mitigation_example",
                        EvaluationConstraints.of(PERSONAL_NEVER_LEAVES_EU), null),
                new Case("microSecEnD koushikkothagal + V11 {C9,C10}", rankingKoushikkothagal, "koushikkothagal",
                        EvaluationConstraints.of(INTERNAL_MUST_LOG_LOCALLY, LOGS_MUST_BE_SANITIZED), null));
    }

    @Test
    @EnabledIfSystemProperty(named = "arcovia.evaluation", matches = "true",
            disabledReason = "opt-in evaluation run; set -Darcovia.evaluation=true")
    public void everySelectedActionIsNeededBySomeScenario() throws Exception {
        List<CaseResult> results = new ArrayList<>();
        for (Case study : cases()) {
            results.add(measure(study));
        }

        var report = report(results);
        LOGGER.info(report);
        writeArtifacts(report, results);

        assertEquals(cases().size(), results.size(), "Every case must be measured");
        assertFalse(results.stream().allMatch(result -> result.actionCount() <= 1),
                "At least one case must return a multi-action plan, otherwise the experiment "
                + "cannot distinguish an irredundant plan from a redundant one");
    }

    /**
     * Repairs one case, then withholds each selected action in turn and re-validates.
     *
     * @param study the case to measure
     * @return the per-action outcome for that case
     */
    private CaseResult measure(Case study) throws Exception {
        var repair = repair(study);
        var plan = repair.selectedActions();

        var outcomes = new ArrayList<ActionOutcome>();
        for (var withheld = 0; withheld < plan.size(); withheld++) {
            outcomes.add(withhold(study, plan, withheld));
        }
        return new CaseResult(study.name(), plan.size(), repair.objectiveValue(),
                repair.consideredScenarioCount(), List.copyOf(outcomes));
    }

    /**
     * Runs the robust repair on one case and returns its result.
     *
     * @param study the case to repair
     * @return the robust repair result, including the selected plan
     * @throws Exception if the repair cannot be run
     */
    private RobustRepairResult repair(Case study) throws Exception {
        var model = load(study);
        var manager = new UncertaintyAwareOptimizationManager(
                model, study.constraints(), false, select(model, study.sourceFilter()));
        manager.setRepairCostSpecification(EvaluationArtifacts.costProfile());
        manager.repair();
        return manager.getRobustRepairResult().orElseThrow();
    }

    /**
     * Applies the plan without one of its actions and re-analyses every declared scenario.
     *
     * @param study    the case the plan belongs to
     * @param plan     the complete returned plan
     * @param withheld the index of the action to leave out
     * @return what re-analysis found without that action
     */
    private ActionOutcome withhold(Case study, List<ActionTerm> plan, int withheld) {
        var subset = new ArrayList<>(plan);
        var removed = subset.remove(withheld);

        var fresh = load(study);
        var sources = select(fresh, study.sourceFilter());
        var patched = fresh.baseModel();
        new SubsetApplier(fresh, study.constraints(), sources).applySubset(patched, subset);

        var validation = new RobustRepairValidator(new ScenarioPreparationService())
                .validate(patched, sources, study.constraints());
        var violatedScenarios = (int) validation.scenarios().stream()
                .filter(scenario -> !scenario.violationFree())
                .count();
        return new ActionOutcome(describe(removed), validation.totalViolationCount(),
                violatedScenarios, validation.scenarioCount());
    }

    /**
     * Reaches the inherited action application so that a plan subset can be applied directly.
     */
    private static final class SubsetApplier extends UncertaintyAwareOptimizationManager {

        SubsetApplier(LoadedUncertaintyModel model, List<Constraint> constraints,
                      List<UncertaintySource> sources) {
            super(model, constraints, false, sources);
        }

        void applySubset(DataFlowDiagramAndDictionary target, List<ActionTerm> actions) {
            applyActions(target, actions);
        }
    }

    private record ActionOutcome(String action, int violations, int violatedScenarios, int scenarios) {

        /**
         * A withheld action is load-bearing when its absence lets a violation return.
         */
        boolean reintroducedAViolation() {
            return violations > 0;
        }
    }

    private record CaseResult(String name, int actionCount, double objective, int scenarios,
                              List<ActionOutcome> outcomes) {

        int loadBearingActions() {
            return (int) outcomes.stream().filter(ActionOutcome::reintroducedAViolation).count();
        }

        boolean irredundant() {
            return loadBearingActions() == actionCount;
        }
    }

    /**
     * Renders the readable report, taking its heading from the row components.
     *
     * @param results the measured cases
     * @return the complete report text
     */
    private static String report(List<CaseResult> results) {
        return """
                       Repair plan irredundancy
                       Each selected action is withheld in turn, the reduced plan is applied to a freshly
                       loaded base model, and every declared scenario is re-analysed. An action is
                       load-bearing when its absence lets at least one violation return.
                       """ + "\n"
               + RecordTableRenderer.render(rows(results), WithheldActionRow.class)
               + "\n" + summary(results);
    }

    /**
     * Counts the plans and the actions the re-analysis found load-bearing.
     *
     * @param results the measured cases
     * @return the one-line headline of the report
     */
    private static String summary(List<CaseResult> results) {
        var plans = results.stream().filter(result -> result.actionCount() > 0).toList();
        var irredundant = plans.stream().filter(CaseResult::irredundant).count();
        var actions = results.stream().mapToInt(CaseResult::actionCount).sum();
        var loadBearing = results.stream().mapToInt(CaseResult::loadBearingActions).sum();
        return String.format(Locale.ROOT,
                "%d of %d plans irredundant; %d of %d selected actions load-bearing.%n",
                irredundant, plans.size(), loadBearing, actions);
    }

    /**
     * Names what the re-analysis concluded about one plan.
     *
     * @param result the measured case
     * @return the verdict recorded for it
     */
    private static String verdict(CaseResult result) {
        if (result.actionCount() == 0) {
            return "NO_PLAN";
        }
        return result.irredundant() ? "IRREDUNDANT" : "REDUNDANT";
    }

    private void writeArtifacts(String report, List<CaseResult> results) {
        try {
            Path directory = EvaluationArtifacts.createRunDirectory(current, "irredundancy");
            EvaluationArtifacts.write(directory, "report.txt", report);
            EvaluationArtifacts.write(directory, "summary.csv",
                    RecordCsv.table(WithheldActionRow.class, rows(results)));
            EvaluationArtifacts.writeMeasurements(directory, measurements(results));
            LOGGER.info("Evaluation artefacts: " + directory);
        } catch (Exception exception) {
            LOGGER.error("Could not write evaluation artefacts", exception);
        }
    }

    /**
     * One row per withheld action, and the single schema of the CSV artifact.
     *
     * @param caseName           the case the plan was computed for
     * @param scenarios          declared scenarios re-analyzed per withheld action
     * @param actions            the size of the returned plan
     * @param objective          the objective the plan achieved
     * @param loadBearingActions actions whose absence lets a violation return
     * @param irredundant        whether every action of the plan is load-bearing
     * @param withheldAction     the action removed for this row
     * @param residualViolations violations after applying the plan without it
     * @param violatedScenarios  scenarios that still contain a violation
     */
    record WithheldActionRow(
            @JsonProperty("case") @ReportColumn(value = "case", alignment = ReportColumn.Alignment.LEFT) String caseName,
            @ReportColumn("scenarios") int scenarios,
            @ReportColumn("actions") int actions,
            @ReportColumn("objective") double objective,
            @ReportColumn("load-bearing") int loadBearingActions,
            @ReportColumn("irredundant") boolean irredundant,
            @ReportColumn(value = "withheld action", alignment = ReportColumn.Alignment.LEFT) String withheldAction,
            @ReportColumn("residual v") int residualViolations,
            @ReportColumn("violated s") int violatedScenarios) {
    }

    /**
     * Flattens every case into one row per withheld action.
     *
     * @param results the measured cases
     * @return the rows of the CSV artifact, in case order
     */
    private static List<WithheldActionRow> rows(List<CaseResult> results) {
        return results.stream()
                .flatMap(result -> result.outcomes().stream().map(outcome -> new WithheldActionRow(
                        result.name(), result.scenarios(), result.actionCount(), result.objective(),
                        result.loadBearingActions(), result.irredundant(), outcome.action(),
                        outcome.violations(), outcome.violatedScenarios())))
                .toList();
    }

    /**
     * Projects one measurement row per case, so the shared schema carries the plan size and the
     * count of actions the architecture actually needs.
     */
    private static List<EvaluationRecord> measurements(List<CaseResult> results) {
        String profile = EvaluationArtifacts.costProfile().id();
        List<EvaluationRecord> records = results.stream().map(r -> EvaluationRecord.of(r.name(), "R")
                .costProfile(profile)
                .scenarios(r.scenarios(), r.scenarios(), r.scenarios())
                .plan(r.actionCount(), r.objective(), verdict(r))
                .stopReason(r.loadBearingActions() + "/" + r.actionCount() + " load-bearing")
                .build()).toList();
        return List.copyOf(records);
    }

    private static String describe(ActionTerm action) {
        return action.toString().replace("ActionTerm", "").replace("AbstractChar", "");
    }

    private LoadedUncertaintyModel load(Case study) {
        return new UncertaintyModelLoader().load(new UncertaintyModelSpec(
                study.folder().resolve(study.prefix() + ".dataflowdiagram"),
                study.folder().resolve(study.prefix() + ".datadictionary"),
                study.folder().resolve(study.prefix() + ".uncertainty")));
    }

    private static List<UncertaintySource> select(LoadedUncertaintyModel model,
                                                  Predicate<UncertaintySource> filter) {
        return filter == null ? model.sources() : model.sources().stream().filter(filter).toList();
    }

    /**
     * @param name         case identifier, matching the correctness corpus where it overlaps
     * @param folder       directory holding the three model resources
     * @param prefix       shared file-name stem of those resources
     * @param constraints  the constraint set
     * @param sourceFilter which declared sources take part; {@code null} selects all
     */
    private record Case(String name, Path folder, String prefix, List<Constraint> constraints,
                        Predicate<UncertaintySource> sourceFilter) {
    }
}
