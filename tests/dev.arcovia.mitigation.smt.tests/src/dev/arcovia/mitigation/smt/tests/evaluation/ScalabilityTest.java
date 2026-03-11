package dev.arcovia.mitigation.smt.tests.evaluation;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.sat.Scaler;
import dev.arcovia.mitigation.smt.Mitigation;
import dev.arcovia.mitigation.smt.SolvingResult;
import dev.arcovia.mitigation.smt.tests.evaluation.SatHelper.RepairResult;
import dev.arcovia.mitigation.smt.utils.ParsingUtils;

/**
 * @author Nikolas Rank Compares the runtime of SMT to SAT when different scalability dimensions are considered
 */
public class ScalabilityTest {
    private static final Logger logger = Logger.getLogger(ScalabilityTest.class);

    private static final int RUNS_PER_CONFIGURATION = 10;
    private static final long MAX_TIME_MILLIS = TimeUnit.MINUTES.toMillis(60);
    private static final boolean evaluateSMT = true;
    private static final boolean evaluateSAT = true;

    private record ScaleInput(DataFlowDiagramAndDictionary inputDfd, List<AnalysisConstraint> inputConstraints, int scaleFactor) {
    }

    private record ScaleOutput(DataFlowDiagramAndDictionary outputDfd, List<AnalysisConstraint> outputConstraints) {
    }

    private static int powerOfTwo(int power) {
        return Integer.parseInt(BigInteger.TWO.pow(power)
                .toString());
    }

    public void scalabilityTest(Function<ScaleInput, ScaleOutput> scaleFunc, String name) throws Exception {
        if (evaluateSMT) {
            smtScalabilityTest(scaleFunc, name);
        }
        if (evaluateSAT) {
            satScalabilityTest(scaleFunc, name);
        }
    }

    private static boolean isExponentialTest(String name) {
        Set<String> exponentialTests = Set.of("labelTypes", "labels", "tfgLength", "labelsAfterNeverFlows", "labelsBeforeNeverFlows",
                "numConstraints");
        return exponentialTests.contains(name);
    }

    private static int maybeExponentiate(int scale, String name) {
        if (isExponentialTest(name)) {
            return powerOfTwo(scale);
        } else {
            return scale;
        }
    }

    private record SMTScalabiltyResult(int scale, int runsPerConfiguration, long totalRuntimeSMT, List<Long> runtimesSMT, long totalTimeFindTFGs,
            List<Long> findTFGsTime) {
    };

    private record SATScalabilityResult(int scale, int runsPerConfiguration, long totalRuntimeSAT, List<Long> runtimesSAT) {
    };

    private static final Set<String> cyclicDFDs = Set.of("anilallewar", "mudigal-technologies", "yidongnan", "spring-petclinic", "callistaenterprise",
            "apssouza22", "sqshq");

    private static void satScalabilityTest(Function<ScaleInput, ScaleOutput> scaleFunc, String name) throws Exception {
        List<EvaluationSupport.Configuration> configs = EvaluationSupport.configurations();

        Path outPath = Path.of("testresults/results/scalabilityResults/" + name + "/" + RUNS_PER_CONFIGURATION + "Runs" + (MAX_TIME_MILLIS / 60000)
                + "Minutes" + "/satData.json");

        int scale = 0;
        long totalRuntimeSat = 0;
        List<SATScalabilityResult> results = new ArrayList<>();

        // in some dimensions we scale exponentially, linearly in others
        int scaleOrExp = maybeExponentiate(scale, name);

        // Increase scale while runtime is below bound and scale maximum is not reached
        while (totalRuntimeSat < MAX_TIME_MILLIS && scaleOrExp <= 2048) {

            List<Long> satRuntimes = new ArrayList<>();
            totalRuntimeSat = 0;

            // For all TUHH-Model & Constraint Pairs
            for (EvaluationSupport.Configuration cfg : configs) {

                if (name.equals("tfgAmount") && cyclicDFDs.contains(cfg.model())) {
                    continue;
                }

                for (int runIdx = 0; runIdx < RUNS_PER_CONFIGURATION; runIdx++) {
                    DataFlowDiagramAndDictionary base = ParsingUtils.loadDFD(cfg.model(), cfg.model() + "_0");
                    // Scale
                    ScaleOutput out = scaleFunc.apply(new ScaleInput(base, cfg.constraints(), scaleOrExp));

                    // Run Sat repair
                    RepairResult rr = SatHelper.runRepair(out.outputDfd, false, out.outputConstraints, SatHelper.MIN_COSTS);
                    if (runIdx == 0) {
                        if (rr.violationsAfter() > 0 || ParsingUtils.countViolations(rr.repairedDfd(), cfg.constraints()) > 0) {
                            throw new IllegalStateException("SAT invalid at scale=" + scaleOrExp + " for " + cfg.model() + "_" + cfg.variantId());
                        }
                    }

                    // Save outputs
                    long dt = rr.runtimeInMilliseconds();
                    satRuntimes.add(dt);
                    totalRuntimeSat += dt;
                }
            }
            results.add(new SATScalabilityResult(scaleOrExp, RUNS_PER_CONFIGURATION, totalRuntimeSat, satRuntimes));
            EvaluationSupport.writeJson(outPath, results);
            
            logger.info("Wrote results to " + outPath);

            scale++;
            scaleOrExp = maybeExponentiate(scale, name);
        }

    }

