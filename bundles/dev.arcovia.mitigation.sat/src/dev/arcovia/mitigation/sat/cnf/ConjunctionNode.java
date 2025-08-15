package dev.arcovia.mitigation.sat.cnf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.arcovia.mitigation.sat.Constraint;

public class ConjunctionNode extends LogicNode implements BranchNode {
	protected final List<LogicNode> predicates = new ArrayList<LogicNode>();
	
	public ConjunctionNode() {
		super(LogicNodeDescriptor.CONJUNCTION);
	}

    @Override
	public void addPredicate(LogicNode predicate) {
		predicates.add(predicate);
	}
	
	@Override
	public void collectCNFClauses(List<Constraint> result, List<Constraint> activeClauses) {
		predicates.forEach(it -> it.collectCNFClauses(result, activeClauses));
	}
}
