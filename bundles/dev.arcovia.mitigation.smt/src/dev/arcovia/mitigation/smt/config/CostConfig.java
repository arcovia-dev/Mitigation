package dev.arcovia.mitigation.smt.config;

import java.util.Map;

import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

/**
 * Contains a cost function configuration
 */

public class CostConfig {

    /**
     * Returns the costs for adding labels
     * @return Cost Map
     */
    public Map<String, Integer> getAddLabelCost() {
        return addLabelCost;
    }

    /**
     * Returns the cost for removing labels
     * @return Cost Map
     */
    public Map<String, Integer> getRemoveLabelCost() {
        return removeLabelCost;
    }

    /**
     * Returns node-specific weight factors
     * @return Node factor Map
     */
    public Map<Node, Integer> getNodeFactor() {
        return nodeFactor;
    }

    /**
     * Returns pin-specific weight factors
     * @return Pin factor Map
     */
    public Map<Pin, Integer> getPinFactor() {
        return pinFactor;
    }

    /**
     * Returns whether this cost config will weigh nodes and pins according to TFGs
     * @return flag
     */
    public boolean isWeighTFGs() {
        return weighTFGs;
    }

    /**
     * Sets the node factors of this cost config
     * @param nodeFactor New Node Factors
     */
    public void setNodeFactor(Map<Node, Integer> nodeFactor) {
        this.nodeFactor = nodeFactor;
    }

    /**
     * Sets the pin factors of this cost config
     * @param pinFactor New pin factors
     */
    public void setPinFactor(Map<Pin, Integer> pinFactor) {
        this.pinFactor = pinFactor;
    }

    private final Map<String, Integer> addLabelCost;
    private final Map<String, Integer> removeLabelCost;
    private Map<Node, Integer> nodeFactor;
    private Map<Pin, Integer> pinFactor;
    private final boolean weighTFGs;

    /**
     * Creates a Cost Config based on parameters
     * @param addLabelCost Cost for Label Addition
     * @param removeLabelCost Cost for Label Removal
     * @param nodeFactor Node Factors
     * @param pinFactor Pin Factors
     * @param weighTFGs If tfg weighing should be enabled
     */
    protected CostConfig(Map<String, Integer> addLabelCost, Map<String, Integer> removeLabelCost, Map<Node, Integer> nodeFactor,
            Map<Pin, Integer> pinFactor, boolean weighTFGs) {
        this.addLabelCost = addLabelCost;
        this.removeLabelCost = removeLabelCost;
        this.nodeFactor = nodeFactor;
        this.pinFactor = pinFactor;
        this.weighTFGs = weighTFGs;
    }
}
