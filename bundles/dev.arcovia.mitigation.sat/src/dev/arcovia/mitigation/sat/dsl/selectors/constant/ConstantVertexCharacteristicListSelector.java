package dev.arcovia.mitigation.sat.dsl.selectors.constant;

import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.sat.dsl.nodes.ConjunctionNode;
import dev.arcovia.mitigation.sat.dsl.nodes.DisjunctionNode;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsListSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsSelector;

public class ConstantVertexCharacteristicListSelector implements ConstantDataSelector {
    private final VertexCharacteristicsListSelector selector;

    public ConstantVertexCharacteristicListSelector(VertexCharacteristicsListSelector selector) {
        this.selector = selector;
    }

    @Override
    public void addLiterals(BranchNode root, boolean hasOutgoingData, boolean hasIncomingData) {
        var node = selector.isInverted() ? new ConjunctionNode() : new DisjunctionNode();
        root.addPredicate(node);
        selector.getCharacteristicsSelectorDataList().forEach(it ->
                new ConstantVertexCharacteristicSelector(
                        new VertexCharacteristicsSelector(null, it, selector.isInverted())
                ).addLiterals(node, hasOutgoingData, hasIncomingData));
    }
}