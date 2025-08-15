package dev.arcovia.mitigation.sat.cnf;

import java.util.List;

import dev.arcovia.mitigation.sat.*;
import org.dataflowanalysis.analysis.dsl.selectors.AbstractSelector;
import org.dataflowanalysis.analysis.dsl.selectors.CharacteristicsSelectorData;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicsSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsSelector;

public class LiteralNode extends LogicNode {
	protected final Literal literal;
	
	private LiteralNode(boolean positive, CompositeLabel label) {
		super(LogicNodeDescriptor.LITERAL);
		this.literal = new Literal(!positive, label); // inverted for CNF
	}

    public static LiteralNode ofIncomingData(DataCharacteristicsSelector selector) {
        return new LiteralNode(selector.isInverted(), new IncomingDataLabel(getConstantLabel(selector.getCharacteristicsSelectorData())));
    }

    public static LiteralNode ofOutgoingData(DataCharacteristicsSelector selector) {
        return new LiteralNode(selector.isInverted(), new OutgoingDataLabel(getConstantLabel(selector.getCharacteristicsSelectorData())));
    }

    public static LiteralNode ofNode(VertexCharacteristicsSelector selector) {
        return new LiteralNode(selector.isInverted(), new OutgoingDataLabel(getConstantLabel(selector.getCharacteristicsSelectorData())));
    }

    private static Label getConstantLabel(CharacteristicsSelectorData characteristicsSelectorData) {
        var characteristicType = characteristicsSelectorData.characteristicType();
        var characteristicValue = characteristicsSelectorData.characteristicValue();

        if(!characteristicType.isConstant()) throw new IllegalStateException("Selector Type not constant:" + characteristicsSelectorData);
        if(!characteristicValue.isConstant()) throw new IllegalStateException("Selector Value not constant:" + characteristicsSelectorData);

        var type = characteristicType.values().orElseThrow().get(0);
        var value = characteristicValue.values().orElseThrow().get(0);

        return new Label(type, value);
    }
	
	@Override
	public void collectCNFClauses(List<Constraint> result, List<Constraint> activeClauses) {
		activeClauses.forEach(it -> it.literals().add(literal));
	}
}
