package dev.arcovia.mitigation.smt.tests.evaluation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.Mitigation;
import dev.arcovia.mitigation.smt.utils.ParsingUtils;

public class ViolationsTest {

    @Test
    public void testAllForViolations() throws Exception {
        List<ViolationsResult> results = new ArrayList<>();
        List<EvaluationSupport.Configuration> configs = EvaluationSupport.configurations();

        int totalViolations = 0;

        for (EvaluationSupport.Configuration cfg : configs) {
            DataFlowDiagramAndDictionary dfd = ParsingUtils.loadDFD(cfg.model(), cfg.model() + "_0");

            int before = ParsingUtils.countViolations(dfd, cfg.constraints());
            totalViolations += before;

            System.out.println("Running " + cfg.model() + " with constraints " + cfg.variantId());
            DataFlowDiagramAndDictionary repaired = Mitigation.run(dfd, cfg.constraints(), null)
                    .repairedDFD();

            int after = ParsingUtils.countViolations(repaired, cfg.constraints());
            if (after > 0) {
                throw new IllegalStateException("Violations after repair still present for " + cfg.model() + "_" + cfg.variantId());
            }

            results.add(new ViolationsResult(cfg.model(), cfg.variantId(), before, after));
        }

        EvaluationSupport.writeJson(Path.of("testresults/results/violationResults/data.json"), results);
        System.out.println("Total violations " + totalViolations);
    }

    private record ViolationsResult(String model, int constraints, int violationsBefore, int violationsAfter) {
    }
}
