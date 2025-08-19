package dev.arcovia.mitigation.sat.cnf.selectors.constant;

import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.sat.cnf.nodes.BranchNode;
import dev.arcovia.mitigation.sat.cnf.nodes.LiteralNode;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsSelector;

public class ConstantVertexCharacteristicSelector implements ConstantDataSelector {
    private final VertexCharacteristicsSelector selector;

    public ConstantVertexCharacteristicSelector(VertexCharacteristicsSelector selector) {
        this.selector = selector;
    }

    @Override
    public void addLiterals(BranchNode root, boolean hasOutgoingData, boolean hasIncomingData) {
        var characteristicType = selector.getCharacteristicsSelectorData().characteristicType();
        var characteristicValue = selector.getCharacteristicsSelectorData().characteristicValue();

        if(!characteristicType.isConstant()) {
            throw new IllegalStateException("Selector Type not constant:" + selector);
        }

        if(!characteristicValue.isConstant()) {
            throw new IllegalStateException("Selector Type not constant:" + selector);
        }

        var type = characteristicType.values().orElseThrow().get(0);
        var values = characteristicValue.values().orElseThrow().get(0);

        root.addPredicate(new LiteralNode(selector.isInverted(), new NodeLabel(new Label(type, values))));
    }
}