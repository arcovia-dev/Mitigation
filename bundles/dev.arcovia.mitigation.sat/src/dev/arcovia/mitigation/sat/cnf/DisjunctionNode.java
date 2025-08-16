package dev.arcovia.mitigation.sat.cnf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.arcovia.mitigation.sat.Constraint;

public class DisjunctionNode extends BranchNode {
	protected final List<LogicNode> predicates = new ArrayList<LogicNode>();
	
	public DisjunctionNode() {
		super(LogicNodeDescriptor.LITERAL);
	}

    @Override
	public void addPredicate(LogicNode predicate) {
		predicates.add(predicate);
	}
	
	@Override
	public void collectCNFClauses(List<Constraint> result, List<Constraint> activeClauses) {
		List<List<Constraint>> branchClauses = new ArrayList<>();
	    branchClauses.add(activeClauses);

	    predicates.stream()
	            .skip(1)
	            .forEach(p -> {
	                List<Constraint> copiedClauses = activeClauses.stream()
	                		.map(Constraint::literals)
	                		.map(Constraint::new)
	                		.peek(result::add)
	                		.toList();
	                branchClauses.add(copiedClauses);
	            });

	    for (int i = 0; i < predicates.size(); i++) {
	        List<Constraint> branch = branchClauses.get(i);
	        predicates.get(i).collectCNFClauses(result, branch);
	        if (i > 0) {
	            activeClauses.addAll(branch);
	        }
	    }
	}
}
