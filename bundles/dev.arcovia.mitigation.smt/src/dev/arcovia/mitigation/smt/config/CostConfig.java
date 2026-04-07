package dev.arcovia.mitigation.smt.config;

import java.util.Map;

import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

public record CostConfig(Map<String, Integer> addLabelCost, Map<String, Integer> removeLabelCost, Map<Node, Integer> nodeFactor,
        Map<Pin, Integer> pinFactor, boolean weighTFGs) {
}