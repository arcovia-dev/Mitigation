package dev.arcovia.mitigation.smt.tests.evaluation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.Mitigation;
import dev.arcovia.mitigation.smt.operations.Operation;
import dev.arcovia.mitigation.smt.util.Util;

public class ModificationsTest {

    @Test
    public void testAllForModifications() throws Exception {
        List<ModificationsResult> results = new ArrayList<>();
        List<EvaluationSupport.Configuration> configs = EvaluationSupport.configurations();

        for (EvaluationSupport.Configuration cfg : configs) {
            DataFlowDiagramAndDictionary dfd = Util.loadDFD(cfg.model(), cfg.model() + "_0");

            System.out.println("Running " + cfg.model() + " with constraints " + cfg.variantId());
            List<Operation> suggestedActions = Mitigation.run(dfd, cfg.constraints(), null)
                    .repairOperations();

            int removableActions = 0;
            for (Operation action : suggestedActions) {
                DataFlowDiagramAndDictionary undone = action.undoOperation(dfd);
                if (Util.countViolations(undone, cfg.constraints()) <= 0) {
                    removableActions++;
                }
                // Ensure that operation gets added to the DFD again, so that they are actually tested in isolation
                dfd = action.doOperation(undone);
            }

            if (removableActions > 0) {
                System.out.println("An operation can be removed");
                System.exit(1);
            }

            results.add(new ModificationsResult(cfg.model(), cfg.variantId(), suggestedActions.size(), removableActions));
        }

        EvaluationSupport.writeJson(Path.of("testresults/results/modificationResults/data.json"), results);
    }

    private record ModificationsResult(String model, int constraints, int modificationCount, int removeableActions) {
    }
}
