package dev.arcovia.mitigation.sat.tests;

import com.google.common.collect.ImmutableMap;
import dev.arcovia.mitigation.sat.Label;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
import org.dataflowanalysis.dfd.datadictionary.SetAssignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class BaseTest {
    Map<Label, Integer> costs = ImmutableMap.<Label, Integer>builder()
            .put(new Label("Sensitivity", "Personal"), 10)
            .put(new Label("Location", "nonEU"), 5)
            .put(new Label("Encryption", "Encrypted"), 1)
            .put(new Label("Stereotype", "internal"), 3)
            .put(new Label("Stereotype", "local_logging"), 1)
            .build();


    protected Map<String, List<String>> getNodeBehavior(DataFlowDiagramAndDictionary dfd){
        var behaviors = dfd.dataDictionary()
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
        return nodeBehavior;
    }
    protected Map<String, List<String>> getNodeProperties(DataFlowDiagramAndDictionary dfd){
        var nodes = dfd.dataFlowDiagram()
                .getNodes();
        Map<String, List<String>> nodeProperties = new HashMap<>();
        for (var node : nodes) {
            var nodeProp = node.getProperties();
            List<String> nodePropStr = new ArrayList<>();
            for (var prop : nodeProp) {
                nodePropStr.add(prop.getEntityName());
            }
            nodeProperties.put(node.getEntityName(), nodePropStr);
        }
        return nodeProperties;
    }

}
