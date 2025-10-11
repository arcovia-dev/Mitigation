package dev.arcovia.mitigation.sat.dsl.selectors.dynamic;

import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.sat.dsl.selectors.constant.ConstantVertexCharacteristicListSelector;
import org.dataflowanalysis.analysis.dsl.selectors.CharacteristicsSelectorData;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsListSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsSelector;
import org.dataflowanalysis.analysis.dsl.variable.ConstraintVariableReference;

import java.util.List;
import java.util.Map;

/**
 * Wraps a {@link VertexCharacteristicsSelector} to provide dynamic vertex characteristic selection.
 * Converts variable values into constant vertex characteristic literals for a {@link BranchNode}
 * and generates {@link Label} objects for dynamic vertex characteristics based on provided variables.
 */
public class DynamicVertexCharacteristicSelector implements DynamicDataSelector {
    private final VertexCharacteristicsSelector selector;

    /**
     * Constructs a {@link DynamicVertexCharacteristicSelector} wrapping the given {@link VertexCharacteristicsSelector}.
     *
     * @param selector the {@link VertexCharacteristicsSelector} to wrap
     */
    public DynamicVertexCharacteristicSelector(VertexCharacteristicsSelector selector) {
        this.selector = selector;
    }

    /**
     * Returns whether this vertex characteristic selector is inverted.
     *
     * @return true if the selector is inverted, false otherwise
     */
    @Override
    public boolean isInverted() {
        return selector.isInverted();
    }

    /**
     * Adds literals to the root node based on the dynamic vertex characteristic values.
     * Converts variable values into constant vertex characteristic selectors and delegates
     * literal addition. Throws an exception if no variables are found.
     *
     * @param root the {@link BranchNode} to which literals are added
     * @param variables a map of variable names to their corresponding string values
     * @param inverted whether the literals should be added as inverted
     * @throws IllegalStateException if no variables are found for the selector
     */
    @Override
    public void addLiterals(BranchNode root, Map<String, List<String>> variables, boolean inverted) {
        List<String> vars = variables
                .get(selector.getVertexCharacteristics().characteristicValue().name());
        if(vars == null || vars.isEmpty()) { throw new IllegalStateException("Variables not found."); }
        List<CharacteristicsSelectorData> data = vars.stream().map(it ->
                new CharacteristicsSelectorData(
                        selector.getVertexCharacteristics().characteristicType(),
                        ConstraintVariableReference.ofConstant(List.of(it))
                )).toList();
        new ConstantVertexCharacteristicListSelector(
                new VertexCharacteristicsListSelector(null, data, inverted)
        ).addLiterals(root);
    }

    /**
     * Returns a list of {@link Label} objects for the dynamic vertex characteristic based on the provided variables.
     * Ensures the characteristic type is constant and maps variable values to labels.
     *
     * @param variables a map of variable names to their corresponding string values
     * @return a list of {@link Label} objects representing the characteristic values
     * @throws IllegalStateException if the characteristic type is not constant
     */
    @Override
    public List<Label> getLabels(Map<String, List<String>> variables) {
        var characteristicType = selector.getVertexCharacteristics().characteristicType();
        var characteristicValue = selector.getVertexCharacteristics().characteristicValue();

        if(!characteristicType.isConstant()) {
            throw new IllegalStateException("Selector Type not constant:" + selector);
        }

        var type = characteristicType.values().orElseThrow().get(0);
        var values = variables.get(characteristicValue.name());

        return values.stream().map(it -> new Label(type, it)).toList();
    }
}