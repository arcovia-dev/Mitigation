package dev.arcovia.mitigation.sat.dsl.nodes;

import java.util.ArrayList;
import java.util.List;

import dev.arcovia.mitigation.sat.Constraint;

/**
 * Represents a disjunction (OR) node in a logical formula tree.
 * <p>
 * Contains a list of predicate {@link LogicNode} elements and provides methods to add predicates, collect CNF clauses
 * with branching for each predicate, and generate a string representation of the disjunction.
 * <p>
 * It is not a record class because each node should have different hash values.
 */
public class DisjunctionNode implements BranchNode {
    protected final List<LogicNode> predicates = new ArrayList<>();

    /**
     * Returns the list of predicate nodes.
     * @return a list of {@link LogicNode} representing the predicates
     */
    public List<LogicNode> getPredicates() {
        return predicates;
    }

    /**
     * Adds the specified predicate to the list if it is not already contained. Intended only for constructing a formula
     * tree from the CNF, not for the DSL.
     * @param predicate the {@link LogicNode} to add
     */
    @Override
    public void addPredicate(LogicNode predicate) {
        // this is only for constructing a formula tree from the cnf, not for the dsl
        if (predicate instanceof LiteralNode literalNode && predicates.contains(literalNode)) {
            return;
        }
        predicates.add(predicate);
    }

    /**
     * Collects CNF clauses from each predicate, creating separate branches of active constraints for all but the first
     * predicate. Each branch is processed recursively, and resulting constraints are accumulated in the provided result
     * list.
     * @param result the list to which collected {@link Constraint} objects are added
     * @param activeConstraints the list of currently active {@link Constraint} objects
     */
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
            predicates.get(i)
                    .collectCNFClauses(result, branch);
            if (i > 0) {
                activeConstraints.addAll(branch);
            }
        }
    }

    /**
     * Returns a string representation of this node by concatenating all predicate strings with "OR". Empty predicates are
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
                    .append("OR ");
        }
        res.delete(res.length() - 3, res.length());
        res.append(") ");
        return res.toString();
    }
}
