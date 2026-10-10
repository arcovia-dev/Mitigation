package dev.arcovia.mitigation.uncertainty.evaluation.correctness;

import dev.arcovia.mitigation.cost.ActionType;
import dev.arcovia.mitigation.cost.ObjectiveCostBreakdown;
import dev.arcovia.mitigation.cost.RepairCostSpecification;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationArtifacts;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationRecord;
import dev.arcovia.mitigation.uncertainty.helper.RecordTableRenderer;
import dev.arcovia.mitigation.uncertainty.helper.ReportColumn;
import org.apache.log4j.Logger;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * Renders and writes artifacts for the robust-versus-baseline evaluation.
 */
final class RobustVsBaselineReport {

    private static final Logger LOGGER = Logger.getLogger(RobustVsBaselineReport.class);

    /**
     * Prevents instantiation of this static utility class.
     */
    private RobustVsBaselineReport() {
    }

    /**
     * Renders, logs, and writes all evaluation artifacts for one completed run.
     *
     * @param workingDirectory project directory used to create the run directory
     * @param rows             completed comparison rows
     * @param expectations     frozen case expectations
     * @param measurements     normalized arm-level measurements
     */
    static void write(
            Path workingDirectory,
            List<ComparisonRow> rows,
            Map<String, CaseExpectation> expectations,
            List<EvaluationRecord> measurements) {
        String report = renderReport(rows) + renderPriceOfRobustness(rows);
        LOGGER.info(report);
        try {
            Path directory = EvaluationArtifacts.createRunDirectory(workingDirectory, "robust-vs-baseline");
            EvaluationArtifacts.write(directory, "report.txt", report);
            EvaluationArtifacts.writeMeasurements(directory, measurements);
            EvaluationArtifacts.writeJsonLines(directory, "case-manifest.jsonl", expectations.entrySet().stream()
                    .map(entry -> EvaluationArtifacts.toJson(manifestExport(entry.getKey(), entry.getValue())))
                    .toList());
            EvaluationArtifacts.writeJsonLines(directory, "raw-records.jsonl", rows.stream()
                    .map(row -> EvaluationArtifacts.toJson(comparisonExport(row, expectations)))
                    .toList());
            LOGGER.info("Evaluation artefacts: " + directory);
        } catch (Exception exception) {
            LOGGER.error("Could not write evaluation artefacts", exception);
        }
    }

    /**
     * Renders the main comparison table and diagnostics.
     *
     * @param rows completed comparison rows
     * @return the plain-text comparison report
     */
    private static String renderReport(List<ComparisonRow> rows) {
        StringBuilder report = new StringBuilder();
        report.append("\n");
        report.append("==================== Robust vs. Baseline Repair Evaluation ====================\n");
        report.append(RecordTableRenderer.render(
                rows.stream().map(ComparisonReportRow::of).toList(), ComparisonReportRow.class));
        report.append("Legend: v/s = violations/violated scenarios in independent full-space validation. ")
                .append("Runtimes are recorded but interpreted only by the scalability evaluation.\n\n");
        appendMechanismDiagnostics(report, rows);
        report.append("The adjacent JSONL artefacts retain source and scenario identifiers for audit.\n");
        report.append("==============================================================================\n");
        return report.toString();
    }

