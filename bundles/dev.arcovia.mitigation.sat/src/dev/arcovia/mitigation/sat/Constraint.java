package dev.arcovia.mitigation.sat;

import org.eclipse.jdt.annotation.NonNull;

import java.util.List;

/**
 * The Constraint class represents a collection of literals that together define
 * a specific logical condition or rule. Each literal within the constraint is a
 * component that can either be satisfied or not, based on its state.
 */
public record Constraint(List<Literal> literals) {

    // TODO remove after debugging
    @Override
    @NonNull
    public String toString() {
        var s = new StringBuilder();
            for (var literal : literals) {
                if (literals.contains(literal)) {
                    s.append(literal.positive() ? "" : "!").append(literals.indexOf(literal)).append(" ");
                } else {
                    s.append(literal.positive() ? "" : "!").append(literals.size()).append(" ");
                    literals.add(literal);
                }
            }
        return s.toString();
    }
}
