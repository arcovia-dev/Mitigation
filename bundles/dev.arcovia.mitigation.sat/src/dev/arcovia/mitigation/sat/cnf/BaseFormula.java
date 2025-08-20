package dev.arcovia.mitigation.sat.cnf;

import dev.arcovia.mitigation.sat.Constraint;
import dev.arcovia.mitigation.sat.cnf.nodes.BranchNode;
import dev.arcovia.mitigation.sat.cnf.nodes.LogicNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BaseFormula {
	private final BranchNode root;
	
	public BaseFormula(BranchNode root) {
		this.root = root;
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
