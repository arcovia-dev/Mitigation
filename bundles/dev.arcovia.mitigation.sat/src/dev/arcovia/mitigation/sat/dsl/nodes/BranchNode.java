package dev.arcovia.mitigation.sat.dsl.nodes;

public interface BranchNode extends LogicNode {
    void addPredicate(LogicNode predicate);
}
