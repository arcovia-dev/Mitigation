package dev.arcovia.mitigation.sat.cnf;

import java.util.ArrayList;

import org.dataflowanalysis.analysis.DataFlowConfidentialityAnalysis;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;

public class CNFTranslation {
	protected final AnalysisConstraint constraint;
	protected DataFlowConfidentialityAnalysis analysis;
	protected ArrayList<Clause> cnf;
	protected BaseFormula dataFormula;
	protected BaseFormula nodeFormula;
	protected BaseFormula variableFormula;
	
	public CNFTranslation(AnalysisConstraint constraint) {
		this.constraint = constraint;
	}
	
	private void constructDataFormula() {
		LogicNode root = new LogicNode(LogicNodeDescriptor.CONJUNCTION);
		
		
		
		BaseFormula formula = new BaseFormula(null);
	}
	
	private void constructNodeFormula() {
		
	}
	
	private void constructVariableFormula() {
		
	}
	
	private void constructCNF() {
		
	}
}
