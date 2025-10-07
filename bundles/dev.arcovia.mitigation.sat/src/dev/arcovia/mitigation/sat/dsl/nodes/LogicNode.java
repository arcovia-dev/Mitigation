package dev.arcovia.mitigation.sat.dsl.nodes;

import java.util.List;

import dev.arcovia.mitigation.sat.Constraint;

/**
 * Represents a node in a logical formula tree that can contribute to CNF clauses.
 * <p>
 * Implementing classes must provide a method to collect CNF constraints from this node
 * and its children into the given result list.
 */
public interface LogicNode {
	void collectCNFClauses(List<Constraint> result, List<Constraint> activeClauses);
}
