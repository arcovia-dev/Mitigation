package dev.arcovia.mitigation.smt.config;

/**
 * Config Builder for convenient config creation.
 */

public class ConfigBuilder {

    // Considering only those modifications that can repair confidentiality
    // violations is faster
    private boolean onlyRelevantModifications = true;
    // Consider complete solution as default
    private boolean addNodeLabels = true;
    private boolean removeNodeLabels = true;
    private boolean addDataLabels = true;
    private boolean removeDataLabels = true;
    // Default cost
    private CostConfig costConfig = new CostConfigBuilder().build();
    // These introduce runtime overhead
    private boolean checkForViolationsAfter = false;
    private boolean findExpressionTreeSize = false;
    // It is not proven that inspecting only violating TFGs leads to minimal repairs
    // for all cases
    // Therefore default false
    private boolean onlyViolatingTFGs = false;

    /**
     * WWhether only relevant Modifications should be considered
     * @param onlyRelevantModifications
     * @return The Builder
     */
    public ConfigBuilder onlyRelevantModifications(boolean onlyRelevantModifications) {
        this.onlyRelevantModifications = onlyRelevantModifications;
        return this;
    }

    /**
     * Whether node labels should be added
     * @param addNodeLabels
     * @return The Builder
     */
    public ConfigBuilder addNodeLabels(boolean addNodeLabels) {
        this.addNodeLabels = addNodeLabels;
        return this;
    }

    /**
     * Whether node labels should be removed
     * @param removeNodeLabels
     * @return The builder
     */
    public ConfigBuilder removeNodeLabels(boolean removeNodeLabels) {
        this.removeNodeLabels = removeNodeLabels;
        return this;
    }

    /**
     * Whether data labels should be added
     * @param addDataLabels
     * @return The Builder
     */
    public ConfigBuilder addDataLabels(boolean addDataLabels) {
        this.addDataLabels = addDataLabels;
        return this;
    }

    /**
     * Whether data labels should be removed
     * @param removeDataLabels
     * @return The builder
     */
    public ConfigBuilder removeDataLabels(boolean removeDataLabels) {
        this.removeDataLabels = removeDataLabels;
        return this;
    }

    /**
     * Sets the cost config of this builder
     * @param costConfig
     * @return The builder
     */
    public ConfigBuilder costConfig(CostConfig costConfig) {
        this.costConfig = costConfig;
        return this;
    }

    /**
     * Whether DFA should check for violations after
     * @param checkForViolationsAfter
     * @return The builder
     */
    public ConfigBuilder checkForViolationsAfter(boolean checkForViolationsAfter) {
        this.checkForViolationsAfter = checkForViolationsAfter;
        return this;
    }

    /**
     * Whether the Z3 expression tree size should be returned
     * @param findExpressionTreeSize
     * @return The builder
     */
    public ConfigBuilder findExpressionTreeSize(boolean findExpressionTreeSize) {
        this.findExpressionTreeSize = findExpressionTreeSize;
        return this;
    }

    /**
     * Whether only violating TFGs should be encoded
     * @param onlyViolatingTFGs
     * @return The builder
     */
    public ConfigBuilder onlyViolatingTFGs(boolean onlyViolatingTFGs) {
        this.onlyViolatingTFGs = onlyViolatingTFGs;
        return this;
    }

    /**
     * Creates a Config object based on the attributes of this builder
     * @return Config
     */
    public Config build() {
        return new Config(onlyRelevantModifications, addNodeLabels, removeNodeLabels, addDataLabels, removeDataLabels, costConfig,
                checkForViolationsAfter, findExpressionTreeSize, onlyViolatingTFGs);
    }

}
