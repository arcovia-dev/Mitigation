package dev.arcovia.mitigation.sat.cnf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.arcovia.mitigation.sat.Constraint;

public class ConjunctionNode extends BranchNode {
	protected final List<LogicNode> predicates = new ArrayList<LogicNode>();
	
	public ConjunctionNode() {
		super(LogicNodeDescriptor.CONJUNCTION);
	}

    @Override
	public void addPredicate(LogicNode predicate) {
		predicates.add(predicate);
	}
	
	@Override
	public void collectCNFClauses(List<Constraint> result, List<Constraint> activeConstraints) {
		predicates.forEach(it -> it.collectCNFClauses(result, activeConstraints));
	}

    @Override
    public String toString() {
        if (predicates.isEmpty()) return "";
        if (predicates.size() == 1) return predicates.get(0).toString();
        var res = new StringBuilder();
        res.append("( ");
        for (LogicNode predicate : predicates) {
            res.append(predicate.toString()).append("AND ");
        }
        res.delete(res.length() - 4, res.length());
        res.append(") ");
        return res.toString();
    }
}
