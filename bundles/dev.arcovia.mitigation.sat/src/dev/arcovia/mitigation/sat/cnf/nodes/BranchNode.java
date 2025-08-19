package dev.arcovia.mitigation.sat.cnf.nodes;

public abstract class BranchNode extends LogicNode {
    abstract public void addPredicate(LogicNode predicate);
}
