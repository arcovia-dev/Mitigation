package dev.arcovia.mitigation.utils.dsl.selectors.constant;

import dev.arcovia.mitigation.utils.dsl.nodes.BranchNode;

/**
 * Represents a selector that adds constant data literals to a logical formula tree.
 * <p>
 * Implementations of this interface operate on fixed (constant) data characteristics and directly insert the
 * corresponding literals into the given {@link BranchNode}. This is typically used for constructing CNF formulas from
 * static data constraints.
 * @see BranchNode
 */
public interface ConstantDataSelector {
    void addLiterals(BranchNode root);
}