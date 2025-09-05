package dev.arcovia.mitigation.sat.dsl.selectors.conditional;

import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.sat.dsl.selectors.dynamic.DynamicDataSelector;
import org.dataflowanalysis.analysis.dsl.selectors.EmptySetOperationConditionalSelector;
import org.dataflowanalysis.analysis.dsl.selectors.Intersection;

import java.util.List;
import java.util.Map;

public class DynamicEmptySetOperationConditionalSelector implements DynamicConditionalSelector {
    private final DynamicSetOperation operation;

    public DynamicEmptySetOperationConditionalSelector(EmptySetOperationConditionalSelector selector) {
        var setOperation = selector.getSetOperation();
        if (setOperation instanceof Intersection intersection) {
            this.operation = new DynamicIntersection(intersection);
            return;
        }
        throw new IllegalArgumentException("No valid operation found");
    }

    @Override
    public void addLiterals(BranchNode root, Map<String, DynamicDataSelector> dynamicSelectors, Map<String, List<String>> variables) {
        operation.addLiterals(root, dynamicSelectors, variables);
    }
}