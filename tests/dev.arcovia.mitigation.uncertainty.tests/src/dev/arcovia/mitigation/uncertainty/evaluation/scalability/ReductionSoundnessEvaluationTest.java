package dev.arcovia.mitigation.uncertainty.evaluation.scalability;

import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.uncertainty.UncertaintyAwareOptimizationManager;
import dev.arcovia.mitigation.uncertainty.enumeration.ScenarioCombinationGenerator;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationArtifacts;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationRecord;
import dev.arcovia.mitigation.uncertainty.evaluation.fixtures.TwoSubsystemsFixture;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelLoader;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelSpec;
import dev.arcovia.mitigation.uncertainty.pipeline.ScenarioPreparationService;
import dev.arcovia.mitigation.uncertainty.validation.RobustRepairValidator;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that source reductions preserve the unreduced robust-repair result.
 */
public class ReductionSoundnessEvaluationTest {
    private final Path current = Path.of(System.getProperty("user.dir"));

    @Test
    @EnabledIfSystemProperty(named = "arcovia.evaluation", matches = "true",
            disabledReason = "opt-in evaluation run; set -Darcovia.evaluation=true")
    public void reductionsPreserveFullSpaceValidityAndObjective() throws Exception {
        // Discarded warm-up
        run(caseStudies().get(0), Configuration.FULL);

        List<ReductionRow> rows = new ArrayList<>();
        for (CaseStudy caseStudy : caseStudies()) {
            Map<Configuration, ReductionRow> results = new EnumMap<>(Configuration.class);
            for (Configuration configuration : Configuration.values()) {
                ReductionRow row = run(caseStudy, configuration);
                results.put(configuration, row);
                rows.add(row);
            }
            assertEquivalentToFull(results);
        }
        writeArtefacts(rows);
    }

    /**
     * Runs every reduction configuration for one case.
     *
     * @param caseStudy     the model and constraint to evaluate
     * @param configuration the enabled reductions
     * @return the complete run record
     * @throws Exception if the robust repair fails
     */
    private ReductionRow run(CaseStudy caseStudy, Configuration configuration) throws Exception {
        LoadedUncertaintyModel loaded = load(caseStudy);
        List<Constraint> constraints = caseStudy.constraints();
        var manager = new UncertaintyAwareOptimizationManager(loaded, constraints, false, loaded.sources());
        manager.setRepairCostSpecification(EvaluationArtifacts.costProfile());
        manager.setImpactPruningEnabled(configuration.impactPruning);
        manager.setPartitioningEnabled(configuration.partitioning);
        long startNanos = System.nanoTime();
        manager.repair();
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
        var result = manager.getRobustRepairResult().orElseThrow();
        var validation = new RobustRepairValidator(new ScenarioPreparationService())
                .validate(result.repairedModel(), loaded.sources(), constraints);
        return new ReductionRow(
                caseStudy.name,
                configuration.name(),
                loaded.sources().stream().map(UncertaintySource::getEntityName).toList(),
                new ScenarioCombinationGenerator().generate(loaded.sources()).size(),
                result.consideredScenarioCount(),
                validation.scenarioCount(),
                validation.totalViolationCount(),
                result.objectiveValue(),
                result.solverStatus(),
                result.validationStatus().name(),
                validation.passed(),
                elapsedMillis);
    }

    /**
     * Verifies that every reduced run matches the unreduced reference.
     *
     * @param results rows keyed by reduction configuration
     */
    private static void assertEquivalentToFull(Map<Configuration, ReductionRow> results) {
        ReductionRow full = results.get(Configuration.FULL);
        assertTrue(full.fullValidationPassed, "The unreduced reference must pass full-space validation");
        for (ReductionRow actual : results.values()) {
            assertEquals(full.solverStatus, actual.solverStatus,
                    "Solver feasibility must match FULL for " + actual.configuration);
            assertEquals(full.objectiveValue, actual.objectiveValue, 1e-9,
                    "Minimum objective must match FULL for " + actual.configuration);
            assertTrue(actual.fullValidationPassed,
                    "Full-space validation must pass for " + actual.configuration);
            assertEquals(actual.fullScenarioCount, actual.validatedScenarioCount,
                    "The independent validation must cover S_full for " + actual.configuration);
        }
    }

