package dev.arcovia.mitigation.sat.cnf;

public abstract class BranchNode extends LogicNode {

    public BranchNode(LogicNodeDescriptor descriptor) {
        super(descriptor);
    }

    abstract void addPredicate(LogicNode predicate);
}
