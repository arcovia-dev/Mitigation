package dev.arcovia.mitigation.uncertainty.evaluation.scalability;

import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationArtifacts;
import dev.arcovia.mitigation.uncertainty.evaluation.RecordCsv;
import dev.arcovia.mitigation.uncertainty.evaluation.fixtures.FactorSweepFixture.Spec;
import dev.arcovia.mitigation.uncertainty.evaluation.fixtures.ScalabilitySubjectFixtures;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Sweeps each cost driver of the robust pipeline on its own, one sweep per driver, until a point
 * exceeds a wall-clock budget or leaves too little heap to continue.
 * <p>
 * Stop rules:
 * 1. Retained set >= MAX_TIMED_MIB, checked before timing -> [HEAP_HEADROOM, point not timed]
 * 2. A measured run failed -> [RUN_INCOMPLETE:<outcome>]
 * 3. Median run >= BUDGET_MS -> [BUDGET_EXCEEDED]
 * 4. Retained set >= MAX_NEXT_MIB -> [HEAP_HEADROOM, point timed]
 * 5. The subject admits no larger value -> [SUBJECT_EXHAUSTED]
 * 6. MAX_POINTS points measured -> [POINT_CAP]
 */
class FactorScalabilityEvaluationTest {

    private static final Logger LOGGER = Logger.getLogger(FactorScalabilityEvaluationTest.class);

    private static final int GLOBAL_WARMUP_RUNS = Integer.getInteger("arcovia.eval.factorGlobalWarmupRuns", 50);
    private static final int WARMUP_RUNS = Integer.getInteger("arcovia.eval.factorWarmupRuns", 1);
    private static final int MEASURED_RUNS = Integer.getInteger("arcovia.eval.factorMeasuredRuns", 3);

    /**
     * Wall-clock budget per point in milliseconds.
     */
    private static final long BUDGET_MS = Long.getLong("arcovia.eval.factorBudgetMs", 60_000L);

    /**
     * Largest retained scenario set that is still timed, in MiB. Default is 60% of 8 GiB.
     */
    private static final long MAX_TIMED_MIB = Long.getLong("arcovia.eval.factorMaxTimedMib", 4915L);

    /**
     * Largest retained scenario set after which a next point is attempted, in MiB. Default is 35% of 8 GiB.
     */
    private static final long MAX_NEXT_MIB = Long.getLong("arcovia.eval.factorMaxNextMib", 2867L);

    /**
     * Runaway guard
     */
    private static final int MAX_POINTS = 64;

    private static List<Factor> factors() {
        return List.of(
                new Factor("sources", "sourceCount", "generated, choicesPerSource=2, violatingChains=1",
                        1, value -> value + 1, 0,
                        FactorSubject.generated(value -> new Spec(value, 2, 1))),
                new Factor("choices", "choicesPerSource", "generated, sourceCount=2, violatingChains=1",
                        2, value -> value * 2, 0,
                        FactorSubject.generated(value -> new Spec(2, value, 1))),
                new Factor("violations", "violatingChains", "generated, sourceCount=1, choicesPerSource=2",
                        1, value -> value * 2, 0,
                        FactorSubject.generated(value -> new Spec(1, 2, value))),
                new Factor("published", "publishedSources", "koushikkothagal21, C3 authentication rule",
                        1, value -> value + 1, ScalabilitySubjectFixtures.publishedSourceLimit(),
                        FactorSubject.published()),
                new Factor("model", "modelSize", "duplicated motif, no uncertainty, one scenario",
                        1, value -> value * 2, 0, FactorSubject.modelGrowth()),
                new Factor("actions", "actionSpace", "constructed candidates, no model, one violation",
                        2, value -> value * 2, 0, FactorSubject.actionSpace()));
    }

    @Test
    @EnabledIfSystemProperty(named = "arcovia.evaluation", matches = "true",
            disabledReason = "opt-in evaluation run; set -Darcovia.evaluation=true")
    void persistsOneBudgetedSweepPerCostDriver() throws Exception {
        // solver loads its native library on first use
        FactorSubject.actionSpace().structure(2);

        Path directory = EvaluationArtifacts.createRunDirectory(Path.of(System.getProperty("user.dir")),
                "factor-scalability");
        List<SweepState> states = factors().stream().map(SweepState::new).toList();
        warmUpAllDrivers();

        // Breadth first
        for (int step = 0; step < MAX_POINTS && states.stream().anyMatch(SweepState::active); step++) {
            for (SweepState state : states) {
                if (!state.active()) {
                    continue;
                }
                state.record(measure(state.factor(), state.value()));
                // Written after every point, not every sweep!
                writeArtifacts(directory, states.stream().map(SweepState::toSweep).toList());

                if (state.failedOnMemory()) {
                    LOGGER.error("Heap exhausted by " + state.factor().driver() + "; ending the run "
                                 + "because no later measurement on this heap would be trustworthy.");
                    states.forEach(other -> other.retireAsContaminated(state.factor().driver()));
                    break;
                }
            }
        }
        states.forEach(state -> assertFalse(state.points().isEmpty(),
                "Every sweep must measure at least its first point"));
    }

