package dev.arcovia.mitigation.sat.dsl.selectors.constant;

import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.sat.dsl.nodes.ConjunctionNode;
import dev.arcovia.mitigation.sat.dsl.nodes.DisjunctionNode;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicListSelector;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicsSelector;

public class ConstantDataCharacteristicListSelector implements ConstantDataSelector {
    private final DataCharacteristicListSelector selector;

    public ConstantDataCharacteristicListSelector(DataCharacteristicListSelector selector) {
        this.selector = selector;
    }

    @Override
    public void addLiterals(BranchNode root) {
        var incomingDataNode = selector.isInverted() ? new ConjunctionNode() : new DisjunctionNode();
        root.addPredicate(incomingDataNode);

        var dataSelectors = selector.getDataCharacteristics().stream().map(it ->
                new ConstantDataCharacteristicSelector(
                        new DataCharacteristicsSelector(null, it, selector.isInverted())
                )).toList();
        dataSelectors.forEach(it -> it.addLiterals(incomingDataNode));
    }
}