package dev.arcovia.mitigation.sat.cnf;

import java.util.List;

import dev.arcovia.mitigation.sat.Constraint;

public abstract class LogicNode {
	protected final LogicNodeDescriptor descriptor;
	
	public LogicNode(LogicNodeDescriptor descriptor) {
		this.descriptor = descriptor;
	}
	
	public abstract void collectCNFClauses(List<Constraint> result, List<Constraint> activeClauses);
}
