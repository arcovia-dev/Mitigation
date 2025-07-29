package dev.arcovia.mitigation.sat;

import java.util.List;

/**
 * The Constraint class represents a collection of literals that together define
 * a specific logical condition or rule. Each literal within the constraint is a
 * component that can either be satisfied or not, based on its state.
 */
public record Constraint(List<Literal> literals) {

}
