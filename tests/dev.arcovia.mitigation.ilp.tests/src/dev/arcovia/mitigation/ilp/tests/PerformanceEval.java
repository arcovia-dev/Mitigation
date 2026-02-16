package dev.arcovia.mitigation.ilp.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.*;
import java.util.*;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.ilp.OptimizationManager;
import dev.arcovia.mitigation.sat.Scaler;
import dev.arcovia.mitigation.sat.timeMeasurement;
import dev.arcovia.mitigation.sat.RunConfig;
import dev.arcovia.mitigation.sat.MeasurementWriter;

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
            List.of(0, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 750);

    private static final List<Integer> TFG_AMOUNT_SCALINGS =
            List.of(0, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 
                    11000, 12000, 13000, 14000, 15000);

    private static final List<Integer> CONSTRAINT_SCALINGS =
            List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                    40, 60, 80, 100, 120, 140, 160, 180, 200, 220, 240);
    

    // -----------------------------
    // Repeats / Warmup
    // -----------------------------
    private static boolean WARMUP = false;
    private static final int MEASUREMENT_REPEATS = 3;

    // -----------------------------
    // Output (CSV) for Jupyter
    // -----------------------------
    private static final Path OUT_DIR = Paths.get("perf-results");
    private static final Path CSV_FILE = OUT_DIR.resolve("performance_measurements.csv");

    // flush every N lines (low overhead, still crash-safe)
    private static final int FLUSH_EVERY = 3;

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
        if (! WARMUP){
            timeMeasurement timer = new timeMeasurement();
            try {
                experiment.run(timer);
                WARMUP = true;
            } catch (OutOfMemoryError oom) {
                throw oom;
            } catch (Throwable t) {
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

}