    /**
     * One line of the comparison table.
     *
     * <p>Several cells pair two counts, so they are rendered into strings here instead of being
     * given a column each. The hand-built table this replaced declared six headings over nine
     * printed fields, and every row after the second heading was therefore captioned by the wrong
     * one.
     *
     * @param caseStudy      the case, truncated to keep the table within a terminal width
     * @param scenarioCount  declared scenarios
     * @param basePre         unrepaired violations over violated scenarios
     * @param nominalResidual violations left by the uncertainty-unaware arm, over their scenarios
     * @param nominalObjective the objective that arm reached, or {@code ERR}
     * @param robustResidual violations left by the robust arm, over their scenarios
     * @param robustObjective the objective the robust arm reached
     * @param robustOutcome  how the robust repair ended
     * @param verdict        the verdict assigned to the case
     */
    private record ComparisonReportRow(
            @ReportColumn(value = "Case study", alignment = ReportColumn.Alignment.LEFT) String caseStudy,
            @ReportColumn("S_full") int scenarioCount,
            @ReportColumn("B0 pre v/s") String basePre,
            @ReportColumn("B1 residual v/s") String nominalResidual,
            @ReportColumn("B1 JΘ") String nominalObjective,
            @ReportColumn("R residual v/s") String robustResidual,
            @ReportColumn("R JΘ") String robustObjective,
            @ReportColumn(value = "R outcome", alignment = ReportColumn.Alignment.LEFT) String robustOutcome,
            @ReportColumn(value = "Verdict", alignment = ReportColumn.Alignment.LEFT) String verdict) {

        static ComparisonReportRow of(ComparisonRow row) {
            return new ComparisonReportRow(
                    truncate(row.context().name(), 44),
                    row.base().scenarioCount(),
                    pair(row.base().violations(), row.base().violatedScenarios()),
                    pair(row.nominal().validation().totalViolations(),
                            row.nominal().validation().violatedScenarios()),
                    objective(row.nominal().objective()),
                    pair(row.robust().validation().totalViolations(),
                            row.robust().validation().violatedScenarios()),
                    objective(row.robust().objective()),
                    row.robust().outcome().name(),
                    row.verdict().label());
        }

        private static String pair(long violations, long scenarios) {
            return violations + " / " + scenarios;
        }
    }

    /**
     * Appends selected and candidate repair actions for each comparison case.
     *
     * @param report report under construction
     * @param rows   completed comparison rows
     */
    private static void appendMechanismDiagnostics(StringBuilder report, List<ComparisonRow> rows) {
        report.append(rows.stream().map(row -> "• " + row.context().name() + "  [" + row.verdict().label() + "]\n"
                                               + "    R selected : " + row.robust().selectedActions() + "\n"
                                               + "    candidates : " + row.base().candidateActions() + "\n"
                                               + errorDiagnostics(row)).collect(Collectors.joining("",
                "---- Mechanism diagnostics (selected vs. candidate repair actions) ----\n", "\n")));
    }

    /**
     * Renders the error classes captured while executing a comparison case.
     *
     * @param row completed comparison row
     * @return zero or more error-diagnostic lines
     */
    private static String errorDiagnostics(ComparisonRow row) {
        String baseline = row.nominal().errorDetail() == null ? ""
                : "    B1 error    : " + row.nominal().errorDetail() + "\n";
        String robust = row.robust().errorDetail() == null ? ""
                : "    R error     : " + row.robust().errorDetail() + "\n";
        return baseline + robust;
    }

    /**
     * Renders the price-of-robustness table and its aggregate interpretation.
     *
     * @param rows completed comparison rows
     * @return the price-of-robustness report section
     */
    private static String renderPriceOfRobustness(List<ComparisonRow> rows) {
        StringBuilder report = new StringBuilder();
        report.append("\n---- Price of robustness (Q4) ----\n");
        report.append("PoR = JO(R)/JO(B1); Util = fraction of declared scenarios in which the nominal\n");
        report.append("repair leaves a violation. Shape counts are additive/subtractive/structural.\n\n");
        report.append(RecordTableRenderer.render(
                rows.stream().map(PriceReportRow::of).toList(), PriceReportRow.class));
        appendPriceSummary(report, rows);
        appendPriceSummary(report, rows);
        return report.toString();
    }

