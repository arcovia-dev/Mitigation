package dev.arcovia.mitigation.smt.tests.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.util.Util;

public class OutPinToAssTest {

    @Test
    public void testOutPinToAss() throws Exception {
        var tuhhModels = TuhhModels.getTuhhModels();
        for (var model : tuhhModels.keySet()) {
            if (!tuhhModels.get(model)
                    .contains(0))
                continue;
            for (int i : List.of(1, 2, 4, 5, 7, 8, 10, 11)) {
                if (!tuhhModels.get(model)
                        .contains(i)) {
                    continue;
                }
                DataFlowDiagramAndDictionary dfdAndDD = Util.loadDFD(model, model + "_" + i);
                DataFlowDiagram dfd = dfdAndDD.dataFlowDiagram();

                Map<Pin, List<AbstractAssignment>> outPinToAss = Util.outPinToAss(dfd.getNodes());

                for (Entry<Pin, List<AbstractAssignment>> entry : outPinToAss.entrySet()) {

                    Node node = dfd.getNodes()
                            .stream()
                            .filter(x -> x.getBehavior()
                                    .getOutPin()
                                    .contains(entry.getKey()))
                            .findFirst()
                            .get();

                    for (AbstractAssignment assignment : entry.getValue()) {
                        assertEquals(assignment.getOutputPin(), entry.getKey());
                        assertTrue(node.getBehavior()
                                .getAssignment()
                                .contains(assignment));
                    }
                }

            }
        }

    }

}
