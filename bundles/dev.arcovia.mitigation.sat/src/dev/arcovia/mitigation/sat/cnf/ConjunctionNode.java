package dev.arcovia.mitigation.sat.cnf;

import java.util.Collections;
import java.util.List;

import dev.arcovia.mitigation.sat.Constraint;

public class ConjunctionNode extends LogicNode {
	protected final List<LogicNode> predicates = Collections.emptyList();
	
	public ConjunctionNode() {
		super(LogicNodeDescriptor.CONJUNCTION);
	}
	
	public void addPredicate(LogicNode predicate) {
		predicates.add(predicate);
	}
	
	@Override
	public void collectCNFClauses(List<Constraint> result, List<Constraint> activeClauses) {
		predicates.forEach(it -> it.collectCNFClauses(result, activeClauses));
	}
}
