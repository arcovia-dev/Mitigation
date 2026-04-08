package dev.arcovia.mitigation.utils.dsl.selectors.conditional;

import dev.arcovia.mitigation.utils.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.utils.dsl.selectors.dynamic.DynamicDataSelector;

import org.dataflowanalysis.analysis.dsl.selectors.VariableConditionalSelector;

import java.util.List;
import java.util.Map;

/**
 * A dynamic implementation of a conditional selector that integrates a {@link VariableConditionalSelector} with dynamic
 * data selectors to construct CNF literals at runtime.
 * <p>
 * This class acts as a bridge between variable-based conditional selectors and dynamically resolved data selectors,
 * allowing flexible formula construction based on runtime variable mappings.
 * <p>
 * It ensures that only non-inverted dynamic selectors are used, throwing an exception if an inverted selector is
 * encountered.
 * @see VariableConditionalSelector
 * @see DynamicDataSelector
 * @see BranchNode
 */
public class DynamicVariableConditionalSelector implements DynamicConditionalSelector {
    private final VariableConditionalSelector selector;

    /**
     * Constructs a {@link DynamicVariableConditionalSelector} wrapping the given {@link VariableConditionalSelector}.
     * @param selector the {@link VariableConditionalSelector} to wrap
     */
    public DynamicVariableConditionalSelector(VariableConditionalSelector selector) {
        this.selector = selector;
    }

    /**
     * Adds literals to the root node using the dynamic selector corresponding to this selector's variable. Throws an
     * exception if the dynamic selector is inverted, as only positive selectors are allowed.
     * @param root the {@link BranchNode} to which literals are added
     * @param dynamicSelectors a map of dynamic selectors by variable name
     * @param variables a map of variable names to their corresponding string values
     * @throws IllegalStateException if the dynamic selector is inverted
     */
    @Override
    public void addLiterals(BranchNode root, Map<String, DynamicDataSelector> dynamicSelectors, Map<String, List<String>> variables) {
        var dynamicDataSelector = dynamicSelectors.get(selector.getConstraintVariable()
                .name());
        if (dynamicDataSelector.isInverted()) {
            throw new IllegalStateException("CharacteristicSelector must be positive");
        }
        dynamicDataSelector.addLiterals(root, variables, selector.isInverted());
    }
}