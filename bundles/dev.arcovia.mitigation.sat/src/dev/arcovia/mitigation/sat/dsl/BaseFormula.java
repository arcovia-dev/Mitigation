package dev.arcovia.mitigation.sat.dsl;

import dev.arcovia.mitigation.sat.Constraint;
import dev.arcovia.mitigation.sat.Literal;
import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.sat.dsl.nodes.ConjunctionNode;
import dev.arcovia.mitigation.sat.dsl.nodes.DisjunctionNode;
import dev.arcovia.mitigation.sat.dsl.nodes.LiteralNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BaseFormula {
	private final BranchNode root;

    /**
     * Constructs a {@link BaseFormula} with an initial root node as a {@link ConjunctionNode}.
     */
    public BaseFormula() {
		this.root = new ConjunctionNode();
	}

    /**
     * Adds the root node of another {@link BaseFormula} as a predicate to this formula's root.
     *
     * @param baseFormula the {@link BaseFormula} to add
     * @return this {@link BaseFormula} instance for chaining
     */
    public BaseFormula add(BaseFormula baseFormula) {
        root.addPredicate(baseFormula.getRoot());
        return this;
    }

    /**
     * Constructs a {@link BaseFormula} from a list of CNF {@link Constraint} objects.
     * Each constraint is converted into a conjunction node, and all are combined under a disjunction node.
     *
     * @param cnf the list of CNF constraints to convert
     * @return a {@link BaseFormula} representing the CNF
     */
    public static BaseFormula fromCNF(List<Constraint> cnf) {
        var root = new DisjunctionNode();
        for (Constraint constraint : cnf) {
            var node = new ConjunctionNode();
            root.addPredicate(node);
            for (Literal literal : constraint.literals()) {
                node.addPredicate(new LiteralNode(literal));
            }
        }
        var baseFormula = new BaseFormula();
        baseFormula.getRoot().addPredicate(root);
        return baseFormula;
    }

    /**
     * Returns the root {@link BranchNode} of this formula.
     *
     * @return the root node
     */
    public BranchNode getRoot() {
        return root;
    }

    /**
     * Converts this formula into a list of CNF {@link Constraint} objects.
     * If the formula is empty, returns an empty list.
     *
     * @return a list of CNF constraints representing this formula
     */
    public List<Constraint> toCNF() {
        var constraint = new Constraint(new ArrayList<>());
        var result = new ArrayList<>(List.of(constraint));
        var activeClauses = new ArrayList<>(List.of(constraint));

		root.collectCNFClauses(result, activeClauses);

        if (result.size() == 1 && result.get(0).literals().isEmpty()) {
            return Collections.emptyList();
        }
        return result;
	}

    /**
     * Returns a string representation of this formula by delegating to its root node.
     *
     * @return a string representation of the formula
     */
    @Override
    public String toString() {
        return root.toString();
    }
}
