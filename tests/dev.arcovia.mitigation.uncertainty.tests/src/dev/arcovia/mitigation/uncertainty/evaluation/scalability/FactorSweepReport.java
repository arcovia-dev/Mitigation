package dev.arcovia.mitigation.uncertainty.evaluation.scalability;

import dev.arcovia.mitigation.uncertainty.helper.RecordTableRenderer;
import dev.arcovia.mitigation.uncertainty.helper.ReportColumn;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders the per-driver sweeps as the human-readable report that goes with the CSV.
 */
final class FactorSweepReport {

    private FactorSweepReport() {
    }

    /**
     * One rendered sweep.
     *
     * @param driver     the quantity of the sweep varied
     * @param scope      the pipeline stages its rows cover
     * @param fixedDesc  what was held fixed
     * @param stopReason why the sweep ended
     * @param rows       its points, in order
     */
    record Sweep(String driver, String scope, String fixedDesc, String stopReason, List<FactorRow> rows) {
    }

    /**
     * One scale point, and the schema of the CSV artifact.
     *
     * @param seriesId              the swept series
     * @param scope                 the pipeline stages the point covers
     * @param driver                the swept quantity
     * @param driverValue           its value at this point
     * @param scenarioCount         materialized scenarios
     * @param violationCount        violating nodes summed over all scenarios
     * @param coverageRowCount      coverage constraints in the program
     * @param canonicalActionCount  distinct canonical actions across all scenarios
     * @param ilpVariableCount      binary action variables
     * @param ilpConstraintCount    constraint rows
     * @param solverStatus          the solver status of the structural measurement
     * @param prepareUs             preparation duration in microseconds
     * @param solveUs               solve duration in microseconds
     * @param retainedBytes         heap held by one materialized scenario set
     * @param peakBytes             summed heap high-water mark over the measured runs
     * @param measuredRuns          measured runs started
     * @param completedMeasuredRuns measured runs that completed
     * @param medianTotalUs         median duration in microseconds
     * @param minTotalUs            fastest measured run
     * @param maxTotalUs            slowest measured run
     * @param outcome               how the measured runs ended
     * @param stopReason            why the sweep stopped after this point, or {@code RUNNING}
     */
    record FactorRow(
            String seriesId,
            String scope,
            String driver,

            @ReportColumn("value")
            int driverValue,

            @ReportColumn("scenarios")
            int scenarioCount,

            @ReportColumn("violations")
            int violationCount,

            @ReportColumn("coverage rows")
            int coverageRowCount,

            @ReportColumn("canonical actions")
            int canonicalActionCount,

            @ReportColumn("ILP vars")
            int ilpVariableCount,

            @ReportColumn("ILP rows")
            int ilpConstraintCount,

            @ReportColumn(
                    value = "solver status",
                    alignment = ReportColumn.Alignment.LEFT
            )
            String solverStatus,

            @ReportColumn(
                    value = "prepare ms",
                    format = ReportColumn.Format.MICROSECONDS_AS_MS
            )
            long prepareUs,

            @ReportColumn(
                    value = "solve ms",
                    format = ReportColumn.Format.MICROSECONDS_AS_MS
            )
            long solveUs,

            @ReportColumn(
                    value = "retained MiB",
                    format = ReportColumn.Format.BYTES_AS_MIB
            )
            long retainedBytes,

            @ReportColumn(
                    value = "peak MiB",
                    format = ReportColumn.Format.BYTES_AS_MIB
            )
            long peakBytes,

            @ReportColumn("runs")
            int measuredRuns,

            @ReportColumn("completed")
            int completedMeasuredRuns,

            @ReportColumn(
                    value = "median ms",
                    format = ReportColumn.Format.MICROSECONDS_AS_MS
            )
            long medianTotalUs,

            @ReportColumn(
                    value = "min ms",
                    format = ReportColumn.Format.MICROSECONDS_AS_MS
            )
            long minTotalUs,

            @ReportColumn(
                    value = "max ms",
                    format = ReportColumn.Format.MICROSECONDS_AS_MS
            )
            long maxTotalUs,

            @ReportColumn(
                    value = "outcome",
                    alignment = ReportColumn.Alignment.LEFT
            )
            String outcome,

            String stopReason
    ) {
    }

    /**
     * Renders every sweep, with the protocol that produced them.
     *
     * @param sweeps       the sweeps, in the order they ran
     * @param warmupRuns   discarded runs per point
     * @param measuredRuns measured runs per point
     * @param budgetMs     the wall-clock budget per point
     * @param maxNextMib   retained set in MiB from which no further point is attempted
     * @param maxTimedMib  retained set in MiB from which a point is not timed
     * @return the report text
     */
    static String render(List<Sweep> sweeps, int warmupRuns, int measuredRuns, long budgetMs,
                         long maxNextMib, long maxTimedMib) {
        return sweeps.stream()
                .map(sweep -> sweep.driver() + " [" + sweep.scope() + "] (" + sweep.fixedDesc() + "), "
                              + (sweep.stopReason().equals("RUNNING")
                        ? "TRUNCATED: the run ended before this driver reached a stop condition"
                        : "stopped by " + sweep.stopReason())
                              + '\n'
                              + RecordTableRenderer.render(sweep.rows(), FactorRow.class) + '\n')
                .collect(Collectors.joining("", """
                        Per-driver scalability sweeps
                        Scope: one sweep per cost driver, every other driver held fixed. Drivers do not share a
                        scope, and the scope column says which stages each row covers. Rows of different scopes
                        must not be placed on one axis.
                        Protocol: %d discarded warm-up and %d measured runs per point. A sweep stops after the
                        first point whose median run reaches %d ms, whose retained heap reaches %d MiB
                        and so leaves no room for the next point, or whose scenario set alone reaches
                        %d MiB and so leaves no room for its own timed runs.
                        """.formatted(warmupRuns, measuredRuns, budgetMs, maxNextMib, maxTimedMib), ""));
    }
}
