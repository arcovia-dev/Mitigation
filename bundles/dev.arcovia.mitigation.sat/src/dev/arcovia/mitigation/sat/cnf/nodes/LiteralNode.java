package dev.arcovia.mitigation.sat.cnf.nodes;

import java.util.List;

import dev.arcovia.mitigation.sat.*;

public class LiteralNode extends LogicNode {
	private final Literal literal;

    // TODO remove comment
    // Careful!! Literal has boolean positive while all selectors have boolean inverted
	public LiteralNode(boolean inverted, CompositeLabel label) {
		this.literal = new Literal(inverted, label); // inverted for CNF
	}
	
	@Override
	public void collectCNFClauses(List<Constraint> result, List<Constraint> activeConstraints) {
        activeConstraints.forEach(it -> it.literals().add(literal));
	}

    @Override
    public String toString() {
        var positive = literal.positive() ? "!" : "";
        var label = literal.compositeLabel();
        return "%s[%s %s.%s] ".formatted(positive, label.category(), label.label().type(), label.label().value());
    }
}
