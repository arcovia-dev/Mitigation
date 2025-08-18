package dev.arcovia.mitigation.sat.cnf;

import dev.arcovia.mitigation.sat.Constraint;

import java.util.ArrayList;
import java.util.List;

public class BaseFormula {
	private final LogicNode root;
	
	public BaseFormula(LogicNode root) {
		this.root = root;
	}
	
	public List<Constraint> toCNF() {
        var constraint = new Constraint(new ArrayList<>());
        var result = new ArrayList<>(List.of(constraint));
        var activeClauses = new ArrayList<>(List.of(constraint));

		root.collectCNFClauses(result, activeClauses);
        return result;
	}

    @Override
    public String toString() {
        return root.toString();
    }
}
