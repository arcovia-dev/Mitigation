package dev.arcovia.mitigation.utils.dsl.nodes;

import java.util.ArrayList;
import java.util.List;

import dev.arcovia.mitigation.sat.Constraint;

/**
 * Represents a conjunction (AND) node in a logical formula tree.
 * <p>
 * Contains a list of predicate {@link LogicNode} elements and provides methods to add predicates, collect CNF clauses
 * from its children, and generate a string representation of the conjunction.
 * <p>
 * It is not a record class because each node should have different hash values.
 */
public class ConjunctionNode implements BranchNode {
    private final List<LogicNode> predicates = new ArrayList<>();

    /**
     * Returns the list of predicate logic nodes.
     * @return a list of {@link LogicNode} representing the predicates
     */
    public List<LogicNode> getPredicates() {
        return predicates;
    }

    /**
     * Adds a predicate to the list if it is not already present. This method is used only for constructing a formula tree
     * from the CNF, not for the DSL.
     * @param predicate the {@link LogicNode} to add
     */
    @Override
    public void addPredicate(LogicNode predicate) {
        if (predicate instanceof LiteralNode literalNode && predicates.contains(literalNode)) {
            return;
        }
        predicates.add(predicate);
    }

    /**
     * Collects CNF clauses from all predicate nodes and adds them to the given result list.
     * @param result the list to which collected {@link Constraint} objects are added
     * @param activeConstraints the list of currently active {@link Constraint} objects
     */
    @Override
    public void collectCNFClauses(List<Constraint> result, List<Constraint> activeConstraints) {
        predicates.forEach(it -> it.collectCNFClauses(result, activeConstraints));
    }

    /**
     * Returns a string representation of this node by concatenating all predicate strings with "AND". Empty predicates are
     * skipped, and multiple predicates are enclosed in parentheses.
     * @return a string representation of the combined predicates
     */
    @Override
    public String toString() {
        if (predicates.isEmpty())
            return "";
        if (predicates.size() == 1)
            return predicates.get(0)
                    .toString();
        var res = new StringBuilder();
        res.append("( ");
        for (LogicNode predicate : predicates) {
            String str = predicate.toString();
            if (str.isEmpty())
                continue;
            res.append(str)
                    .append("AND ");
        }
        res.delete(res.length() - 4, res.length());
        res.append(") ");
        return res.toString();
    }
}
