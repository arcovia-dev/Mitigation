package dev.arcovia.mitigation.sat.dsl.nodes;

import java.util.List;

import dev.arcovia.mitigation.sat.Constraint;

public interface LogicNode {
	void collectCNFClauses(List<Constraint> result, List<Constraint> activeClauses);
}
