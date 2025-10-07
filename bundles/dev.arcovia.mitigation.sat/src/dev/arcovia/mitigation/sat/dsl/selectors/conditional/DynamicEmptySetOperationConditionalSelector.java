package dev.arcovia.mitigation.sat.dsl.selectors.conditional;

import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.sat.dsl.selectors.dynamic.DynamicDataSelector;
import org.dataflowanalysis.analysis.dsl.selectors.EmptySetOperationConditionalSelector;
import org.dataflowanalysis.analysis.dsl.selectors.Intersection;

import java.util.List;
import java.util.Map;

public class DynamicEmptySetOperationConditionalSelector implements DynamicConditionalSelector {
    private final DynamicSetOperation operation;

    /**
     * Constructs a {@link DynamicEmptySetOperationConditionalSelector} based on the given selector.
     * Supports only intersection operations; throws an exception for unsupported operations.
     *
     * @param selector the {@link EmptySetOperationConditionalSelector} to wrap dynamically
     * @throws IllegalArgumentException if the selector's operation is not supported
     */
    public DynamicEmptySetOperationConditionalSelector(EmptySetOperationConditionalSelector selector) {
        var setOperation = selector.getSetOperation();
        if (setOperation instanceof Intersection intersection) {
            this.operation = new DynamicIntersection(intersection);
            return;
        }
        throw new IllegalArgumentException("No valid operation found");
    }

    /**
     * Delegates the addition of literals to the underlying dynamic set operation.
     *
     * @param root the {@link BranchNode} to which literals are added
     * @param dynamicSelectors a map of dynamic selectors by name
     * @param variables a map of variable names to their corresponding string values
     */
    @Override
    public void addLiterals(BranchNode root, Map<String, DynamicDataSelector> dynamicSelectors, Map<String, List<String>> variables) {
        operation.addLiterals(root, dynamicSelectors, variables);
    }
}