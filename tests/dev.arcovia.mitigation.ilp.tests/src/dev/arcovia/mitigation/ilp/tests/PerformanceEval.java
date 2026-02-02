package dev.arcovia.mitigation.ilp.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.ilp.OptimizationManager;
import dev.arcovia.mitigation.sat.Scaler;
import dev.arcovia.mitigation.ilp.timeMeasurement;

public class PerformanceEval {

    private final String MinDFD = "models/sourceSink.json";

    AnalysisConstraint constraint = new ConstraintDSL().ofData()
            .withLabel("Sensitivity", "Personal")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Location", "nonEU")
            .create();

    // -----------------------------
    // Meaningful scalings
    // NOTE: no duplicates needed anymore; warmup is explicit below
    // -----------------------------
    private static final List<Integer> TFG_LENGTH_SCALINGS =
            List.of(0, 1, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256, 384, 512);

    private static final List<Integer> TFG_AMOUNT_SCALINGS =
            List.of(0, 1, 2, 4, 8, 16, 32, 64, 96, 128, 192, 256, 384, 512,
                    768, 1024, 1536, 2024, 2500, 3000, 3500, 4000, 5000, 6000, 7000, 8000, 9000, 10000);

    private static final List<Integer> CONSTRAINT_SCALINGS =
            List.of(1, 2, 5, 10, 20, 25, 30, 35, 40, 45, 50, 55, 60, 70, 80, 90, 100);

    // -----------------------------
    // Repeats / Warmup
    // -----------------------------
    private static final int WARMUP_RUNS = 1;
    private static final int MEASUREMENT_REPEATS = 3;

    // -----------------------------
    // Output (CSV) for Jupyter
    // -----------------------------
    private static final Path OUT_DIR = Paths.get("perf-results");
    private static final Path CSV_FILE = OUT_DIR.resolve("performance_measurements.csv");

    // flush every N lines (low overhead, still crash-safe)
    private static final int FLUSH_EVERY = 20;

    // -----------------------------
    // Runner entrypoint
    // -----------------------------
    public static void main(String[] args) throws Exception {
        PerformanceEval eval = new PerformanceEval();
        eval.runAllExperiments();
        System.out.println("Done.");
    }

    public void runAllExperiments() throws Exception {
        Files.createDirectories(OUT_DIR);

        // Load already-done runIds (so you can restart after crash/OOM)
        Set<String> done = MeasurementWriter.loadDoneRunIds(CSV_FILE);

        try (MeasurementWriter writer = new MeasurementWriter(CSV_FILE, done, FLUSH_EVERY)) {
            scaleTFGLength(writer);
            scaleTFGAmount(writer);
            scaleConstraints(writer);
        }
    }

    // -----------------------------
    // JUnit wrappers
    // -----------------------------
    @Test
    @Disabled("Long-running performance experiment: run via main() to avoid timeouts and IDE issues")
    public void scaleTFGLength_test() throws Exception {
        Files.createDirectories(OUT_DIR);
        Set<String> done = MeasurementWriter.loadDoneRunIds(CSV_FILE);
        try (MeasurementWriter writer = new MeasurementWriter(CSV_FILE, done, FLUSH_EVERY)) {
            scaleTFGLength(writer);
        }
    }

    @Test
    @Disabled("Long-running performance experiment: run via main() to avoid timeouts and IDE issues")
    public void scaleTFGAmount_test() throws Exception {
        Files.createDirectories(OUT_DIR);
        Set<String> done = MeasurementWriter.loadDoneRunIds(CSV_FILE);
        try (MeasurementWriter writer = new MeasurementWriter(CSV_FILE, done, FLUSH_EVERY)) {
            scaleTFGAmount(writer);
        }
    }

    @Test
    @Disabled("Long-running performance experiment: run via main() to avoid timeouts and IDE issues")
    public void scaleConstraints_test() throws Exception {
        Files.createDirectories(OUT_DIR);
        Set<String> done = MeasurementWriter.loadDoneRunIds(CSV_FILE);
        try (MeasurementWriter writer = new MeasurementWriter(CSV_FILE, done, FLUSH_EVERY)) {
            scaleConstraints(writer);
        }
    }

