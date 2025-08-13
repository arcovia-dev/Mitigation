package dev.arcovia.mitigation.sat.cnf;

import java.util.ArrayList;

import dev.arcovia.mitigation.sat.Literal;

public class Clause {
	protected final ArrayList<Literal> literals;
	
	public Clause() {
		this.literals = new ArrayList<Literal>();
	}
	
	public Clause(Clause clause) {
		this.literals = new ArrayList<Literal>(clause.literals);
	}
	
	public void add(Literal literal) {
		literals.add(literal);
	}
}
