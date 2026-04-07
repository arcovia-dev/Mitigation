package dev.arcovia.mitigation.smt.config;

/**
 * Contains an input configuration for solving
 */
public record Config(boolean onlyRelevantModifications, boolean addNodeLabels, boolean removeNodeLabels, boolean addDataLabels,
        boolean removeDataLabels, CostConfig costConfig, boolean checkForViolationsAfter, boolean findExpressionTreeSize, boolean onlyViolatingTFGs) {
}
