package dev.arcovia.mitigation.utils.dsl.selectors.conditional;

import dev.arcovia.mitigation.sat.IncomingDataLabel;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.utils.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.utils.dsl.nodes.ConjunctionNode;
import dev.arcovia.mitigation.utils.dsl.nodes.DisjunctionNode;
import dev.arcovia.mitigation.utils.dsl.nodes.LiteralNode;
import dev.arcovia.mitigation.utils.dsl.selectors.dynamic.DynamicDataCharacteristicSelector;
import dev.arcovia.mitigation.utils.dsl.selectors.dynamic.DynamicDataSelector;
import dev.arcovia.mitigation.utils.dsl.selectors.dynamic.DynamicVertexCharacteristicSelector;

import org.dataflowanalysis.analysis.dsl.selectors.Intersection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Performs a dynamic intersection operation between two variable sets in a graph-based data model. Combines labels from
 * {@link DynamicDataCharacteristicSelector} and {@link DynamicVertexCharacteristicSelector} based on shared variable
 * values, adding corresponding literal predicates to a {@link BranchNode}. Used to dynamically construct logical
 * conditions involving intersecting data and vertex characteristics.
 */
public class DynamicIntersection implements DynamicSetOperation {
    private final Intersection intersection;

    /**
     * Constructs a {@link DynamicIntersection} wrapping the given {@link Intersection} operation.
     * @param intersection the {@link Intersection} to wrap
     */
    public DynamicIntersection(Intersection intersection) {
        this.intersection = intersection;
    }

    /**
     * Adds literals to the given root node based on the intersection of variables from dynamic selectors. Handles
     * combinations of {@link DynamicDataCharacteristicSelector} and {@link DynamicVertexCharacteristicSelector}. Throws an
     * exception if no intersection is found or if selectors are of unexpected types.
     * @param root the {@link BranchNode} to which literals are added
     * @param dynamicSelectors a map of dynamic selectors by variable name
     * @param variables a map of variable names to their corresponding string values
     * @throws IllegalStateException if the intersection of variables is empty
     * @throws IllegalArgumentException if dynamic selectors are of unexpected types
     */
    @Override
    public void addLiterals(BranchNode root, Map<String, DynamicDataSelector> dynamicSelectors, Map<String, List<String>> variables) {
        var firstVariableReference = intersection.getFirstVariable()
                .name();
        var secondVariableReference = intersection.getSecondVariable()
                .name();

        var variableSet = Stream.concat(variables.get(firstVariableReference)
                .stream(),
                variables.get(secondVariableReference)
                        .stream())
                .distinct()
                .toList();

        if (variableSet.isEmpty()) {
            throw new IllegalStateException("No Intersection found.");
        }

        Map<String, List<String>> conditionalVariables = new HashMap<>();
        conditionalVariables.put(firstVariableReference, variableSet);
        conditionalVariables.put(secondVariableReference, variableSet);

        var firstDynamicSelector = dynamicSelectors.get(firstVariableReference);
        var secondDynamicSelector = dynamicSelectors.get(secondVariableReference);

        if (firstDynamicSelector instanceof DynamicDataCharacteristicSelector dataSelector
                && secondDynamicSelector instanceof DynamicVertexCharacteristicSelector vertexSelector) {
            var dataLabels = dataSelector.getLabels(conditionalVariables);
            var nodeLabels = vertexSelector.getLabels(conditionalVariables);

            addIntersectionLiterals(root, dataLabels, nodeLabels);
            return;
        }

        if (firstDynamicSelector instanceof DynamicVertexCharacteristicSelector vertexSelector
                && secondDynamicSelector instanceof DynamicDataCharacteristicSelector dataSelector) {
            var dataLabels = dataSelector.getLabels(conditionalVariables);
            var nodeLabels = vertexSelector.getLabels(conditionalVariables);

            addIntersectionLiterals(root, dataLabels, nodeLabels);
            return;
        }

        throw new IllegalArgumentException("Unexpected dynamic selectors: " + firstDynamicSelector + secondDynamicSelector);
    }

    private void addIntersectionLiterals(BranchNode root, List<Label> dataLabels, List<Label> nodeLabels) {
        var rootNode = new DisjunctionNode();
        root.addPredicate(rootNode);

        for (int i = 0; i < dataLabels.size(); i++) {
            var node = new ConjunctionNode();
            rootNode.addPredicate(node);

            node.addPredicate(new LiteralNode(true, new NodeLabel(nodeLabels.get(i))));
            node.addPredicate(new LiteralNode(true, new IncomingDataLabel(dataLabels.get(i))));
        }
    }
}