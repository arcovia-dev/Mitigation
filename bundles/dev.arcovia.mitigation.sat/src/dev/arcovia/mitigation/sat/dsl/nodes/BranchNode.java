package dev.arcovia.mitigation.sat.dsl.nodes;

public abstract class BranchNode extends LogicNode {
    abstract public void addPredicate(LogicNode predicate);
}
