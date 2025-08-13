package dev.arcovia.mitigation.sat.cnf;

import java.util.ArrayList;
import java.util.List;

import dev.arcovia.mitigation.sat.Literal;

public class LiteralNode {
	protected final List<LiteralNode> predicates;
	protected final LogicNodeDescriptor descriptor;
	protected final Literal literal;
	
	public LiteralNode(LogicNodeDescriptor descriptor) {
		this.predicates = new ArrayList<LiteralNode>();
		this.descriptor = descriptor;
		this.literal = null;
	}
	
	public LiteralNode(Literal literal) {
		this.predicates = null;
		this.descriptor = LogicNodeDescriptor.LITERAL;
		this.literal = literal;
	}
	
	public void addPredicate(LiteralNode predicate) {
		if(descriptor == LogicNodeDescriptor.LITERAL) { return; }
		predicates.add(predicate);
	}
	
	public void collectCNFClauses(List<Clause> result, List<Clause> activeClauses) {
		switch(descriptor) {
			case CONJUNCTION	-> traverseConjunction(result, activeClauses);
			case DISJUNCTION	-> traverseDisjunction(result, activeClauses);
			case LITERAL		-> traverseLiteral(result, activeClauses);
		}
	}
	
	private void traverseConjunction(List<Clause> result, List<Clause> activeClauses) {
		predicates.forEach(it -> it.collectCNFClauses(result, activeClauses));
	}
	
	private void traverseDisjunction(List<Clause> result, List<Clause> activeClauses) {
	    List<List<Clause>> branchClauses = new ArrayList<>();
	    branchClauses.add(activeClauses);

	    predicates.stream()
	            .skip(1)
	            .forEach(p -> {
	                List<Clause> copiedClauses = activeClauses.stream()
	                		.map(Clause::new)
	                		.peek(result::add)
	                		.toList();
	                branchClauses.add(copiedClauses);
	            });

	    for (int i = 0; i < predicates.size(); i++) {
	        List<Clause> branch = branchClauses.get(i);
	        predicates.get(i).collectCNFClauses(result, branch);
	        if (i > 0) {
	            activeClauses.addAll(branch);
	        }
	    }
	}
	
	private void traverseLiteral(List<Clause> result, List<Clause> activeClauses) {
		activeClauses.forEach(it -> it.add(literal));
	}
}