    /**
     * One line of the price-of-robustness table.
     *
     * @param caseStudy        the case, truncated to keep the table within a terminal width
     * @param verdict          the verdict assigned to the case
     * @param nominalObjective the objective of the uncertainty-unaware arm, or {@code n/a}
     * @param robustObjective  the objective of the robust arm, or {@code n/a}
     * @param price            the ratio of the two objectives, or {@code n/a}
     * @param utility          the fraction of declared scenarios the nominal repair leaves violated
     * @param nominalShape     additive, subtractive and structural counts of the nominal plan
     * @param robustShape      the same counts for the robust plan
     */
    private record PriceReportRow(
            @ReportColumn(value = "Case", alignment = ReportColumn.Alignment.LEFT) String caseStudy,
            @ReportColumn(value = "Verdict", alignment = ReportColumn.Alignment.LEFT) String verdict,
            @ReportColumn("JO(B1)") String nominalObjective,
            @ReportColumn("JO(R)") String robustObjective,
            @ReportColumn("PoR") String price,
            @ReportColumn("Util") String utility,
            @ReportColumn(value = "B1 shape", alignment = ReportColumn.Alignment.LEFT) String nominalShape,
            @ReportColumn(value = "R shape", alignment = ReportColumn.Alignment.LEFT) String robustShape) {

        static PriceReportRow of(ComparisonRow row) {
            return new PriceReportRow(
                    truncate(row.context().name(), 46),
                    truncate(row.verdict().label(), 20),
                    objective(row.nominal().objective()),
                    objective(row.robust().objective()),
                    ratio(row.priceOfRobustness()),
                    ratio(row.robustnessUtility()),
                    String.valueOf(ActionShape.of(row.nominal().costBreakdown())),
                    String.valueOf(ActionShape.of(row.robust().costBreakdown())));
        }

        private static String ratio(double value) {
            return Double.isNaN(value) ? "n/a" : String.format(Locale.ROOT, "%.2f", value);
        }
    }

    /**
     * Appends aggregate price, coverage, and repair-shape observations.
     *
     * @param report report under construction
     * @param rows   completed comparison rows
     */
    private static void appendPriceSummary(StringBuilder report, List<ComparisonRow> rows) {
        var comparable = rows.stream()
                .filter(row -> !Double.isNaN(row.priceOfRobustness()))
                .toList();
        var paying = comparable.stream()
                .filter(row -> row.priceOfRobustness() > 1.0)
                .toList();
        report.append(String.format("PoR defined for %d of %d cases; %d pay a premium.%n",
                comparable.size(), rows.size(), paying.size()));
        appendPremiumRange(report, paying);
        appendFreeCoverageWarnings(report, comparable);
        appendPlanShapeSummary(report, comparable);
    }

    /**
     * Appends the range of observed robustness premiums when one exists.
     *
     * @param report report under construction
     * @param paying cases whose robust goal exceeds the nominal one
     */
    private static void appendPremiumRange(StringBuilder report, List<ComparisonRow> paying) {
        if (paying.isEmpty()) {
            return;
        }
        double minimum = paying.stream().mapToDouble(ComparisonRow::priceOfRobustness).min().orElseThrow();
        double maximum = paying.stream().mapToDouble(ComparisonRow::priceOfRobustness).max().orElseThrow();
        report.append(String.format(Locale.ROOT, "Premium range %.2f to %.2f.%n", minimum, maximum));
    }

    /**
     * Flags cases that gain measured coverage without a higher goal.
     *
     * @param report     report under construction
     * @param comparable cases with a defined price of robustness
     */
    private static void appendFreeCoverageWarnings(StringBuilder report, List<ComparisonRow> comparable) {
        comparable.stream()
                .filter(row -> row.priceOfRobustness() == 1.0 && row.robustnessUtility() > 0)
                .forEach(row -> report.append("CHECK: ").append(row.context().name())
                        .append(" gains coverage at PoR = 1. A free lunch needs an explanation, not a headline.\n"));
    }

    /**
     * Appends aggregate differences between nominal and robust repair shapes.
     *
     * @param report     report under construction
     * @param comparable cases with a defined price of robustness
     */
    private static void appendPlanShapeSummary(StringBuilder report, List<ComparisonRow> comparable) {
        long shapeChanged = comparable.stream().filter(RobustVsBaselineReport::planShapeChanged).count();
        long removalsDropped = comparable.stream().filter(RobustVsBaselineReport::droppedLabelRemoval).count();
        long removalsEliminated = comparable.stream().filter(RobustVsBaselineReport::eliminatedLabelRemoval).count();
        report.append(String.format("Plan shape differs from nominal in %d of %d cases.%n",
                shapeChanged, comparable.size()));
        report.append(String.format("Robust plan uses fewer label removals in %d cases, and none at all in %d.%n",
                removalsDropped, removalsEliminated));
    }

