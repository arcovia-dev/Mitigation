package dev.arcovia.mitigation.sat.dsl.nodes;

import java.util.List;

import dev.arcovia.mitigation.sat.*;

public record LiteralNode(Literal literal) implements LogicNode {

	public LiteralNode(boolean inverted, CompositeLabel label) {
        // Careful!! Literal has boolean positive while all selectors have boolean inverted
        // this constructs the literal inverted which is needed for the CNF
        this(new Literal(inverted, label));
	}
	
	@Override
	public void collectCNFClauses(List<Constraint> result, List<Constraint> activeConstraints) {
        activeConstraints.forEach(it -> it.literals().add(literal));
	}

    @Override
    public String toString() {
        var positive = literal.positive() ? "!" : "";
        var label = literal.compositeLabel();
        return "%s[%s %s.%s] ".formatted(positive, label.category().name(), label.label().type(), label.label().value());
    }
}
