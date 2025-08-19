package dev.arcovia.mitigation.sat.cnf.nodes;

import java.util.ArrayList;
import java.util.List;

import dev.arcovia.mitigation.sat.Constraint;

public class ConjunctionNode extends BranchNode {
	private final List<LogicNode> predicates = new ArrayList<LogicNode>();

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
            String str = predicate.toString();
            if(str.isEmpty()) continue;
            res.append(str).append("AND ");
        }
        res.delete(res.length() - 4, res.length());
        res.append(") ");
        return res.toString();
    }
}
