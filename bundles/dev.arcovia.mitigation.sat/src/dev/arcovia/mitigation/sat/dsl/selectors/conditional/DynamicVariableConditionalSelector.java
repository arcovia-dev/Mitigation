package dev.arcovia.mitigation.sat.dsl.selectors.conditional;

import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.sat.dsl.selectors.dynamic.DynamicDataSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VariableConditionalSelector;

import java.util.List;
import java.util.Map;

public class DynamicVariableConditionalSelector implements DynamicConditionalSelector {
    private final VariableConditionalSelector selector;

    public DynamicVariableConditionalSelector(VariableConditionalSelector selector) {
        this.selector = selector;
    }

    @Override
    public void addLiterals(BranchNode root, Map<String, DynamicDataSelector> dynamicSelectors, Map<String, List<String>> variables) {
        var dynamicDataSelector = dynamicSelectors.get(selector.getConstraintVariable().name());
        if (dynamicDataSelector.isInverted()) {
            throw new IllegalStateException("CharacteristicSelector must be positive");
        }
        dynamicDataSelector.addLiterals(root, variables, selector.isInverted());
    }
}