    private static void smtScalabilityTest(Function<ScaleInput, ScaleOutput> scaleFunc, String name) throws Exception {
        List<EvaluationSupport.Configuration> configs = EvaluationSupport.configurations();
        Path outPath = Path.of("testresults/results/scalabilityResults/" + name + "/" + RUNS_PER_CONFIGURATION + "Runs" + (MAX_TIME_MILLIS / 60000)
                + "Minutes" + "/smtData.json");

        int scale = 0;
        long totalRuntimeSmt = 0;
        // in some dimensions we scale exponentially, linearly in others
        int scaleOrExp = maybeExponentiate(scale, name);

        List<SMTScalabiltyResult> results = new ArrayList<>();

        // Increase scale while runtime is below bound and scale maximum is not reached
        while (totalRuntimeSmt < MAX_TIME_MILLIS && scaleOrExp <= 2048) {
            List<Long> smtRuntimes = new ArrayList<>();
            List<Long> findTFGsTimes = new ArrayList<>();

            totalRuntimeSmt = 0;
            long totalTimeFindTFGs = 0;

            // For all TUHH-Model & Constraint Pairs
            for (EvaluationSupport.Configuration cfg : configs) {

                if (name.equals("tfgAmount") && cyclicDFDs.contains(cfg.model())) {
                    continue;
                }

                for (int runIdx = 0; runIdx < RUNS_PER_CONFIGURATION; runIdx++) {

                    DataFlowDiagramAndDictionary base = ParsingUtils.loadDFD(cfg.model(), cfg.model() + "_0");
                    // Scale respective dimension
                    ScaleOutput out = scaleFunc.apply(new ScaleInput(base, cfg.constraints(), scaleOrExp));

                    // Check smt Time
                    long before = System.currentTimeMillis();
                    SolvingResult solving = Mitigation.run(out.outputDfd, out.outputConstraints, null);
                    long after = System.currentTimeMillis();

                    // Check for violations only once
                    if (runIdx == 0) {
                        if (!solving.satisfiable() || ParsingUtils.countViolations(solving.repairedDFD(), cfg.constraints()) > 0) {
                            throw new IllegalStateException("SMT invalid at scale=" + scaleOrExp + " for " + cfg.model() + "_" + cfg.variantId());
                        }
                    }
                    // Save outputs
                    long dt = after - before;
                    smtRuntimes.add(dt);
                    totalRuntimeSmt += dt;

                    totalTimeFindTFGs += solving.findTFGsTimeMs();
                    findTFGsTimes.add(solving.findTFGsTimeMs());
                }
            }
            results.add(new SMTScalabiltyResult(scaleOrExp, RUNS_PER_CONFIGURATION, totalRuntimeSmt, smtRuntimes, totalTimeFindTFGs, findTFGsTimes));
            EvaluationSupport.writeJson(outPath, results);
            System.out.println("Wrote results to " + outPath + " for " + scaleOrExp);

            scale++;
            scaleOrExp = maybeExponentiate(scale, name);
        }
    }

    @Disabled
    @Test
    public void testTFGAmount() throws Exception {
        scalabilityTest(scaleTFGAmount, "tfgAmount");
    }

    // Scale TFG amount linearly
    private static final Function<ScaleInput, ScaleOutput> scaleTFGAmount = (in) -> {
        Scaler scaler = new Scaler(in.inputDfd);

        DataFlowDiagramAndDictionary dfdAfter = scaler.scaleTFGAmount(in.scaleFactor);

        return new ScaleOutput(dfdAfter, in.inputConstraints);
    };

    @Disabled
    @Test
    public void testTFGLength() throws Exception {
        scalabilityTest(scaleTFGLength, "tfgLength");
    }

