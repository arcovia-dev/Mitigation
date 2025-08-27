package dev.arcovia.mitigation.sat.dsl.selectors.conditional;

import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.sat.dsl.selectors.dynamic.DynamicDataSelector;

import java.util.List;
import java.util.Map;

public interface DynamicSetOperation {
    void addLiterals(BranchNode root, Map<String, DynamicDataSelector> dynamicSelectors, Map<String, List<String>> variables, boolean hasOutgoingData, boolean hasIncomingData);
}
