package dev.arcovia.mitigation.sat.cnf.nodes;

public abstract class BranchNode extends LogicNode {

    public BranchNode(LogicNodeDescriptor descriptor) {
        super(descriptor);
    }

    abstract public void addPredicate(LogicNode predicate);
}
