package dev.arcovia.mitigation.smt.tests.evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.Mitigation;
import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.config.ConfigBuilder;
import dev.arcovia.mitigation.smt.tests.evaluation.SatHelper.RepairResult;
import dev.arcovia.mitigation.smt.util.Util;

public class RuntimeComparisonTest {

    private static final int RUNS_PER_CONFIGURATION = 100;

    @Test
    public void testAllForRuntime() throws Exception {
        List<RuntimeResult> runtimeResults = new ArrayList<>();
        List<EvaluationSupport.Configuration> configs = EvaluationSupport.configurations();

        for (EvaluationSupport.Configuration cfg : configs) {
            Config config = new ConfigBuilder().findExpressionTreeSize(true)
                    .build();

            long dagSizeAfter = Mitigation.run(Util.loadDFD(cfg.model(), cfg.model() + "_0"), cfg.constraints(), config)
                    .expressionTreeSize()
                    .orElseThrow();

            List<Long> smtRuntimes = measureSmtRuntimes(cfg.model(), cfg.constraints(), RUNS_PER_CONFIGURATION);
            List<Long> satRuntimes = measureSatRuntimes(cfg.model(), cfg.constraints(), RUNS_PER_CONFIGURATION);

            int clauseCount = extractClauseCount("testresults/aName.cnf");

            runtimeResults.add(new RuntimeResult(dagSizeAfter, clauseCount, smtRuntimes, satRuntimes));
        }

        EvaluationSupport.writeJson(Path.of("testresults/results/runtimeResults/comparison/data.json"), runtimeResults);
    }

    private static List<Long> measureSmtRuntimes(String model, List<AnalysisConstraint> constraints, int runs) throws Exception {

        List<Long> runtimes = new ArrayList<>(runs);
        for (int i = 0; i < runs; i++) {
            DataFlowDiagramAndDictionary dfd = Util.loadDFD(model, model + "_0");
            long before = System.currentTimeMillis();
            Mitigation.run(dfd, constraints, null);
            long after = System.currentTimeMillis();
            runtimes.add(after - before);
        }
        return runtimes;
    }

    private static List<Long> measureSatRuntimes(String model, List<AnalysisConstraint> constraints, int runs) throws Exception {

        List<Long> runtimes = new ArrayList<>(runs);
        for (int i = 0; i < runs; i++) {
            DataFlowDiagramAndDictionary dfd = Util.loadDFD(model, model + "_0");
            RepairResult rr = SatHelper.runRepair(dfd, false, constraints, SatHelper.MIN_COSTS);
            runtimes.add(rr.runtimeInMilliseconds());
        }
        return runtimes;
    }

    private record RuntimeResult(long dagSize, int clauseCount, List<Long> runtimesSMT, List<Long> runtimesSAT) {
    }

    private static int extractClauseCount(String filePath) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String firstLine = reader.readLine();
            if (firstLine != null && firstLine.startsWith("p cnf")) {
                String[] parts = firstLine.trim()
                        .split("\\s+");
                if (parts.length == 4)
                    return Integer.parseInt(parts[3]);
            }
        }
        throw new IllegalArgumentException("First line is not in the expected 'p cnf <vars> <clauses>' format.");
    }
}
