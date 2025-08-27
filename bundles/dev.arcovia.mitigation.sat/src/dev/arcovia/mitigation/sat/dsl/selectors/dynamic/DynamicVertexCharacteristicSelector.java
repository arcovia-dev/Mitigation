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

public class DynamicVertexCharacteristicSelector implements DynamicDataSelector {
    private final VertexCharacteristicsSelector selector;

    public DynamicVertexCharacteristicSelector(VertexCharacteristicsSelector selector) {
        this.selector = selector;
    }

    @Override
    public boolean isInverted() {
        return selector.isInverted();
    }

    @Override
    public void addLiterals(BranchNode root, Map<String, List<String>> variables, boolean hasOutgoingData, boolean hasIncomingData, boolean inverted) {
        List<String> vars = variables
                .get(selector.getCharacteristicsSelectorData().characteristicValue().name());
        if(vars == null || vars.isEmpty()) { throw new IllegalStateException("Variables not found."); }
        List<CharacteristicsSelectorData> data = vars.stream().map(it ->
                new CharacteristicsSelectorData(
                        selector.getCharacteristicsSelectorData().characteristicType(),
                        ConstraintVariableReference.ofConstant(List.of(it))
                )).toList();
        new ConstantVertexCharacteristicListSelector(
                new VertexCharacteristicsListSelector(null, data, inverted)
        ).addLiterals(root, hasOutgoingData, hasIncomingData);
    }

    @Override
    public List<Label> getLabels(Map<String, List<String>> variables) {
        var characteristicType = selector.getCharacteristicsSelectorData().characteristicType();
        var characteristicValue = selector.getCharacteristicsSelectorData().characteristicValue();

        if(!characteristicType.isConstant()) {
            throw new IllegalStateException("Selector Type not constant:" + selector);
        }

        var type = characteristicType.values().orElseThrow().get(0);
        var values = variables.get(characteristicValue.name());

        return values.stream().map(it -> new Label(type, it)).toList();
    }
}