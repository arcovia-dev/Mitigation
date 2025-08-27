package dev.arcovia.mitigation.sat.dsl.selectors.constant;

import dev.arcovia.mitigation.sat.IncomingDataLabel;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.OutgoingDataLabel;
import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.sat.dsl.nodes.LiteralNode;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicsSelector;

public class ConstantDataCharacteristicSelector implements ConstantDataSelector {
    private final DataCharacteristicsSelector selector;

    public ConstantDataCharacteristicSelector(DataCharacteristicsSelector selector) {
        this.selector = selector;
    }

    @Override
    public void addLiterals(BranchNode root, boolean hasOutgoingData, boolean hasIncomingData) {
        var characteristicType = selector.getDataCharacteristic().characteristicType();
        var characteristicValue = selector.getDataCharacteristic().characteristicValue();

        if(!characteristicType.isConstant()) {
            throw new IllegalStateException("Selector Type not constant:" + selector);
        }

        if(!characteristicValue.isConstant()) {
            throw new IllegalStateException("Selector Type not constant:" + selector);
        }

        var type = characteristicType.values().orElseThrow().get(0);
        var values = characteristicValue.values().orElseThrow().get(0);

        if (hasIncomingData) {
            root.addPredicate(new LiteralNode(selector.isInverted(), new IncomingDataLabel(new Label(type, values))));
        }
        if (hasOutgoingData) {
            root.addPredicate(new LiteralNode(selector.isInverted(), new OutgoingDataLabel(new Label(type, values))));
        }
    }
}