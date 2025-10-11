package dev.arcovia.mitigation.sat.dsl.selectors.conditional;

import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.sat.dsl.selectors.dynamic.DynamicDataSelector;

import java.util.List;
import java.util.Map;

/**
 * Represents a dynamic set operation that constructs and adds CNF literals to a logical formula tree.
 * <p>
 * Implementations of this interface perform set-based logical operations (such as unions or intersections) on dynamic
 * data selectors and append the corresponding literals to a given {@link BranchNode}.
 * <p>
 * These operations are typically used in the dynamic translation of data flow constraints into CNF form.
 * @see BranchNode
 * @see DynamicDataSelector
 */
public interface DynamicSetOperation {
    void addLiterals(BranchNode root, Map<String, DynamicDataSelector> dynamicSelectors, Map<String, List<String>> variables);
}
