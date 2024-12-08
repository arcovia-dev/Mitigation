package dev.abunai.confidentiality.mitigation.tests.sat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.dataflowanalysis.converter.WebEditorConverter;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import dev.abunai.confidentiality.mitigation.sat.Constraint;
import dev.abunai.confidentiality.mitigation.sat.Label;
import dev.abunai.confidentiality.mitigation.sat.Mechanic;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SatTest {

    public final String MIN_SAT = "models/minsat.json";

    @Test
    public void automaticTest() throws ContradictionException, TimeoutException, IOException {
        var webConverter = new WebEditorConverter();
        var dfdConverter = new DataFlowDiagramConverter();
        var dfd = webConverter.webToDfd(MIN_SAT);

        // (personal AND nonEU) => encrypted
        var constraints = List.of(List.of(new Constraint(false, "Data", new Label("Sensitivity", "Personal")),
                new Constraint(false, "Node", new Label("Location", "nonEU")), new Constraint(true, "Data", new Label("Encryption", "Encrypted"))),
                List.of(new Constraint(false, "Data", new Label("Sensitivity", "Personal")),
                        new Constraint(false, "Node", new Label("Location", "nonEU")),
                        new Constraint(true, "Data", new Label("Encryption", "Encrypted"))));

        var repairedDfd = new Mechanic().repair(dfd, constraints);

        checkIfConsistent(repairedDfd);

        dfdConverter.storeWeb(dfdConverter.dfdToWeb(repairedDfd), "repaired.json");
    }

    private void checkIfConsistent(DataFlowDiagramAndDictionary repairedDfd) {
        var nodes = repairedDfd.dataFlowDiagram()
                .getNodes();
        var behaviors = repairedDfd.dataDictionary()
                .getBehaviour();

        Map<String, List<String>> nodeBehavior = new HashMap<>();
        for (var behavior : behaviors) {
            List<String> nodeBehStr = new ArrayList<>();
            var assignments = behavior.getAssignment();
            for (var assignment : assignments) {
                if (assignment instanceof Assignment cast) {
                    var labels = cast.getOutputLabels();
                    for (var label : labels) {
                        nodeBehStr.add(label.getEntityName());
                    }
                }
                //todo get forwarded labels
                else if (assignment instanceof ForwardingAssignment cast) {
                
                }
            }
            nodeBehavior.put(behavior.getEntityName(), nodeBehStr);

        }

        Map<String, List<String>> nodeProperties = new HashMap<>();
        for (var node : nodes) {
            var nodeProp = node.getProperties();
            List<String> nodePropStr = new ArrayList<>();
            for (var prop : nodeProp) {
                nodePropStr.add(prop.getEntityName());
            }
            nodeProperties.put(node.getEntityName(), nodePropStr);
        }
        //Map<String, List<String>> expectedNodeBehavior = Map.of("process", List.of("Personal", "Encrypted"), "user", List.of("Personal"), "db",List.of());
        Map<String, List<String>> expectedNodeBehavior = Map.of("process", List.of("Encrypted"), "user", List.of("Personal"), "db",
                List.of());

        Map<String, List<String>> expectedNodeProperties = Map.of("process", List.of(), "user", List.of(), "db", List.of("nonEU"));

        assertEquals(expectedNodeBehavior, nodeBehavior);
        assertEquals(expectedNodeProperties, nodeProperties);

    }

}