    // -----------------------------
    // Experiments
    // -----------------------------
    private void scaleTFGLength(MeasurementWriter writer) {
        for (int scaling : TFG_LENGTH_SCALINGS) {
            RunConfig cfg = RunConfig.forTFG("tfg_length", scaling, 0);
            runWithWarmupAndRepeats(writer, cfg, (timer) -> {
                Scaler scaler = new Scaler(MinDFD);
                var scaledDFD = scaler.scaleTFGLength(scaling);

                OptimizationManager optimization = new OptimizationManager(scaledDFD, List.of(constraint));
                var dfd = optimization.repair(timer);
                assertTrue(optimization.isViolationFree(dfd));
            });
        }
    }

    private void scaleTFGAmount(MeasurementWriter writer) {
        for (int scaling : TFG_AMOUNT_SCALINGS) {
            RunConfig cfg = RunConfig.forTFG("tfg_amount", 0, scaling);
            runWithWarmupAndRepeats(writer, cfg, (timer) -> {
                Scaler scaler = new Scaler(MinDFD);
                var scaledDFD = scaler.scaleTFGAmount(scaling);

                OptimizationManager optimization = new OptimizationManager(scaledDFD, List.of(constraint));
                var dfd = optimization.repair(timer);
                assertTrue(optimization.isViolationFree(dfd));
            });
        }
    }

    private void scaleConstraints(MeasurementWriter writer) {
        int numberDummyLabels = 400;

        for (int s : CONSTRAINT_SCALINGS) {
            runConstraintCase(writer, numberDummyLabels, "constraints_amountConstraint", s, 1, 1, 1, 1);
        }
        for (int s : CONSTRAINT_SCALINGS) {
            runConstraintCase(writer, numberDummyLabels, "constraints_numberWithLabel", 1, s, 1, 1, 1);
        }
        for (int s : CONSTRAINT_SCALINGS) {
            runConstraintCase(writer, numberDummyLabels, "constraints_numberWithoutLabel", 1, 1, s, 1, 1);
        }
        for (int s : CONSTRAINT_SCALINGS) {
            runConstraintCase(writer, numberDummyLabels, "constraints_numberWithCharacteristic", 1, 1, 1, s, 1);
        }
        for (int s : CONSTRAINT_SCALINGS) {
            runConstraintCase(writer, numberDummyLabels, "constraints_numberWithoutCharacteristic", 1, 1, 1, 1, s);
        }

        // all together
        for (int s : CONSTRAINT_SCALINGS) {
            int half = s / 2;
            runConstraintCase(writer, numberDummyLabels, "constraints_allTogether", s, half, half, half, half);
        }
    }

    private void runConstraintCase(
            MeasurementWriter writer,
            int numberDummyLabels,
            String experimentName,
            int amountConstraint,
            int numberWithLabel,
            int numberWithoutLabel,
            int numberWithCharacteristic,
            int numberWithoutCharacteristic
    ) {
        RunConfig cfg = RunConfig.forConstraints(
                experimentName,
                amountConstraint, numberWithLabel, numberWithoutLabel,
                numberWithCharacteristic, numberWithoutCharacteristic,
                numberDummyLabels
        );

        runWithWarmupAndRepeats(writer, cfg, (timer) -> {
            Scaler scaler = new Scaler(MinDFD);
            var scaledDFD = scaler.scaleLabels(numberDummyLabels);

            var constraints = scaler.scaleConstraint(
                    amountConstraint, numberWithLabel, numberWithoutLabel,
                    numberWithCharacteristic, numberWithoutCharacteristic,
                    numberDummyLabels
            );

            OptimizationManager optimization = new OptimizationManager(scaledDFD, constraints);
            var dfd = optimization.repair(timer);
            assertTrue(optimization.isViolationFree(dfd));
        });
    }

