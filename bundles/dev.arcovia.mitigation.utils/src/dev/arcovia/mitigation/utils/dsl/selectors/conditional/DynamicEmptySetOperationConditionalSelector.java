package dev.arcovia.mitigation.utils.dsl.selectors.conditional;

import dev.arcovia.mitigation.utils.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.utils.dsl.selectors.dynamic.DynamicDataSelector;

import org.dataflowanalysis.analysis.dsl.selectors.EmptySetOperationConditionalSelector;
import org.dataflowanalysis.analysis.dsl.selectors.Intersection;

import java.util.List;
import java.util.Map;

/**
 * A dynamic implementation of an empty set operation conditional selector.
 * <p>
 * This class adapts an {@link EmptySetOperationConditionalSelector} into a runtime-resolvable form by delegating its
 * behavior to a corresponding {@link DynamicSetOperation}. It currently supports only intersection operations.
 * <p>
 * During initialization, the class inspects the provided selector’s set operation and wraps it in a suitable dynamic
 * counterpart (e.g., {@link DynamicIntersection}). If an unsupported operation is encountered, an
 * {@link IllegalArgumentException} is thrown.
 * <p>
 * At runtime, this class allows dynamic CNF literal generation based on provided {@link BranchNode} structures and
 * variable mappings.
 * @see EmptySetOperationConditionalSelector
 * @see DynamicSetOperation
 * @see DynamicIntersection
 * @see BranchNode
 */
public class DynamicEmptySetOperationConditionalSelector implements DynamicConditionalSelector {
    private final DynamicSetOperation operation;

    /**
     * Constructs a {@link DynamicEmptySetOperationConditionalSelector} based on the given selector. Supports only
     * intersection operations; throws an exception for unsupported operations.
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
     * @param root the {@link BranchNode} to which literals are added
     * @param dynamicSelectors a map of dynamic selectors by name
     * @param variables a map of variable names to their corresponding string values
     */
    @Override
    public void addLiterals(BranchNode root, Map<String, DynamicDataSelector> dynamicSelectors, Map<String, List<String>> variables) {
        operation.addLiterals(root, dynamicSelectors, variables);
    }
}