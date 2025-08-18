package dev.arcovia.mitigation.sat.cnf.selectors.constant;

import dev.arcovia.mitigation.sat.cnf.nodes.BranchNode;
import dev.arcovia.mitigation.sat.cnf.nodes.ConjunctionNode;
import dev.arcovia.mitigation.sat.cnf.nodes.DisjunctionNode;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicListSelector;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicsSelector;

public class ConstantDataCharacteristicListSelector implements ConstantDataSelector {
    private final DataCharacteristicListSelector selector;

    public ConstantDataCharacteristicListSelector(DataCharacteristicListSelector selector) {
        this.selector = selector;
    }

    @Override
    public void addLiterals(BranchNode root, boolean hasOutgoingData, boolean hasIncomingData) {
        var incomingDataNode = selector.isInverted() ? new ConjunctionNode() : new DisjunctionNode();
        var outgoingDataNode = selector.isInverted() ? new ConjunctionNode() : new DisjunctionNode();
        root.addPredicate(incomingDataNode);
        root.addPredicate(outgoingDataNode);

        var dataSelectors = selector.getCharacteristicsSelectorDataList().stream().map(it ->
                new ConstantDataCharacteristicSelector(
                        new DataCharacteristicsSelector(null, it, selector.isInverted())
                )).toList();
        dataSelectors.forEach(it -> it.addLiterals(incomingDataNode, hasOutgoingData, !hasIncomingData));
        dataSelectors.forEach(it -> it.addLiterals(outgoingDataNode, !hasOutgoingData, hasIncomingData));
    }
}