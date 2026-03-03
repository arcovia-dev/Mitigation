package dev.arcovia.mitigation.smt.tests.evaluation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.Mitigation;
import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.config.ConfigBuilder;
import dev.arcovia.mitigation.smt.util.Util;

public class RuntimeTest {

    @Test
    public void testAllForRuntime() throws Exception {
        List<RuntimeResult> runtimeResults = new ArrayList<>();
        List<EvaluationSupport.Configuration> configs = EvaluationSupport.configurations();
        int totalRuns = 100;

        for (EvaluationSupport.Configuration cfg : configs) {
            Config config = new ConfigBuilder().findExpressionTreeSize(true)
                    .build();

            long dagSizeAfter = Mitigation.run(Util.loadDFD(cfg.model(), cfg.model() + "_0"), cfg.constraints(), config)
                    .expressionTreeSize()
                    .orElseThrow();

            List<Long> runtimes = new ArrayList<>(totalRuns);
            for (int j = 0; j < totalRuns; j++) {
                System.out.println("Running " + cfg.model() + " with constraints " + cfg.variantId());
                DataFlowDiagramAndDictionary dfd = Util.loadDFD(cfg.model(), cfg.model() + "_0");
                long before = System.currentTimeMillis();
                Mitigation.run(dfd, cfg.constraints(), null);
                long after = System.currentTimeMillis();
                runtimes.add(after - before);
            }

            runtimeResults.add(new RuntimeResult(dagSizeAfter, runtimes));
        }

        EvaluationSupport.writeJson(Path.of("testresults/results/runtimeResults/100runs/data.json"), runtimeResults);
    }

    private record RuntimeResult(long dagSize, List<Long> averageRuntime) {
    }
}
