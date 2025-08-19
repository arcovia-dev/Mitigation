package dev.arcovia.mitigation.sat.cnf.selectors.constant;

import dev.arcovia.mitigation.sat.cnf.nodes.BranchNode;

public interface ConstantDataSelector {
    void addLiterals(BranchNode root, boolean hasOutgoingData, boolean hasIncomingData);
}