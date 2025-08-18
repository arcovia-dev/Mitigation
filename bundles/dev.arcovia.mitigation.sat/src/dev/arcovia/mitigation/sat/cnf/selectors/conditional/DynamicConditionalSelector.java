package dev.arcovia.mitigation.sat.cnf.selectors.conditional;

import dev.arcovia.mitigation.sat.cnf.nodes.BranchNode;
import dev.arcovia.mitigation.sat.cnf.selectors.dynamic.DynamicDataSelector;

import java.util.List;
import java.util.Map;

public interface DynamicConditionalSelector {
    void addLiterals(BranchNode root, Map <String, DynamicDataSelector> dynamicSelectors, Map<String, List<String>> variables, boolean hasOutgoingData,  boolean hasIncomingData);
}