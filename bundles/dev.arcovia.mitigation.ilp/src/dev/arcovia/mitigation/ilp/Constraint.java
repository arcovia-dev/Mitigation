package dev.arcovia.mitigation.ilp;

import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.result.DSLResult;

import dev.arcovia.mitigation.sat.CompositeLabel;
import dev.arcovia.mitigation.sat.Literal;
import dev.arcovia.mitigation.sat.IncomingDataLabel;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.LabelCategory;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.utils.dsl.CNFTranslation;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

public class Constraint {
	private final AnalysisConstraint dslConstraint;
	private EvaluationFunction evaluationFunction;
	private final List<MitigationStrategy> mitigations;
	private List<CompositeLabel> preconditionLabel = new ArrayList<>();

	public Constraint(AnalysisConstraint dsl, List<MitigationStrategy> mitigations) {
		this.dslConstraint = dsl;
		this.evaluationFunction = new EvaluationFunction() {
			@Override
			public Set<Node> evaluate(DFDFlowGraphCollection flowGraph) {
				return getDSLViolations(flowGraph);
			}

			@Override
			public boolean isMatched(DFDVertex vertex) {
				return DSLIsMatched(vertex);
			}
		};
		this.mitigations = mitigations;
	}

	public Constraint(AnalysisConstraint dslConstraint) {
		this.dslConstraint = dslConstraint;
		this.evaluationFunction = new EvaluationFunction() {
			@Override
			public Set<Node> evaluate(DFDFlowGraphCollection flowGraph) {
				return getDSLViolations(flowGraph);
			}

			@Override
			public boolean isMatched(DFDVertex vertex) {
				return DSLIsMatched(vertex);
			}
		};
		this.mitigations = determineMitigations();

	}

	public Constraint(List<MitigationStrategy> mitigations) {
		this.dslConstraint = null;
		this.evaluationFunction = null;
		this.mitigations = mitigations;
	}

	public void addEvalFunction(EvaluationFunction evaluationFunction) {
		this.evaluationFunction = evaluationFunction;
	}

	public List<MitigationStrategy> getMitigations() {
		return new ArrayList<>(mitigations);
	}

	public boolean isPrecondition(CompositeLabel label) {
		if (dslConstraint == null) {
			return preconditionLabel.contains(label);
		}
		var translation = new CNFTranslation(dslConstraint);
		var literals = translation.constructCNF().get(0).literals();

		for (var literal : literals) {
			if (!literal.positive() && literal.compositeLabel().equals(label)) {
				return true;
			}
		}
		return false;
	}

	public void addPrecondition(CompositeLabel label) {
		preconditionLabel.add(label);
	}

	public void removeMitigation(MitigationStrategy mitgation) {
		mitigations.remove(mitgation);
	}

	public void findAlternativeMitigations() {
		if (dslConstraint == null) {
			return;
		}

		var mitigations = determineMitigations();

		for (var mitigation : mitigations) {
			if (!this.mitigations.contains(mitigation)) {
				this.mitigations.add(mitigation);
			}
		}

	}

	public Set<Node> determineViolations(DFDFlowGraphCollection flowGraph) {
		return evaluationFunction.evaluate(flowGraph);
	}

	private Set<Node> getDSLViolations(DFDFlowGraphCollection flowGraph) {
		Set<Node> violatingNodes = new HashSet<>();
		List<DSLResult> results = this.dslConstraint.findViolations(flowGraph);
		for (var result : results) {
			var tfg = result.getTransposeFlowGraph();
			for (var vertex : result.getMatchedVertices()) {
				violatingNodes.add(new Node((DFDVertex) vertex, tfg, this));
			}
		}
		return violatingNodes;
	}

	/***
	 * This functions determines whether a Node matches the Antecedent of this
	 * constraint.
	 * 
	 * @param node
	 * @return
	 */
	public boolean isMatched(DFDVertex node) {
		return evaluationFunction.isMatched(node);
	}

	private boolean DSLIsMatched(DFDVertex node) {
		// protection if no dsl is provided
		if (dslConstraint == null) {
			return false;
		}

		var translation = new CNFTranslation(dslConstraint);
		List<String> negativeLiterals = new ArrayList<>();
		List<String> positiveLiterals = new ArrayList<>();
		for (var literal : translation.constructCNF().get(0).literals()) {
			if (literal.positive()) {
				positiveLiterals.add(literal.compositeLabel().toString());
			} else {
				negativeLiterals.add(literal.compositeLabel().toString());
			}
		}

		Set<String> nodeLiterals = new HashSet<>();
		for (var nodeChar : node.getAllVertexCharacteristics()) {
			nodeLiterals.add(new NodeLabel(new Label(nodeChar.getTypeName(), nodeChar.getValueName())).toString());
		}
		for (var variables : node.getAllIncomingDataCharacteristics()) {
			for (var dataChar : variables.getAllCharacteristics()) {
				nodeLiterals.add(
						new IncomingDataLabel(new Label(dataChar.getTypeName(), dataChar.getValueName())).toString());
			}
		}

		return nodeLiterals.containsAll(negativeLiterals);
	}

	private List<MitigationStrategy> determineMitigations() {
		var translation = new CNFTranslation(dslConstraint);

		Set<Literal> literals = new HashSet<>();

		for (var literal : translation.constructCNF()) {
			literals.addAll(literal.literals());
		}

		List<MitigationStrategy> mitigations = new ArrayList<>();

		var neverFlows = dslConstraint.getVertexDestinationSelectors().getSelectors().toString();

		for (var literal : literals) {
			if (literal.positive()) {
				MitigationType type;

				if (literal.compositeLabel().category() == LabelCategory.Node) {
					type = MitigationType.NodeLabel;
				} else {
					type = MitigationType.DataLabel;
				}

				mitigations.add(new MitigationStrategy(List.of(literal.compositeLabel()), 1, type));
			} else {
				var label = literal.compositeLabel().label().type() + "." + literal.compositeLabel().label().value();

				if (neverFlows.contains(label)) {
					MitigationType type;

					if (literal.compositeLabel().category() == LabelCategory.Node) {
						type = MitigationType.DeleteNodeLabel;
					} else {
						type = MitigationType.DeleteDataLabel;
					}

					mitigations.add(new MitigationStrategy(List.of(literal.compositeLabel()), 1000, type));

				}

			}
		}

		return mitigations;
	}

}
