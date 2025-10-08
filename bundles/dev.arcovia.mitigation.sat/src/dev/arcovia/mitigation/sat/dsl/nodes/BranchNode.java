package dev.arcovia.mitigation.sat.dsl.nodes;

/**
 * Represents a logical branch node in a formula tree, such as a conjunction or disjunction node.
 * <p>
 * A {@link BranchNode} contains one or more {@link LogicNode} predicates that define
 * the structure of the logical expression. Implementations must define how predicates
 * are added and how CNF clauses are collected.
 */
public interface BranchNode extends LogicNode {
    void addPredicate(LogicNode predicate);
}
