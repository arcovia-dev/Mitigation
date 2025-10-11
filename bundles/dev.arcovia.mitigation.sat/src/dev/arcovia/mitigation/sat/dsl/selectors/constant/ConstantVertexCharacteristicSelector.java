package dev.arcovia.mitigation.sat.dsl.selectors.constant;

import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.sat.dsl.nodes.LiteralNode;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsSelector;

/**
 * Wraps a {@link VertexCharacteristicsSelector} to add a literal representing a constant vertex characteristic to a
 * {@link BranchNode}. Validates that both the characteristic type and value are constant before adding the
 * corresponding {@link LiteralNode}.
 */
public class ConstantVertexCharacteristicSelector implements ConstantDataSelector {
    private final VertexCharacteristicsSelector selector;

    /**
     * Constructs a {@link ConstantVertexCharacteristicSelector} wrapping the given {@link VertexCharacteristicsSelector}.
     * @param selector the {@link VertexCharacteristicsSelector} to wrap
     */
    public ConstantVertexCharacteristicSelector(VertexCharacteristicsSelector selector) {
        this.selector = selector;
    }

    /**
     * Adds a literal to the root node representing this constant vertex characteristic. Ensures that both the
     * characteristic type and value are constant; throws an exception otherwise.
     * @param root the {@link BranchNode} to which the literal is added
     * @throws IllegalStateException if the characteristic type or value is not constant
     */
    @Override
    public void addLiterals(BranchNode root) {
        var characteristicType = selector.getVertexCharacteristics()
                .characteristicType();
        var characteristicValue = selector.getVertexCharacteristics()
                .characteristicValue();

        if (!characteristicType.isConstant()) {
            throw new IllegalStateException("Selector Type not constant:" + selector);
        }

        if (!characteristicValue.isConstant()) {
            throw new IllegalStateException("Selector Type not constant:" + selector);
        }

        var type = characteristicType.values()
                .orElseThrow()
                .get(0);
        var values = characteristicValue.values()
                .orElseThrow()
                .get(0);

        root.addPredicate(new LiteralNode(selector.isInverted(), new NodeLabel(new Label(type, values))));
    }
}