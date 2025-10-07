package dev.arcovia.mitigation.sat.dsl.selectors.conditional;

import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.sat.dsl.selectors.dynamic.DynamicDataSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VariableConditionalSelector;

import java.util.List;
import java.util.Map;

public class DynamicVariableConditionalSelector implements DynamicConditionalSelector {
    private final VariableConditionalSelector selector;

    /**
     * Constructs a {@link DynamicVariableConditionalSelector} wrapping the given {@link VariableConditionalSelector}.
     *
     * @param selector the {@link VariableConditionalSelector} to wrap
     */
    public DynamicVariableConditionalSelector(VariableConditionalSelector selector) {
        this.selector = selector;
    }

    /**
     * Adds literals to the root node using the dynamic selector corresponding to this selector's variable.
     * Throws an exception if the dynamic selector is inverted, as only positive selectors are allowed.
     *
     * @param root the {@link BranchNode} to which literals are added
     * @param dynamicSelectors a map of dynamic selectors by variable name
     * @param variables a map of variable names to their corresponding string values
     * @throws IllegalStateException if the dynamic selector is inverted
     */
    @Override
    public void addLiterals(BranchNode root, Map<String, DynamicDataSelector> dynamicSelectors, Map<String, List<String>> variables) {
        var dynamicDataSelector = dynamicSelectors.get(selector.getConstraintVariable().name());
        if (dynamicDataSelector.isInverted()) {
            throw new IllegalStateException("CharacteristicSelector must be positive");
        }
        dynamicDataSelector.addLiterals(root, variables, selector.isInverted());
    }
}