    /**
     * Determines whether the nominal and robust repair shapes differ.
     *
     * @param row completed comparison row
     * @return {@code true} when the action-shape counts differ
     */
    private static boolean planShapeChanged(ComparisonRow row) {
        return !ActionShape.of(row.nominal().costBreakdown()).equals(ActionShape.of(row.robust().costBreakdown()));
    }

    /**
     * Determines whether robust repair uses fewer label-removal actions.
     *
     * @param row completed comparison row
     * @return {@code true} when robust repair drops at least one label removal
     */
    private static boolean droppedLabelRemoval(ComparisonRow row) {
        return ActionShape.of(row.robust().costBreakdown()).subtractive()
               < ActionShape.of(row.nominal().costBreakdown()).subtractive();
    }

    /**
     * Determines whether robust repair eliminates every label-removal action.
     *
     * @param row completed comparison row
     * @return {@code true} when robust repair has no label removals after dropping some
     */
    private static boolean eliminatedLabelRemoval(ComparisonRow row) {
        return droppedLabelRemoval(row) && ActionShape.of(row.robust().costBreakdown()).subtractive() == 0;
    }

    /**
     * Projects one comparison row onto the stable raw-records JSONL schema.
     *
     * @param row          completed comparison row
     * @param expectations frozen case expectations
     * @return the JSON-specific comparison export
     * @throws IllegalStateException when the case has no frozen expectation
     */
    private static ComparisonExport comparisonExport(
            ComparisonRow row,
            Map<String, CaseExpectation> expectations
    ) {
        var expected = expectationFor(row.context().name(), expectations);
        return new ComparisonExport(
                "effectiveness-comparison",
                costProfile(),
                row.context().name(),
                expected.verdict().label(),
                expected.provenance(),
                expected.sourceTypes(),
                row.context().selectedSourceIds(),
                row.base().scenarioCount(),
                row.robust().consideredScenarioCount(),
                row.robust().validation().scenarioIds().size(),
                row.verdict().label(),
                row.transferRate(),
                baseModelExport(row),
                baselineExport(row),
                robustExport(row));
    }

    /**
     * Projects unrepaired base-model measurements onto the B0 JSON schema.
     *
     * @param row completed comparison row
     * @return the B0 export
     */
    private static BaseModelExport baseModelExport(ComparisonRow row) {
        return new BaseModelExport("B0", row.base().violations(), row.base().violatedScenarios(), row.base().scenarioCount());
    }

    /**
     * Projects nominal-repair measurements onto the B1 JSON schema.
     *
     * @param row completed comparison row
     * @return the B1 export
     */
    private static BaselineExport baselineExport(ComparisonRow row) {
        return new BaselineExport(
                "B1",
                row.nominal().outcome().name(),
                row.nominal().nominalViolations(),
                row.nominal().validation().totalViolations(),
                row.nominal().validation().violatedScenarios(),
                row.nominal().validation().scenarioIds().size(),
                costValue(row.nominal().costBreakdown(), ObjectiveCostBreakdown::objectiveValue),
                costValue(row.nominal().costBreakdown(), ObjectiveCostBreakdown::directActionCost),
                costValue(row.nominal().costBreakdown(), ObjectiveCostBreakdown::sharedEnablerCost),
                countValue(row.nominal().actionCount()),
                "MODIFICATION_PROXY",
                row.nominal().validation().scenarioIds());
    }

    /**
     * Projects robust-repair measurements onto the robust JSON schema.
     *
     * @param row completed comparison row
     * @return the robust repair export
     */
    private static RobustExport robustExport(ComparisonRow row) {
        return new RobustExport(
                "ROBUST",
                row.robust().nominalViolations(),
                row.robust().outcome().name(),
                row.robust().solverStatus(),
                row.robust().pipelineValidationStatus(),
                row.robust().validation().totalViolations(),
                row.robust().validation().violatedScenarios(),
                row.robust().validation().scenarioIds().size(),
                costValue(row.robust().costBreakdown(), ObjectiveCostBreakdown::objectiveValue),
                costValue(row.robust().costBreakdown(), ObjectiveCostBreakdown::directActionCost),
                costValue(row.robust().costBreakdown(), ObjectiveCostBreakdown::sharedEnablerCost),
                countValue(row.robust().actionCount()),
                "MODIFICATION_PROXY",
                row.robust().validation().scenarioIds());
    }

