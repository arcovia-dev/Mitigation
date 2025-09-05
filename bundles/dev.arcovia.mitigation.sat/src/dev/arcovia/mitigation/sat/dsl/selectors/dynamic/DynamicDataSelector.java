package dev.arcovia.mitigation.sat.dsl.selectors.dynamic;

import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;

import java.util.List;
import java.util.Map;

public interface DynamicDataSelector {
    List<Label> getLabels(Map<String, List<String>> variables);
    void addLiterals(BranchNode root, Map<String, List<String>> variables, boolean inverted);
    boolean isInverted();
}