    // -----------------------------
    // Warmup + repeats runner
    // -----------------------------
    private void runWithWarmupAndRepeats(
            MeasurementWriter writer,
            RunConfig cfg,
            RunnableExperiment experiment
    ) {
        // warmups
        for (int w = 0; w < WARMUP_RUNS; w++) {
            String runType = "warmup";
            int repeatIndex = -1 - w; // -1, -2, ... if multiple warmups

            String runId = MeasurementWriter.runId(cfg, runType, repeatIndex);
            if (writer.isDone(runId)) continue;

            timeMeasurement timer = new timeMeasurement();
            try {
                experiment.run(timer);
                writer.append(cfg, timer, runType, repeatIndex);
            } catch (OutOfMemoryError oom) {
                writer.appendFailure(cfg, runType, repeatIndex, "OutOfMemoryError");
                throw oom;
            } catch (Throwable t) {
                writer.appendFailure(cfg, runType, repeatIndex, t.getClass().getSimpleName() + ": " + safeMsg(t));
                throw t;
            }
        }

        // measurements
        for (int i = 0; i < MEASUREMENT_REPEATS; i++) {
            String runType = "measurement";
            int repeatIndex = i;

            String runId = MeasurementWriter.runId(cfg, runType, repeatIndex);
            if (writer.isDone(runId)) continue;

            timeMeasurement timer = new timeMeasurement();
            try {
                experiment.run(timer);
                writer.append(cfg, timer, runType, repeatIndex);
            } catch (OutOfMemoryError oom) {
                writer.appendFailure(cfg, runType, repeatIndex, "OutOfMemoryError");
                throw oom;
            } catch (Throwable t) {
                writer.appendFailure(cfg, runType, repeatIndex, t.getClass().getSimpleName() + ": " + safeMsg(t));
                throw t;
            }
        }
    }

    @FunctionalInterface
    private interface RunnableExperiment {
        void run(timeMeasurement timer);
    }

    private static String safeMsg(Throwable t) {
        String m = t.getMessage();
        if (m == null) return "";
        return m.length() > 200 ? m.substring(0, 200) : m;
    }

    // -----------------------------
    // Config + Writer
    // -----------------------------
    private record RunConfig(
            String experiment,
            int tfgLengthScaling,
            int tfgAmountScaling,
            int amountConstraint,
            int numberWithLabel,
            int numberWithoutLabel,
            int numberWithCharacteristic,
            int numberWithoutCharacteristic,
            int numberDummyLabels
    ) {
        static RunConfig forTFG(String experiment, int tfgLengthScaling, int tfgAmountScaling) {
            return new RunConfig(experiment, tfgLengthScaling, tfgAmountScaling,
                    1, 1, 1, 1, 1, 0);
        }

        static RunConfig forConstraints(
                String experiment,
                int amountConstraint,
                int numberWithLabel,
                int numberWithoutLabel,
                int numberWithCharacteristic,
                int numberWithoutCharacteristic,
                int numberDummyLabels
        ) {
            return new RunConfig(experiment, 0, 0,
                    amountConstraint, numberWithLabel, numberWithoutLabel,
                    numberWithCharacteristic, numberWithoutCharacteristic,
                    numberDummyLabels);
        }

        String key() {
            return String.join("|",
                    experiment,
                    "tfgL=" + tfgLengthScaling,
                    "tfgA=" + tfgAmountScaling,
                    "ac=" + amountConstraint,
                    "wl=" + numberWithLabel,
                    "wol=" + numberWithoutLabel,
                    "wc=" + numberWithCharacteristic,
                    "woc=" + numberWithoutCharacteristic,
                    "dummy=" + numberDummyLabels
            );
        }
    }

    private static final class MeasurementWriter implements AutoCloseable {
        private final Path csv;
        private final BufferedWriter out;
        private final Set<String> doneRunIds;
        private final int flushEvery;
        private int linesSinceFlush = 0;