    /**
     * Projects the common repair cost profile onto the raw-records JSONL schema.
     *
     * @return the declared cost-profile export
     */
    private static CostProfileExport costProfile() {
        RepairCostSpecification specification = EvaluationArtifacts.costProfile();
        Map<String, Double> baseCosts = new LinkedHashMap<>();
        for (ActionType actionType : ActionType.values()) {
            baseCosts.put(actionType.toString(), specification.baseCostFor(actionType));
        }
        return new CostProfileExport(
                specification.id(),
                specification.version(),
                specification.unit(),
                specification.impactCoefficient(),
                specification.rippleCoefficient(),
                specification.structuralCoefficient(),
                specification.assuranceCoefficient(),
                baseCosts,
                specification.sharedCostGroups().size());
    }

    /**
     * Returns a cost component, preserving unavailable repairs as {@code null}.
     *
     * @param breakdown repair cost breakdown, or {@code null}
     * @param value     selected cost component
     * @return the cost component, or {@code null} when no repair exists
     */
    private static Double costValue(
            ObjectiveCostBreakdown breakdown,
            ToDoubleFunction<ObjectiveCostBreakdown> value
    ) {
        return breakdown == null ? null : value.applyAsDouble(breakdown);
    }

    /**
     * Returns an action count, preserving unavailable repairs as {@code null}.
     *
     * @param count action count or a negative sentinel
     * @return the action count, or {@code null} when no repair exists
     */
    private static Integer countValue(int count) {
        return count < 0 ? null : count;
    }

    /**
     * Projects one frozen case expectation onto the case-manifest JSONL schema.
     *
     * @param caseId      stable case identifier
     * @param expectation frozen case expectation
     * @return the case-manifest export
     */
    private static CaseManifestExport manifestExport(String caseId, CaseExpectation expectation) {
        return new CaseManifestExport(
                caseId,
                expectation.verdict().label(),
                expectation.fullScenarioCount(),
                expectation.provenance(),
                expectation.sourceTypes());
    }

    /**
     * Looks up the frozen expectation for one executed case.
     *
     * @param caseId       executed case identifier
     * @param expectations frozen case expectations
     * @return the matching expectation
     * @throws IllegalStateException when no matching expectation exists
     */
    private static CaseExpectation expectationFor(String caseId, Map<String, CaseExpectation> expectations) {
        CaseExpectation expectation = expectations.get(caseId);
        if (expectation == null) {
            throw new IllegalStateException("No frozen expectation for case " + caseId);
        }
        return expectation;
    }

    /**
     * Defines the stable raw-records JSONL schema for one comparison case.
     *
     * @param recordType          record category
     * @param costProfile         declared cost profile
     * @param caseId              stable case identifier
     * @param expectedVerdict     predeclared expected verdict
     * @param provenance          model origin and adaptation status
     * @param declaredSourceTypes selected uncertainty-source types
     * @param selectedSourceIds   selected source identifiers
     * @param sFull               full selected-source scenario product
     * @param sConsidered         scenarios considered by robust solving
     * @param sValidated          independently validated robust scenarios
     * @param verdict             observed comparison verdict
     * @param nominalTransferRate nominal repair transfer rate
     * @param b0                  unrepaired base-model measurements
     * @param b1                  nominal-repair measurements
     * @param robust              robust-repair measurements
     */
    private record ComparisonExport(
            String recordType,
            CostProfileExport costProfile,
            String caseId,
            String expectedVerdict,
            String provenance,
            String declaredSourceTypes,
            List<String> selectedSourceIds,
            int sFull,
            int sConsidered,
            int sValidated,
            String verdict,
            double nominalTransferRate,
            BaseModelExport b0,
            BaselineExport b1,
            RobustExport robust) {
    }

