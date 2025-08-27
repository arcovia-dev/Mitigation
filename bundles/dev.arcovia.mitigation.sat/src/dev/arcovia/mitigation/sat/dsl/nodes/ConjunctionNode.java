package dev.arcovia.mitigation.sat.dsl.nodes;

import java.util.ArrayList;
import java.util.List;

import dev.arcovia.mitigation.sat.Constraint;

// not a record because each node should have different hash values
public class ConjunctionNode implements BranchNode {
	private final List<LogicNode> predicates = new ArrayList<>();

    public List<LogicNode> getPredicates() {
        return predicates;
    }

    @Override
	public void addPredicate(LogicNode predicate) {
        // this is only for constructing a formula tree from the cnf, not for the dsl
        if (predicate instanceof LiteralNode literalNode && predicates.contains(literalNode)) {
            return;
        }
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
