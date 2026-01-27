package dev.arcovia.mitigation.utils.dsl.nodes;

import java.util.List;

import dev.arcovia.mitigation.sat.*;

/**
 * Represents a single literal in a logical formula tree.
 * <p>
 * Encapsulates a {@link Literal}, supports construction from an inversion flag and label, contributes itself to CNF
 * clauses, and provides a string representation including polarity and label details.
 */
public record LiteralNode(Literal literal) implements LogicNode {

    /**
     * Constructs a {@link LiteralNode} with the given inversion flag and label. Note: The inversion is applied to match CNF
     * representation, where literals may differ from selector polarity.
     * @param inverted whether the literal is inverted
     * @param label the {@link CompositeLabel} associated with this literal
     */
    public LiteralNode(boolean inverted, CompositeLabel label) {
        // Careful!! Literal has boolean positive while all selectors have boolean inverted
        // this constructs the literal inverted which is needed for the CNF
        this(new Literal(inverted, label));
    }

    /**
     * Adds this literal to the list of literals in each active constraint.
     * @param result the list to which collected {@link Constraint} objects are added
     * @param activeConstraints the list of currently active {@link Constraint} objects to update
     */
    @Override
    public void collectCNFClauses(List<Constraint> result, List<Constraint> activeConstraints) {
        activeConstraints.forEach(it -> it.literals()
                .add(literal));
    }

    /**
     * Returns a string representation of this literal, including its polarity and composite label details.
     * @return a formatted string representing the literal
     */
    @Override
    public String toString() {
        var positive = literal.positive() ? "!" : "";
        var label = literal.compositeLabel();
        return "%s[%s %s.%s] ".formatted(positive, label.category()
                .name(),
                label.label()
                        .type(),
                label.label()
                        .value());
    }
}
