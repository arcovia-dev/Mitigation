package dev.arcovia.mitigation.sat.tests;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
import org.dataflowanalysis.dfd.datadictionary.SetAssignment;
import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import com.google.common.collect.ImmutableMap;

import dev.arcovia.mitigation.sat.Constraint;
import dev.arcovia.mitigation.sat.IncomingDataLabel;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.Literal;
import dev.arcovia.mitigation.sat.Mechanic;
import dev.arcovia.mitigation.sat.NodeLabel;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SatTest {

    public final String MIN_SAT = "models/minsat.json";

    @Test
    public void automaticTest() throws ContradictionException, TimeoutException, IOException {
        var dfdConverter = new DataFlowDiagramConverter();

        // (personal AND nonEU) => encrypted
        var constraint = new Constraint(List.of(new Literal(false, new IncomingDataLabel(new Label("Sensitivity", "Personal"))),
                new Literal(false, new NodeLabel(new Label("Location", "nonEU"))),
                new Literal(true, new IncomingDataLabel(new Label("Encryption", "Encrypted")))));
        var nodeConstraint = new Constraint(List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
                new Literal(true, new NodeLabel(new Label("Stereotype", "local_logging")))));
        var constraints = List.of(constraint, constraint);

        Map<Label, Integer> costs = ImmutableMap.<Label, Integer>builder()
                .put(new Label("Sensitivity", "Personal"), 10)
                .put(new Label("Location", "nonEU"), 5)
                .put(new Label("Encryption", "Encrypted"), 1)
                .put(new Label("Stereotype", "internal"), 1)
                .put(new Label("Stereotype", "local_logging"), 1)
                .build();

        var repairedDfdCosts = new Mechanic(MIN_SAT, constraints, costs).repair();
        checkIfConsistent(repairedDfdCosts);
        dfdConverter.storeWeb(dfdConverter.dfdToWeb(repairedDfdCosts), "repaired.json");

        var repairedDfdMinimal = new Mechanic(MIN_SAT, constraints).repair();
        checkIfConsistent(repairedDfdMinimal);
    }

    private void checkIfConsistent(DataFlowDiagramAndDictionary repairedDfd) {
        var nodes = repairedDfd.dataFlowDiagram()
                .getNodes();
        var behaviors = repairedDfd.dataDictionary()
                .getBehavior();

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
                } else if (assignment instanceof SetAssignment cast) {
                    var labels = cast.getOutputLabels();
                    for (var label : labels) {
                        nodeBehStr.add(label.getEntityName());
                    }
                } else if (assignment instanceof ForwardingAssignment cast) {
                    for (var pin : cast.getInputPins())
                        nodeBehStr.add("Forwarding: " + pin.getEntityName());
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
        Map<String, List<String>> expectedNodeBehavior = Map.of("process", List.of("Forwarding: process_in_user", "Encrypted"), "user",
                List.of("Personal"), "db", List.of());
        //"internal", "local_logging"
        Map<String, List<String>> expectedNodeProperties = Map.of("process", List.of(), "user", List.of(), "db",
                List.of("nonEU"));

        assertEquals(expectedNodeBehavior, nodeBehavior);
        assertEquals(expectedNodeProperties, nodeProperties);

    }

}