    /**
     * Writes the human-readable and machine-readable evaluation artefacts.
     *
     * @param rows the completed configuration runs
     */
    private void writeArtefacts(List<ReductionRow> rows) {
        try {
            Path directory = EvaluationArtifacts.createRunDirectory(current, "reduction-soundness");
            EvaluationArtifacts.write(directory, "report.txt", report(rows));
            EvaluationArtifacts.write(directory, "summary.csv", csv(rows));
            EvaluationArtifacts.writeMeasurements(directory, measurements(rows));
            EvaluationArtifacts.writeJsonLines(directory, "raw-records.jsonl", rows.stream()
                    .map(this::json)
                    .toList());
            System.out.println("Evaluation artefacts: " + directory);
        } catch (Exception exception) {
            throw new AssertionError("Could not write evaluation artefacts", exception);
        }
    }

    /**
     * Renders a compact result table.
     *
     * @param rows the completed configuration runs
     * @return the report text
     */
    private static String report(List<ReductionRow> rows) {
        StringBuilder report = new StringBuilder();
        report.append("Reduction soundness: every row is independently validated over S_full.\n");
        report.append(String.format("%-24s | %-14s | %6s | %12s | %12s | %8s | %10s%n",
                "Case", "Configuration", "S_full", "S_considered", "S_validated", "JΘ", "Outcome"));
        report.append("-".repeat(108)).append("\n");
        for (ReductionRow row : rows) {
            report.append(String.format("%-24s | %-14s | %6d | %12d | %12d | %8.2f | %10s%n",
                    row.caseName, row.configuration, row.fullScenarioCount, row.consideredScenarioCount,
                    row.validatedScenarioCount, row.objectiveValue,
                    row.fullValidationPassed ? "VALID" : "FAILED"));
        }
        return report.toString();
    }

    /**
     * Renders the summary as CSV.
     *
     * @param rows the completed configuration runs
     * @return the CSV text
     */
    private static String csv(List<ReductionRow> rows) {
        StringBuilder csv = new StringBuilder(
                "case,configuration,selected_sources,s_full,s_considered,s_validated,"
                + "violations,objective,solver_status,pipeline_validation,full_validation\n");
        for (ReductionRow row : rows) {
            csv.append(String.join(",",
                    quote(row.caseName), quote(row.configuration), quote(String.join("|", row.selectedSources)),
                    Integer.toString(row.fullScenarioCount), Integer.toString(row.consideredScenarioCount),
                    Integer.toString(row.validatedScenarioCount), Integer.toString(row.fullViolationCount),
                    Double.toString(row.objectiveValue), quote(row.solverStatus), quote(row.pipelineValidation),
                    Boolean.toString(row.fullValidationPassed))).append("\n");
        }
        return csv.toString();
    }

    /**
     * Renders one raw record as JSON Lines.
     *
     * @param row the completed configuration run
     * @return one JSON object
     */
    private String json(ReductionRow row) {
        return EvaluationArtifacts.toJson(new ReductionRecord(
                row.caseName,
                row.configuration,
                row.selectedSources,
                row.fullScenarioCount,
                row.consideredScenarioCount,
                row.validatedScenarioCount,
                row.fullViolationCount,
                row.objectiveValue,
                row.solverStatus,
                row.pipelineValidation,
                row.fullValidationPassed));
    }

    /**
     * One reduction configuration measured on one case.
     *
     * @param caseId               case identifier
     * @param configuration        which reductions were enabled
     * @param selectedSourceIds    the selected source identifiers
     * @param sFull                the declared scenario product
     * @param sConsidered          scenarios the solver considered
     * @param sValidated           scenarios re-analysed after the repair
     * @param fullViolationCount   residual violations over the full space
     * @param objective            the solver objective
     * @param solverStatus         the solver status
     * @param pipelineValidation   the pipeline validation status
     * @param fullValidationPassed whether the full-space validation passed
     */
    private record ReductionRecord(
            String caseId,
            String configuration,
            List<String> selectedSourceIds,
            int sFull,
            int sConsidered,
            int sValidated,
            int fullViolationCount,
            double objective,
            String solverStatus,
            String pipelineValidation,
            boolean fullValidationPassed) {
    }