    // Scale TFG length exponentially because effect is low
    private static final Function<ScaleInput, ScaleOutput> scaleTFGLength = (in) -> {
        Scaler scaler = new Scaler(in.inputDfd);
        return new ScaleOutput(scaler.scaleTFGLength(in.scaleFactor), in.inputConstraints);
    };

    @Disabled
    @Test
    public void testLabelTypes() throws Exception {
        scalabilityTest(scaleLabelTypes, "labelTypes");
    }

    // Scale label types exponentially because effect is low
    private static final Function<ScaleInput, ScaleOutput> scaleLabelTypes = (in) -> {
        Scaler scaler = new Scaler(in.inputDfd);
        return new ScaleOutput(scaler.scaleLabelTypes(in.scaleFactor), in.inputConstraints);
    };

    @Disabled
    @Test
    public void testLabels() throws Exception {
        scalabilityTest(scaleLabels, "labels");
    }

    // Scale labels exponentially because effect is low
    private static final Function<ScaleInput, ScaleOutput> scaleLabels = (in) -> {
        Scaler scaler = new Scaler(in.inputDfd);
        return new ScaleOutput(scaler.scaleLabels(in.scaleFactor), in.inputConstraints);
    };

    @Disabled
    @Test
    public void testLabelsInConstraintBeforeNeverFlows() throws Exception {
        scalabilityTest(scaleLabelsInConstraintBeforeNeverFlows, "labelsBeforeNeverFlows");
    }

    // Scale single constraint with increasing amount of data selectors linearly.
    // Include the existing constraints for config as well.
    private static final Function<ScaleInput, ScaleOutput> scaleLabelsInConstraintBeforeNeverFlows = (in) -> {
        Scaler scaler = new Scaler(in.inputDfd);
        DataFlowDiagramAndDictionary dfdWithLabels = scaler.scaleLabels(Math.max(4, in.scaleFactor * 3));

        List<AnalysisConstraint> all = new ArrayList<>(in.inputConstraints);
        all.addAll(scaler.scaleConstraint(1, in.scaleFactor, in.scaleFactor, 0, 0, Math.max(4, in.scaleFactor * 3)));

        return new ScaleOutput(dfdWithLabels, all);

    };

    @Disabled
    @Test
    public void testLabelsInConstraintAfterNeverFlows() throws Exception {
        scalabilityTest(scaleLabelsInConstraintAfterNeverFlows, "labelsAfterNeverFlows");
    }

    // Scale single constraint with increasing amount of vertex selectors linearly.
    // Include the existing constraints for config as well.
    private static final Function<ScaleInput, ScaleOutput> scaleLabelsInConstraintAfterNeverFlows = (in) -> {
        Scaler scaler = new Scaler(in.inputDfd);

        DataFlowDiagramAndDictionary dfdWithLabels = scaler.scaleLabels(Math.max(in.scaleFactor * 3, 4));

        List<AnalysisConstraint> all = new ArrayList<>(in.inputConstraints);
        all.addAll(scaler.scaleConstraint(1, 0, 0, in.scaleFactor, in.scaleFactor, Math.max(in.scaleFactor * 3, 4)));
        return new ScaleOutput(dfdWithLabels, all);
    };

    @Disabled
    @Test
    public void testNumConstraints() throws Exception {
        scalabilityTest(scaleNumConstraints, "numConstraints");
    }

    // Scale number of constraints of form data X && !Y neverFlows vertex V && !Z
    // linearly. Include the existing constraints for config as well
    private static final Function<ScaleInput, ScaleOutput> scaleNumConstraints = (in) -> {
        Scaler scaler = new Scaler(in.inputDfd);
        // Scaler crashes with less than 4 labels, so use at least 4 in earlier case.
        DataFlowDiagramAndDictionary dfdWithLabels = scaler.scaleLabels(Math.max(in.scaleFactor * 3, 4));

        List<AnalysisConstraint> all = new ArrayList<>(in.inputConstraints);
        all.addAll(scaler.scaleConstraint(in.scaleFactor, 1, 1, 1, 1, Math.max(in.scaleFactor * 3, 4)));

        return new ScaleOutput(dfdWithLabels, all);
    };

    // Not used because too old
    public void testNodesAndFlows() throws Exception {
        scalabilityTest(scaleNodesAndFlows, "nodesAndFlows");
    }

    // Scale nodes and flows linearly.
    private static final Function<ScaleInput, ScaleOutput> scaleNodesAndFlows = (in) -> {
        Scaler scaler = new Scaler(in.inputDfd);
        return new ScaleOutput(scaler.scaleDFD(in.scaleFactor, in.scaleFactor), in.inputConstraints);
    };
}
