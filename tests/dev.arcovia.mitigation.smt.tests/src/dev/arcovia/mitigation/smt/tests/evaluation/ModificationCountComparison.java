package dev.arcovia.mitigation.smt.tests.evaluation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.sat.ModelCostCalculator;
import dev.arcovia.mitigation.smt.Mitigation;
import dev.arcovia.mitigation.smt.SolvingResult;
import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.config.ConfigBuilder;
import dev.arcovia.mitigation.smt.operations.NodeLabelRemoveOperation;
import dev.arcovia.mitigation.smt.operations.Operation;
import dev.arcovia.mitigation.smt.operations.UnsetAssignmentOperation;
import dev.arcovia.mitigation.smt.utils.ParsingUtils;

public class ModificationCountComparison {

    @Test
    void efficiencyTestOnlyAdd() throws Exception {
        Config smtConfig = new ConfigBuilder().removeDataLabels(false)
                .removeNodeLabels(false)
                .build();

        runComparison(Path.of("testresults/results/modificationResults/comparison/add/data.json"), smtConfig, true, true);
    }

    @Test
    void efficiencyTest() throws Exception {
        runComparison(Path.of("testresults/results/modificationResults/comparison/all/data.json"), null, true, false);
    }

    private void runComparison(Path outFile, Config smtConfig, boolean useNewModelCost, boolean checkForRemoveOperations) throws Exception {
        List<ComparisonResult> results = new ArrayList<>();
        List<EvaluationSupport.Configuration> configs = EvaluationSupport.configurations();

        for (EvaluationSupport.Configuration cfg : configs) {
            System.out.println("Comparing " + cfg.model() + "_" + cfg.variantId());

            DataFlowDiagramAndDictionary baseDfd = ParsingUtils.loadDFD(cfg.model(), cfg.model() + "_0");

            DataFlowDiagramAndDictionary inputDfd = ParsingUtils.loadDFD(cfg.model(), cfg.model() + "_0");
            SatHelper.RepairResult satRepair = SatHelper.runRepair(inputDfd, false, cfg.constraints(), SatHelper.MIN_COSTS);

            DataFlowDiagramAndDictionary repairedDfd = satRepair.repairedDfd();

            int baseCost;
            int satCost;

            var satConstraints = SatHelper.toSatConstraints(cfg.constraints());

            if (useNewModelCost) {
                baseCost = new ModelCostCalculator(baseDfd, satConstraints, SatHelper.MIN_COSTS).calculateCostWithoutForwarding();
                satCost = new ModelCostCalculator(repairedDfd, satConstraints, SatHelper.MIN_COSTS).calculateCostWithoutForwarding();
            } else {
                baseCost = new ModelCostCalculator(baseDfd, satConstraints, SatHelper.MIN_COSTS).calculateCost();
                satCost = new ModelCostCalculator(repairedDfd, satConstraints, SatHelper.MIN_COSTS).calculateCost();
            }

            satCost -= baseCost;

            SolvingResult solving = Mitigation.run(ParsingUtils.loadDFD(cfg.model(), cfg.model() + "_0"), cfg.constraints(), smtConfig);
            int smtCost = solving.repairCost();

            if (smtCost > satCost) {
                throw new Exception("SMT cost greater than SAT Cost. This should not be the case");
            }

            if (checkForRemoveOperations) {
                for (Operation op : solving.repairOperations()) {
                    if (op instanceof UnsetAssignmentOperation || op instanceof NodeLabelRemoveOperation) {
                        throw new IllegalStateException("Unexpected operation type in SMT result: " + op.getClass());
                    }
                }
            }

            if (ParsingUtils.countViolations(solving.repairedDFD(), cfg.constraints()) > 0) {
                throw new IllegalStateException("Violations present after SMT repair for " + cfg.model() + "_" + cfg.variantId());
            }

            results.add(new ComparisonResult(cfg.model(), cfg.variantId(), satCost, smtCost));
        }

        EvaluationSupport.writeJson(outFile, results);
    }

    private record ComparisonResult(String model, int constraints, int satCost, int smtCost) {
    }
}