    /**
     * Defines the base section of a comparison JSONL record.
     *
     * @param configuration configuration label
     * @param vPre          unrepaired violation count
     * @param hPre          unrepaired violating-scenario count
     * @param sValidated    independently checked scenario count
     */
    private record BaseModelExport(String configuration, int vPre, long hPre, int sValidated) {
    }

    /**
     * Defines the norminal section of a comparison JSONL record.
     *
     * @param configuration       configuration label
     * @param outcome             nominal repair outcome
     * @param vNominal            residual violations in the base model
     * @param violations          residual violations across the scenario product
     * @param violatedScenarios   scenarios with residual violations
     * @param sValidated          independently checked scenario count
     * @param objective           repair objective, or {@code null}
     * @param directCost          direct action cost, or {@code null}
     * @param sharedCost          shared-enabler cost, or {@code null}
     * @param selectedActionCount selected action count, or {@code null}
     * @param comparisonLabel     stable comparison label
     * @param scenarioIds         independently validated scenario identifiers
     */
    private record BaselineExport(
            String configuration,
            String outcome,
            int vNominal,
            int violations,
            long violatedScenarios,
            int sValidated,
            Double objective,
            Double directCost,
            Double sharedCost,
            Integer selectedActionCount,
            String comparisonLabel,
            List<String> scenarioIds) {
    }

    /**
     * Defines the robust section of a comparison JSONL record.
     *
     * @param configuration       configuration label
     * @param vNominal            residual violations in the base model
     * @param outcome             independently validated robust outcome
     * @param solverStatus        robust solver status
     * @param pipelineValidation  robust pipeline validation status
     * @param violations          residual violations across the scenario product
     * @param violatedScenarios   scenarios with residual violations
     * @param sValidated          independently checked scenario count
     * @param objective           repair objective, or {@code null}
     * @param directCost          direct action cost, or {@code null}
     * @param sharedCost          shared-enabler cost, or {@code null}
     * @param selectedActionCount selected action count, or {@code null}
     * @param comparisonLabel     stable comparison label
     * @param scenarioIds         independently validated scenario identifiers
     */
    private record RobustExport(
            String configuration,
            int vNominal,
            String outcome,
            String solverStatus,
            String pipelineValidation,
            int violations,
            long violatedScenarios,
            int sValidated,
            Double objective,
            Double directCost,
            Double sharedCost,
            Integer selectedActionCount,
            String comparisonLabel,
            List<String> scenarioIds) {
    }

    /**
     * Defines the stable case-manifest JSONL schema.
     *
     * @param caseId              stable case identifier
     * @param expectedVerdict     predeclared expected verdict
     * @param sFull               full selected-source scenario product
     * @param provenance          model origin and adaptation status
     * @param declaredSourceTypes selected uncertainty-source types
     */
    private record CaseManifestExport(
            String caseId,
            String expectedVerdict,
            int sFull,
            String provenance,
            String declaredSourceTypes) {
    }

    /**
     * Defines the cost-profile section of a raw comparison JSONL record.
     *
     * @param id                    profile identifier
     * @param version               profile version
     * @param unit                  objective-value unit
     * @param impactCoefficient     impact-cost coefficient
     * @param rippleCoefficient     ripple-cost coefficient
     * @param structuralCoefficient structural-cost coefficient
     * @param assuranceCoefficient  assurance-cost coefficient
     * @param baseCosts             base cost by action type
     * @param sharedCostGroups      number of shared-cost groups
     */
    private record CostProfileExport(
            String id,
            String version,
            String unit,
            double impactCoefficient,
            double rippleCoefficient,
            double structuralCoefficient,
            double assuranceCoefficient,
            Map<String, Double> baseCosts,
            int sharedCostGroups) {
    }

    private static String objective(double value) {
        return value < 0 ? "n/a" : String.format(Locale.ROOT, "%.1f", value);
    }

    private static String truncate(String value, int maximumLength) {
        return value.length() <= maximumLength ? value : value.substring(0, maximumLength - 1) + "...";
    }
}