    /**
     * Runs the first point of every driver {@link #GLOBAL_WARMUP_RUNS} times and discards the timings.
     *
     * @throws Exception if a warm-up run fails, because the same point would then fail when measured
     */
    private static void warmUpAllDrivers() throws Exception {
        long started = System.nanoTime();
        for (int run = 0; run < GLOBAL_WARMUP_RUNS; run++) {
            for (Factor factor : factors()) {
                factor.subject().executeUs(factor.firstValue());
            }
        }
        System.gc();
        LOGGER.info("Global warm-up: %d runs of every driver's first point in %d ms".formatted(
                GLOBAL_WARMUP_RUNS, (System.nanoTime() - started) / 1_000_000));
    }

    private Point measure(Factor factor, int value) {
        FactorSubject.Structure structure;
        try {
            structure = factor.subject().structure(value);
        } catch (Exception | OutOfMemoryError failure) {
            System.gc();
            return new Point(factor, value, FactorSubject.failedStructure(failure), List.of(), 0,
                    failure.getClass().getSimpleName(), 0, 0);
        }

        long retainedBytes = structure.retainedBytes();
        if (retainedBytes >= MAX_TIMED_MIB << 20) {
            return new Point(factor, value, structure, List.of(), 0, "HEAP_HEADROOM", retainedBytes, 0);
        }

        resetPeakHeap();
        List<Long> totals = new ArrayList<>(MEASURED_RUNS);
        int completed = 0;
        String outcome = "COMPLETED";
        for (int run = 0; run < WARMUP_RUNS + MEASURED_RUNS; run++) {
            String runOutcome = "COMPLETED";
            long elapsedUs;
            try {
                elapsedUs = factor.subject().executeUs(value);
            } catch (OutOfMemoryError error) {
                runOutcome = "OUT_OF_MEMORY";
                elapsedUs = 0;
            } catch (Exception exception) {
                runOutcome = "EXECUTION_ERROR:" + exception.getClass().getSimpleName();
                elapsedUs = 0;
            }
            if (run < WARMUP_RUNS) {
                continue;
            }
            totals.add(elapsedUs);
            if (runOutcome.equals("COMPLETED")) {
                completed++;
            } else {
                outcome = runOutcome;
            }
        }
        return new Point(factor, value, structure, List.copyOf(totals), completed, outcome,
                retainedBytes, peakHeapBytes());
    }

    private static void resetPeakHeap() {
        ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(pool -> pool.getType() == MemoryType.HEAP)
                .forEach(MemoryPoolMXBean::resetPeakUsage);
    }

    private static long peakHeapBytes() {
        return ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(pool -> pool.getType() == MemoryType.HEAP)
                .mapToLong(pool -> pool.getPeakUsage().getUsed())
                .sum();
    }

    /**
     * Writes the sweeps completed so far, replacing what an earlier call wrote.
     *
     * @param directory the run directory, created once for the whole run
     * @param sweeps    every sweep that has finished
     * @throws Exception if an artifact cannot be written
     */
    private static void writeArtifacts(Path directory, List<Sweep> sweeps) throws Exception {
        EvaluationArtifacts.write(directory, "report.txt", FactorSweepReport.render(
                sweeps.stream().map(Sweep::asReportSweep).toList(),
                GLOBAL_WARMUP_RUNS, WARMUP_RUNS, MEASURED_RUNS, BUDGET_MS, MAX_NEXT_MIB, MAX_TIMED_MIB));
        EvaluationArtifacts.write(directory, "summary.csv", RecordCsv.table(FactorSweepReport.FactorRow.class,
                sweeps.stream().flatMap(sweep -> sweep.rows().stream()).toList()));
        LOGGER.info("Evaluation artefacts: " + directory);
    }

    /**
     * One sweep: a driver, the values it takes, and what is measured at each value.
     *
     * @param id         short identifier used in the records
     * @param driver     the quantity this sweep varies
     * @param fixedDesc  what is held fixed, for the human report
     * @param firstValue the driver value of the first point
     * @param nextValue  the driver value after a given one
     * @param lastValue  the largest value the subject admits, or 0 when unbounded
     * @param subject    what is built and measured at a driver value
     */
    private record Factor(
            String id,
            String driver,
            String fixedDesc,
            int firstValue,
            IntUnaryOperator nextValue,
            int lastValue,
            FactorSubject subject
    ) {
    }

    /**
     * The progress of one driver while the sweeps advance together.
     *
     * <p>Mutable because the drivers interleave: a driver holds its next value and its points across
     * the steps of the outer loop, and retires on its own stop condition while the others continue.
     */
    private static final class SweepState {

        private final Factor factor;
        private final List<Point> points = new ArrayList<>();
        private int value;
        private String stopReason = "POINT_CAP";
        private boolean active = true;

