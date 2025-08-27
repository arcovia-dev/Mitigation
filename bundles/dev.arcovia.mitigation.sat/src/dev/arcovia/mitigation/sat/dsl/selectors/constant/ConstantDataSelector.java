package dev.arcovia.mitigation.sat.dsl.selectors.constant;

import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;

public interface ConstantDataSelector {
    void addLiterals(BranchNode root, boolean hasOutgoingData, boolean hasIncomingData);
}