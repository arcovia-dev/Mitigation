package dev.arcovia.mitigation.smt.config;

/**
 * Contains an input configuration for solving
 */

public class Config {

    private final boolean onlyRelevantModifications;
    private final boolean addNodeLabels;
    private final boolean removeNodeLabels;
    private final boolean addDataLabels;
    private final boolean removeDataLabels;
    private final CostConfig costConfig;
    private final boolean checkForViolationsAfter;
    private final boolean findExpressionTreeSize;
    private final boolean onlyViolatingTFGs;

    protected Config(boolean onlyRelevantLabels, boolean addNodeLabels, boolean removeNodeLabels, boolean addDataLabels, boolean removeDataLabels,
            CostConfig costConfig, boolean checkForViolationsAfter, boolean findExpressionTreeSize, boolean onlyViolatingTFGs) {
        this.onlyRelevantModifications = onlyRelevantLabels;
        this.addNodeLabels = addNodeLabels;
        this.removeNodeLabels = removeNodeLabels;
        this.addDataLabels = addDataLabels;
        this.removeDataLabels = removeDataLabels;
        this.costConfig = costConfig;
        this.findExpressionTreeSize = findExpressionTreeSize;
        this.checkForViolationsAfter = checkForViolationsAfter;
        this.onlyViolatingTFGs = onlyViolatingTFGs;
    }

    /**
     * @return Whether the expression tree size will be returned
     */
    public boolean isFindExpressionTreeSize() {
        return findExpressionTreeSize;
    }

    /**
     * @return Whether only constraint-repairing
     */
    public boolean isOnlyRelevantModifications() {
        return onlyRelevantModifications;
    }

    /**
     * @return Whether node labels should be added
     */
    public boolean isAddNodeLabels() {
        return addNodeLabels;
    }

    /**
     * @return Whether node labels should be removed
     */
    public boolean isRemoveNodeLabels() {
        return removeNodeLabels;
    }

    /**
     * @return Whether data labels should be added
     */
    public boolean isAddDataLabels() {
        return addDataLabels;
    }

    /**
     * @return Whether data labels should be removed
     */
    public boolean isRemoveDataLabels() {
        return removeDataLabels;
    }

    /**
     * @return The chosen cost config
     */
    public CostConfig getCostConfig() {
        return costConfig;
    }

    /**
     * @return Whether the result should be checked for violations by DFA
     */
    public boolean isCheckForViolationsAfter() {
        return checkForViolationsAfter;
    }

    /**
     * @return Whether only violating TFGs should be considered.
     */
    public boolean isOnlyViolatingTFGs() {
        return onlyViolatingTFGs;
    }

}
