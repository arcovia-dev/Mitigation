package dev.arcovia.mitigation.sat.dsl.nodes;

import java.util.ArrayList;
import java.util.List;

import dev.arcovia.mitigation.sat.Constraint;

public class DisjunctionNode extends BranchNode {
	protected final List<LogicNode> predicates = new ArrayList<>();

    @Override
	public void addPredicate(LogicNode predicate) {
		predicates.add(predicate);
	}

    @Override
	public void collectCNFClauses(List<Constraint> result, List<Constraint> activeConstraints) {
		List<List<Constraint>> branchClauses = new ArrayList<>();
	    branchClauses.add(activeConstraints);

	    predicates.stream()
	            .skip(1)
	            .forEach(p -> {
	                List<Constraint> copiedConstraints = activeConstraints.stream()
	                		.map(Constraint::literals)
                            .map(ArrayList::new)
	                		.map(Constraint::new)
	                		.peek(result::add)
	                		.toList();
	                branchClauses.add(copiedConstraints);
	            });

	    for (int i = 0; i < predicates.size(); i++) {
	        List<Constraint> branch = branchClauses.get(i);
	        predicates.get(i).collectCNFClauses(result, branch);
	        if (i > 0) {
	            activeConstraints.addAll(branch);
	        }
	    }
	}

    @Override
    public String toString() {
        if (predicates.isEmpty()) return "";
        if (predicates.size() == 1) return predicates.get(0).toString();
        var res = new StringBuilder();
        res.append("( ");
        for (LogicNode predicate : predicates) {
            String str = predicate.toString();
            if(str.isEmpty()) continue;
            res.append(str).append("OR ");
        }
        res.delete(res.length() - 3, res.length());
        res.append(") ");
        return res.toString();
    }
}
