package dev.arcovia.mitigation.sat.dsl.selectors.conditional;

import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.sat.dsl.selectors.dynamic.DynamicDataSelector;

import java.util.List;
import java.util.Map;

/**
 * Represents a selector for adding literals to a CNF formula tree based on a condition for given variables.
 * <p>
 * Implementations of this interface define how to construct logical conditions dynamically
 * based on variable mappings and corresponding data selectors.
 *
 * @see BranchNode
 * @see DynamicDataSelector
 */
public interface DynamicConditionalSelector {
    void addLiterals(BranchNode root, Map <String, DynamicDataSelector> dynamicSelectors, Map<String, List<String>> variables);
}