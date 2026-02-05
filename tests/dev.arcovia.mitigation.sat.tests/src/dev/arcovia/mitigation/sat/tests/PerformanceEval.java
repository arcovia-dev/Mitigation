package dev.arcovia.mitigation.sat.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import dev.arcovia.mitigation.sat.Scaler;
import dev.arcovia.mitigation.sat.timeMeasurement;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import dev.arcovia.mitigation.sat.RunConfig;
import dev.arcovia.mitigation.sat.MeasurementWriter;
import dev.arcovia.mitigation.sat.Mechanic;
import dev.arcovia.mitigation.sat.Constraint;

public class PerformanceEval {
    private final String MinDFD = "models/sourceSink.json";

    AnalysisConstraint constraint = new ConstraintDSL().ofData()
            .withLabel("Sensitivity", "Personal")
            .withoutLabel("Encryption", "encrypted")
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
                
                var translation = new CNFTranslation(constraint);
                
                var constraints = translation.constructCNF();
                
                try {
                    DataFlowDiagramAndDictionary dfd = new Mechanic(scaledDFD, "MinDFD", constraints).repair();
                    assertTrue(new Mechanic(dfd, null, null).isViolationFree(dfd, constraints));
                } catch (ContradictionException | TimeoutException | IOException e) {
                    e.printStackTrace();
                }
                
            });
        }
    }

    private void scaleTFGAmount(MeasurementWriter writer) {
        for (int scaling : TFG_AMOUNT_SCALINGS) {
            RunConfig cfg = RunConfig.forTFG("tfg_amount", 0, scaling);
            runWithWarmupAndRepeats(writer, cfg, (timer) -> {
                Scaler scaler = new Scaler(MinDFD);
                var scaledDFD = scaler.scaleTFGAmount(scaling);

                var translation = new CNFTranslation(constraint);
                
                var constraints = translation.constructCNF();
                
                try {
                    DataFlowDiagramAndDictionary dfd = new Mechanic(scaledDFD, "MinDFD", constraints).repair();

                    assertTrue(new Mechanic(dfd, null, null).isViolationFree(dfd, constraints));
                } catch (ContradictionException | TimeoutException | IOException e) {
                    e.printStackTrace();
                }
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

            var DSLconstraints = scaler.scaleConstraint(
                    amountConstraint, numberWithLabel, numberWithoutLabel,
                    numberWithCharacteristic, numberWithoutCharacteristic,
                    numberDummyLabels
            );
            
            List<Constraint> constraints = new ArrayList<>();
            
            
            
            for (var c : DSLconstraints) {
                var translation = new CNFTranslation(c);
                constraints.addAll(translation.constructCNF());
            }
            
            
            try {
                DataFlowDiagramAndDictionary dfd = new Mechanic(scaledDFD, "MinDFD", constraints).repair();

                assertTrue(new Mechanic(dfd, null, null).isViolationFree(dfd, constraints));
            } catch (ContradictionException | TimeoutException | IOException e) {
                e.printStackTrace();
            }
            
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
                timer.start();
                experiment.run(timer);
                timer.stop();
                timer.analysis();
                timer.solving();
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
