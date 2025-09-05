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
	
	public BaseFormula() {
		this.root = new ConjunctionNode();
	}

    public BaseFormula add(BaseFormula baseFormula) {
        root.addPredicate(baseFormula.getRoot());
        return this;
    }

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

    public BranchNode getRoot() {
        return root;
    }
	
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

    @Override
    public String toString() {
        return root.toString();
    }
}
