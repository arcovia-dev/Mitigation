package dev.arcovia.mitigation.smt.tests.evaluation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.Mitigation;
import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.config.ConfigBuilder;
import dev.arcovia.mitigation.smt.utils.ParsingUtils;

public class ComplexityReductionTest {

    private static final int RUNS_PER_CONFIGURATION = 100;

    @Disabled
    @Test
    public void testAllForRuntime() throws Exception {
        List<RuntimeResult> runtimeResults = new ArrayList<>();
        List<EvaluationSupport.Configuration> configs = EvaluationSupport.configurations();

        for (EvaluationSupport.Configuration cfg : configs) {
            Config config = new ConfigBuilder().findExpressionTreeSize(true)
                    .build();

            long dagSizeAfter = Mitigation.run(ParsingUtils.loadDFD(cfg.model(), cfg.model() + "_0"), cfg.constraints(), config)
                    .expressionTreeSize()
                    .orElseThrow();

            List<Long> runtimesOn = measureOnRuntimes(cfg.model(), cfg.constraints(), RUNS_PER_CONFIGURATION);
            List<Long> runtimesOff = measureOffRuntimes(cfg.model(), cfg.constraints(), RUNS_PER_CONFIGURATION);

            runtimeResults.add(new RuntimeResult(runtimesOn, runtimesOff));
        }

        EvaluationSupport.writeJson(Path.of("testresults/results/runtimeResults/complexityReduction/data.json"), runtimeResults);
    }

    private static List<Long> measureOnRuntimes(String model, List<AnalysisConstraint> constraints, int runs) throws Exception {

        List<Long> runtimes = new ArrayList<>(runs);
        for (int i = 0; i < runs; i++) {
            DataFlowDiagramAndDictionary dfd = ParsingUtils.loadDFD(model, model + "_0");
            long before = System.currentTimeMillis();
            Mitigation.run(dfd, constraints, null);
            long after = System.currentTimeMillis();
            runtimes.add(after - before);
        }
        return runtimes;
    }

    private static List<Long> measureOffRuntimes(String model, List<AnalysisConstraint> constraints, int runs) throws Exception {

        List<Long> runtimesOff = new ArrayList<>(runs);
        for (int i = 0; i < runs; i++) {
            DataFlowDiagramAndDictionary dfd = ParsingUtils.loadDFD(model, model + "_0");
            Config config = new ConfigBuilder().onlyRelevantModifications(false)
                    .build();
            long before = System.currentTimeMillis();
            Mitigation.run(dfd, constraints, config);
            long after = System.currentTimeMillis();
            runtimesOff.add(after - before);
        }
        return runtimesOff;

    }

    private record RuntimeResult(List<Long> runtimesOn, List<Long> runtimesOff) {
    }
}
