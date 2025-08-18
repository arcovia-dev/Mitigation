package dev.arcovia.mitigation.sat.cnf.selectors.conditional;

import dev.arcovia.mitigation.sat.IncomingDataLabel;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.sat.OutgoingDataLabel;
import dev.arcovia.mitigation.sat.cnf.nodes.BranchNode;
import dev.arcovia.mitigation.sat.cnf.nodes.ConjunctionNode;
import dev.arcovia.mitigation.sat.cnf.nodes.DisjunctionNode;
import dev.arcovia.mitigation.sat.cnf.nodes.LiteralNode;
import dev.arcovia.mitigation.sat.cnf.selectors.dynamic.DynamicDataCharacteristicSelector;
import dev.arcovia.mitigation.sat.cnf.selectors.dynamic.DynamicDataSelector;
import dev.arcovia.mitigation.sat.cnf.selectors.dynamic.DynamicVertexCharacteristicSelector;
import org.dataflowanalysis.analysis.dsl.selectors.Intersection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class DynamicIntersection implements DynamicSetOperation {
    private final Intersection intersection;

    public DynamicIntersection(Intersection intersection) {
        this.intersection = intersection;
    }

    @Override
    public void addLiterals(BranchNode root, Map<String, DynamicDataSelector> dynamicSelectors, Map<String, List<String>> variables, boolean hasOutgoingData, boolean hasIncomingData) {
        var firstVariableReference = intersection.getFirstVariable().name();
        var secondVariableReference = intersection.getSecondVariable().name();

        var variableSet = Stream.concat(
                        variables.get(firstVariableReference).stream(),
                        variables.get(secondVariableReference).stream())
                .distinct().toList();

        if (variableSet.isEmpty()) { throw new IllegalStateException("No Intersection found."); }

        Map<String, List<String>> conditionalVariables = new HashMap<>();
        conditionalVariables.put(firstVariableReference, variableSet);
        conditionalVariables.put(secondVariableReference, variableSet);

        var firstDynamicSelector = dynamicSelectors.get(firstVariableReference);
        var secondDynamicSelector = dynamicSelectors.get(secondVariableReference);

        if (firstDynamicSelector instanceof DynamicDataCharacteristicSelector dataSelector &&
                secondDynamicSelector instanceof DynamicVertexCharacteristicSelector vertexSelector
        ) {
            var dataLabels = dataSelector.getLabels(conditionalVariables);
            var nodeLabels = vertexSelector.getLabels(conditionalVariables);

            addIntersectionLiterals(root, dataLabels, nodeLabels, hasOutgoingData, hasIncomingData);
            return;
        }

        if (firstDynamicSelector instanceof DynamicVertexCharacteristicSelector vertexSelector &&
                secondDynamicSelector instanceof DynamicDataCharacteristicSelector dataSelector
        ) {
            var dataLabels = dataSelector.getLabels(conditionalVariables);
            var nodeLabels = vertexSelector.getLabels(conditionalVariables);

            addIntersectionLiterals(root, dataLabels, nodeLabels, hasOutgoingData, hasIncomingData);
            return;
        }

        throw new IllegalArgumentException("Unexpected dynamic selectors" + firstDynamicSelector + secondDynamicSelector);
    }

    private void addIntersectionLiterals(BranchNode root, List<Label> dataLabels, List<Label> nodeLabels, boolean hasOutgoingData, boolean hasIncomingData) {
        // (not data1 OR not node1) AND (not data2 OR not node2) AND
        // (not node1 OR (not data_in1 and not data_out1) AND
        for (int i = 0; i < dataLabels.size(); i++) {

            var node = new DisjunctionNode();
            root.addPredicate(node);

            var dataNode = new ConjunctionNode();
            node.addPredicate(dataNode);
            node.addPredicate(new LiteralNode(true, new NodeLabel(nodeLabels.get(i))));

            if (hasIncomingData) {
                dataNode.addPredicate(new LiteralNode(true, new IncomingDataLabel(dataLabels.get(i))));
            }

            if (hasOutgoingData) {
                dataNode.addPredicate(new LiteralNode(true, new OutgoingDataLabel(dataLabels.get(i))));
            }
        }
    }
}