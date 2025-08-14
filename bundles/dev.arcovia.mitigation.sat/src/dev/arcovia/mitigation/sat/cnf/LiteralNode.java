package dev.arcovia.mitigation.sat.cnf;

import java.util.List;

import dev.arcovia.mitigation.sat.Constraint;
import dev.arcovia.mitigation.sat.Literal;

public class LiteralNode extends LogicNode {
	protected final Literal literal;
	
	public LiteralNode(Literal literal) {
		super(LogicNodeDescriptor.LITERAL);
		this.literal = literal;
	}
	
	@Override
	public void collectCNFClauses(List<Constraint> result, List<Constraint> activeClauses) {
		activeClauses.forEach(it -> it.literals().add(literal));
	}
}
