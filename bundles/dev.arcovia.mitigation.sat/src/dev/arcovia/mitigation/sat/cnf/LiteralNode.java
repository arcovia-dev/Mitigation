package dev.arcovia.mitigation.sat.cnf;

import java.util.List;

import dev.arcovia.mitigation.sat.*;
import org.dataflowanalysis.analysis.dsl.selectors.AbstractSelector;
import org.dataflowanalysis.analysis.dsl.selectors.CharacteristicsSelectorData;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicsSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsSelector;

public class LiteralNode extends LogicNode {
	protected final Literal literal;
	
	public LiteralNode(boolean positive, CompositeLabel label) {
		super(LogicNodeDescriptor.LITERAL);
		this.literal = new Literal(!positive, label); // inverted for CNF
	}
	
	@Override
	public void collectCNFClauses(List<Constraint> result, List<Constraint> activeClauses) {
		activeClauses.forEach(it -> it.literals().add(literal));
	}
}