    /**
     * Quotes one CSV value.
     *
     * @param value the source value
     * @return the quoted CSV value
     */
    private static String quote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /**
     * Loads the partitioning fixture.
     *
     * @return the loaded model
     */
    private LoadedUncertaintyModel load(CaseStudy caseStudy) {
        return caseStudy.model().get();
    }

    /**
     * Loads one of the published corpus models from the test bundle's model directory.
     *
     * @param folder the model directory, whose files share its name as their prefix
     * @return the loaded model with its declared uncertainty sources
     */
    private static LoadedUncertaintyModel loadFromDisk(String folder) {
        Path directory = Path.of(System.getProperty("user.dir")).resolve("models").resolve(folder);
        return new UncertaintyModelLoader().load(new UncertaintyModelSpec(
                directory.resolve(folder + ".dataflowdiagram"),
                directory.resolve(folder + ".datadictionary"),
                directory.resolve(folder + ".uncertainty")));
    }

    /**
     * Creates the coupled and coupling-safe cases.
     *
     * @return the predeclared reduction cases
     */
    private static List<CaseStudy> caseStudies() {
        return List.of(
                new CaseStudy("personal-coupled", () -> loadFromDisk("mitigation_example"),
                        List.of(new Constraint(new ConstraintDSL().ofData()
                                .withLabel("Sensitivity", "Personal").neverFlows().toVertex()
                                .withCharacteristic("Location", "nonEU").create()))),
                new CaseStudy("public-safe", () -> loadFromDisk("mitigation_example"),
                        List.of(new Constraint(new ConstraintDSL().ofData()
                                .withLabel("Sensitivity", "Public").neverFlows().toVertex()
                                .withCharacteristic("Location", "nonEU").create()))),
                new CaseStudy("two-subsystems-primary", TwoSubsystemsFixture::build,
                        List.of(new Constraint(new ConstraintDSL().ofData()
                                .withLabel("Sensitivity", "Personal")
                                .withoutLabel("Encryption", "Encrypted")
                                .neverFlows().toVertex()
                                .withCharacteristic("Location", "nonEU").create()))));
    }

    private enum Configuration {
        FULL(false, false),
        IMPACT_ONLY(true, false),
        PARTITION_ONLY(false, true),
        BOTH(true, true);

        private final boolean impactPruning;
        private final boolean partitioning;

        Configuration(boolean impactPruning, boolean partitioning) {
            this.impactPruning = impactPruning;
            this.partitioning = partitioning;
        }
    }

    private record CaseStudy(String name, Supplier<LoadedUncertaintyModel> model,
                             List<Constraint> constraints) {
    }

    /**
     * Projects each reduction configuration onto a measurement row, so the reduction comparison
     * can be plotted as a paired scatter rather than read from a table.
     */
    private static List<EvaluationRecord> measurements(List<ReductionRow> rows) {
        String profile = EvaluationArtifacts.costProfile().id();
        return rows.stream()
                .map(row -> EvaluationRecord.of(row.caseName(), "R")
                        .constraintSet(row.configuration())
                        .costProfile(profile)
                        .sources(row.selectedSources().size(), EvaluationRecord.UNSET_COUNT)
                        .scenarios(row.fullScenarioCount(), row.consideredScenarioCount(),
                                row.validatedScenarioCount())
                        .violations(row.fullViolationCount(), EvaluationRecord.UNSET_COUNT,
                                EvaluationRecord.UNSET_COUNT)
                        .plan(EvaluationRecord.UNSET_COUNT, row.objectiveValue(), row.solverStatus())
                        .timings(EvaluationRecord.UNSET_VALUE, EvaluationRecord.UNSET_VALUE,
                                EvaluationRecord.UNSET_VALUE, EvaluationRecord.UNSET_VALUE,
                                EvaluationRecord.UNSET_VALUE, row.repairMillis())
                        .stopReason(row.pipelineValidation())
                        .build())
                .toList();
    }

    private record ReductionRow(
            String caseName,
            String configuration,
            List<String> selectedSources,
            int fullScenarioCount,
            int consideredScenarioCount,
            int validatedScenarioCount,
            int fullViolationCount,
            double objectiveValue,
            String solverStatus,
            String pipelineValidation,
            boolean fullValidationPassed,
            long repairMillis) {
    }
}
