package dev.arcovia.mitigation.sat.cnf;

import java.util.ArrayList;

import org.dataflowanalysis.analysis.DataFlowConfidentialityAnalysis;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.selectors.AbstractSelector;
import org.dataflowanalysis.analysis.dsl.selectors.CharacteristicsSelectorData;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicListSelector;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicsSelector;

import dev.arcovia.mitigation.sat.Constraint;
import dev.arcovia.mitigation.sat.IncomingDataLabel;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.Literal;

public class CNFTranslation {
	protected final AnalysisConstraint analysisConstraint;
	protected DataFlowConfidentialityAnalysis analysis;
	protected ArrayList<Constraint> cnf;
	protected BaseFormula dataFormula;
	protected BaseFormula nodeFormula;
	protected BaseFormula variableFormula;
	
	public CNFTranslation(AnalysisConstraint analysisConstraint) {
		this.analysisConstraint = analysisConstraint;
	}
	
	//TODO check for vertexSourceSelectors
	
	private void constructDataFormula() {
		var root = new ConjunctionNode();
		dataFormula = new BaseFormula(root);
		
		var selectors = analysisConstraint.getDataSourceSelectors().getSelectors();
		
		for (AbstractSelector selector: selectors) {
			// Java 17 implementation for pattern matching syntax of class instance, later version enable this with switch
			
			if (selector instanceof DataCharacteristicsSelector dcs) {
				var type = dcs.getDataCharacteristic().characteristicType();
				var value = dcs.getDataCharacteristic().characteristicValue();
				
				if(!type.isConstant()) throw new IllegalStateException("Selector Type not constant:" + selector);
				if(!value.isConstant()) throw new IllegalStateException("Selector Value not constant:" + selector); // maybe this is for WHERE
				
				root.addPredicate(
						new LiteralNode(
								new Literal(
										!dcs.isInverted(), // this gets inverted once for the resulting CNF
										new IncomingDataLabel(
												new Label(
													type.values().get().get(0),
													value.values().get().get(0)
														)
												)
										)
								)
						);
				continue;
			}
			
			if (selector instanceof DataCharacteristicListSelector dcls) {
				// expecting a LIST(OR) like ( a OR b OR ...)
				var disjunctionNode = new DisjunctionNode();
				root.addPredicate(disjunctionNode);
				
				for (CharacteristicsSelectorData csd: dcls.getDataCharacteristic()) {
					
					var type = csd.characteristicType();
					var value = csd.characteristicValue();
					
					if(!type.isConstant()) throw new IllegalStateException("Selector Type not constant:" + selector);
					if(!value.isConstant()) throw new IllegalStateException("Selector Value not constant:" + selector); // maybe this is for WHERE
					
					disjunctionNode.addPredicate(
							new LiteralNode(
									new Literal(
											dcls.isInverted(), // this gets double inverted, once for NOT ( a OR b ) and once for resulting CNF
											new IncomingDataLabel(
													new Label(
														csd.characteristicType().toString(),
														csd.characteristicValue().toString()
															)
													)
											)
									)
							);
					continue;
				}
			}
			
			throw new IllegalStateException("Unexpected type:" + selector);
		}
	}
	
	private void constructNodeFormula() {
		var root = new ConjunctionNode();
		nodeFormula = new BaseFormula(root);
		
		var selectors = analysisConstraint.getVertexDestinationSelectors().getSelectors();
		
		for (AbstractSelector selector: selectors) {
			// Java 17 implementation for pattern matching syntax of class instance, later version enable this with switch
			
			if (selector instanceof DataCharacteristicsSelector dcs) {
				root.addPredicate(
						new LiteralNode(
								new Literal(
										!dcs.isInverted(), 
										new IncomingDataLabel(
												new Label(
													dcs.getDataCharacteristic().characteristicType().toString(),
													dcs.getDataCharacteristic().characteristicValue().toString()
														)
												)
										)
								)
						);
				continue;
			}
			if (selector instanceof DataCharacteristicListSelector dcls) {
				var disjunctionNode = new DisjunctionNode();
				for (CharacteristicsSelectorData csd: dcls.getDataCharacteristic())
				disjunctionNode.addPredicate(
						new LiteralNode(
								new Literal(
										!dcls.isInverted(), 
										new IncomingDataLabel(
												new Label(
													csd.characteristicType().toString(),
													csd.characteristicValue().toString()
														)
												)
										)
								)
						);
				root.addPredicate(disjunctionNode);
				continue;
			}
			
			throw new IllegalStateException("Unexpected type:" + selector);
		}
	}
	
	private void constructVariableFormula() {
		
	}
	

	private void constructCNF() {
		
	}
}