        MeasurementWriter(Path csv, Set<String> doneRunIds, int flushEvery) throws IOException {
            this.csv = csv;
            this.doneRunIds = doneRunIds;
            this.flushEvery = Math.max(1, flushEvery);

            boolean exists = Files.exists(csv);
            this.out = Files.newBufferedWriter(
                    csv,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );

            if (!exists) {
                out.write(String.join(";",
                        "timestamp",
                        "runId",
                        "key",
                        "experiment",
                        "tfgLengthScaling",
                        "tfgAmountScaling",
                        "amountConstraint",
                        "numberWithLabel",
                        "numberWithoutLabel",
                        "numberWithCharacteristic",
                        "numberWithoutCharacteristic",
                        "numberDummyLabels",
                        "runType",
                        "repeatIndex",
                        "executionTime",
                        "solvingTime",
                        "isolatedExecution",
                        "status",
                        "error"
                ));
                out.newLine();
                out.flush();
            }
        }

        static String runId(RunConfig cfg, String runType, int repeatIndex) {
            return cfg.key() + "|" + runType + "|" + repeatIndex;
        }

        static Set<String> loadDoneRunIds(Path csv) {
            if (!Files.exists(csv)) return new HashSet<>();
            try {
                List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
                if (lines.isEmpty()) return new HashSet<>();

                // Column order: timestamp,runId,key,...
                return lines.stream()
                        .skip(1)
                        .map(line -> line.split(",", 3)) // timestamp,runId,key...
                        .filter(parts -> parts.length >= 2)
                        .map(parts -> parts[1])
                        .collect(Collectors.toCollection(HashSet::new));
            } catch (IOException e) {
                return new HashSet<>();
            }
        }

        boolean isDone(String runId) {
            return doneRunIds.contains(runId);
        }

        void append(RunConfig cfg, timeMeasurement timer, String runType, int repeatIndex) {
            String runId = runId(cfg, runType, repeatIndex);
            doneRunIds.add(runId);

            writeLine(cfg,
                    timer.getExecutionTime(),
                    timer.getSolvingTime(),
                    timer.getIsolatedExecution(),
                    "ok",
                    "",
                    runId,
                    runType,
                    repeatIndex);
        }

        void appendFailure(RunConfig cfg, String runType, int repeatIndex, String error) {
            String runId = runId(cfg, runType, repeatIndex);
            doneRunIds.add(runId);

            writeLine(cfg,
                    -1, -1, -1,
                    "fail",
                    escapeCsv(error),
                    runId,
                    runType,
                    repeatIndex);
        }

        private void writeLine(
                RunConfig cfg,
                long exec,
                long solve,
                long iso,
                String status,
                String error,
                String runId,
                String runType,
                int repeatIndex
        ) {
            try {
                out.write(String.join(";",
                        Instant.now().toString(),
                        runId,
                        cfg.key(),
                        cfg.experiment(),
                        String.valueOf(cfg.tfgLengthScaling()),
                        String.valueOf(cfg.tfgAmountScaling()),
                        String.valueOf(cfg.amountConstraint()),
                        String.valueOf(cfg.numberWithLabel()),
                        String.valueOf(cfg.numberWithoutLabel()),
                        String.valueOf(cfg.numberWithCharacteristic()),
                        String.valueOf(cfg.numberWithoutCharacteristic()),
                        String.valueOf(cfg.numberDummyLabels()),
                        runType,
                        String.valueOf(repeatIndex),
                        String.valueOf(exec),
                        String.valueOf(solve),
                        String.valueOf(iso),
                        status,
                        error
                ));
                out.newLine();

                linesSinceFlush++;
                if (linesSinceFlush >= flushEvery) {
                    out.flush();
                    linesSinceFlush = 0;
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to write measurement to CSV: " + csv, e);
            }
        }

        private static String escapeCsv(String s) {
            if (s == null) return "";
            return s.replace("\n", " ").replace("\r", " ").replace(",", ";");
        }

        @Override
        public void close() throws IOException {
            out.flush();
            out.close();
        }
    }
}