        SweepState(Factor factor) {
            this.factor = factor;
            this.value = factor.firstValue();
        }

        Factor factor() {
            return factor;
        }

        int value() {
            return value;
        }

        boolean active() {
            return active;
        }

        List<Point> points() {
            return points;
        }

        /**
         * Whether this driver's last point ended because the heap was exhausted.
         */
        boolean failedOnMemory() {
            return !points.isEmpty() && points.get(points.size() - 1).outcome().contains("OUT_OF_MEMORY")
                   || !points.isEmpty() && points.get(points.size() - 1).outcome().contains("OutOfMemoryError");
        }

        /**
         * Retires a driver that another driver's heap exhaustion has invalidated.
         *
         * @param culprit the driver that exhausted the heap
         */
        void retireAsContaminated(String culprit) {
            if (!active) {
                return;
            }
            active = false;
            stopReason = "ABORTED: heap exhausted by " + culprit;
        }

        /**
         * Adds a measured point, then either retires this driver or advances it to its next value.
         */
        void record(Point point) {
            points.add(point);
            LOGGER.info(factor.driver() + "=" + value + " scenarios=" + point.structure().scenarioCount()
                        + " median=" + point.medianTotalUs() / 1_000 + "ms retained="
                        + Math.round(point.retainedMb()) + "MB outcome=" + point.outcome());

            String stop = stopReasonFor(point);
            if (stop != null) {
                stopReason = stop;
                active = false;
                return;
            }
            int next = factor.nextValue().applyAsInt(value);
            if (next <= 0) {
                stopReason = "VALUE_OVERFLOW";
                active = false;
                return;
            }
            value = factor.lastValue() > 0 ? Math.min(next, factor.lastValue()) : next;
        }

        /**
         * Why this driver stops after the given point, or {@code null} to take another one.
         */
        private String stopReasonFor(Point point) {
            if (point.outcome().equals("HEAP_HEADROOM")) {
                return "HEAP_HEADROOM";
            }
            if (!point.allMeasuredCompleted()) {
                return "RUN_INCOMPLETE:" + point.outcome();
            }
            if (point.medianTotalUs() >= BUDGET_MS * 1_000) {
                return "BUDGET_EXCEEDED";
            }
            if (point.retainedBytes() >= MAX_NEXT_MIB << 20) {
                return "HEAP_HEADROOM";
            }
            if (factor.lastValue() > 0 && value >= factor.lastValue()) {
                return "SUBJECT_EXHAUSTED";
            }
            return null;
        }

        /**
         * The sweep as it stands, for a report written while the run is still going.
         *
         * @return the points measured so far, and the stop reason once this driver has retired
         */
        Sweep toSweep() {
            return new Sweep(factor, List.copyOf(points), active ? "RUNNING" : stopReason);
        }
    }

    private record Point(
            Factor factor,
            int value,
            FactorSubject.Structure structure,
            List<Long> totals,
            int completedMeasuredRuns,
            String outcome,
            long retainedBytes,
            long peakBytes
    ) {

        boolean allMeasuredCompleted() {
            return completedMeasuredRuns == totals.size() && !totals.isEmpty();
        }

        long medianTotalUs() {
            long[] sorted = totals.stream().mapToLong(Long::longValue).sorted().toArray();
            if (sorted.length == 0) {
                return 0;
            }
            int middle = sorted.length / 2;
            return sorted.length % 2 == 0 ? (sorted[middle - 1] + sorted[middle]) / 2 : sorted[middle];
        }

        long minTotalUs() {
            return totals.stream().mapToLong(Long::longValue).min().orElse(0);
        }

        long maxTotalUs() {
            return totals.stream().mapToLong(Long::longValue).max().orElse(0);
        }

        double retainedMb() {
            return retainedBytes / (1024.0 * 1024.0);
        }
    }

    private record Sweep(
            Factor factor,
            List<Point> points,
            String stopReason
    ) {

        List<FactorSweepReport.FactorRow> rows() {
            return points.stream().map(point -> new FactorSweepReport.FactorRow(
                    "factor-" + factor.id(), factor.subject().scope(), factor.driver(), point.value(),
                    point.structure().scenarioCount(), point.structure().violations(),
                    point.structure().coverageRows(), point.structure().canonicalActions(),
                    point.structure().ilpVariables(), point.structure().ilpConstraints(),
                    point.structure().solverStatus(), point.structure().prepareUs(),
                    point.structure().solveUs(), point.retainedBytes(), point.peakBytes(),
                    point.totals().size(), point.completedMeasuredRuns(), point.medianTotalUs(),
                    point.minTotalUs(), point.maxTotalUs(), point.outcome(),
                    point == points.get(points.size() - 1) ? stopReason : "RUNNING")).toList();
        }

        FactorSweepReport.Sweep asReportSweep() {
            return new FactorSweepReport.Sweep(factor.driver(), factor.subject().scope(),
                    factor.fixedDesc(), stopReason, rows());
        }
    }
}
