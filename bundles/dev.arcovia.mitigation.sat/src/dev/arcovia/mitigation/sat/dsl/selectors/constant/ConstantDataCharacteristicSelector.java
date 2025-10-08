package dev.arcovia.mitigation.sat.dsl.selectors.constant;

import dev.arcovia.mitigation.sat.IncomingDataLabel;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.OutgoingDataLabel;
import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.sat.dsl.nodes.LiteralNode;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicsSelector;

/**
 * Wraps a {@link DataCharacteristicsSelector} to add a literal representing a constant
 * data characteristic to a {@link BranchNode}. Validates that both the characteristic type
 * and value are constant before adding the corresponding {@link LiteralNode}.
 */
public class ConstantDataCharacteristicSelector implements ConstantDataSelector {
    private final DataCharacteristicsSelector selector;

    /**
     * Constructs a {@link ConstantDataCharacteristicSelector} wrapping the given {@link DataCharacteristicsSelector}.
     *
     * @param selector the {@link DataCharacteristicsSelector} to wrap
     */
    public ConstantDataCharacteristicSelector(DataCharacteristicsSelector selector) {
        this.selector = selector;
    }

    /**
     * Adds a literal to the root node representing this constant data characteristic.
     * Ensures that both the characteristic type and value are constant; throws an exception otherwise.
     *
     * @param root the {@link BranchNode} to which the literal is added
     * @throws IllegalStateException if the characteristic type or value is not constant
     */
    @Override
    public void addLiterals(BranchNode root) {
        var characteristicType = selector.getDataCharacteristic().characteristicType();
        var characteristicValue = selector.getDataCharacteristic().characteristicValue();

        if(!characteristicType.isConstant()) {
            throw new IllegalStateException("Selector Type not constant:" + selector);
        }

        if(!characteristicValue.isConstant()) {
            throw new IllegalStateException("Selector Value not constant:" + selector);
        }

        var type = characteristicType.values().orElseThrow().get(0);
        var values = characteristicValue.values().orElseThrow().get(0);

        root.addPredicate(new LiteralNode(selector.isInverted(), new IncomingDataLabel(new Label(type, values))));
    }
}