package dev.arcovia.mitigation.sat.cnf;

import java.util.ArrayList;

import org.dataflowanalysis.analysis.DataFlowConfidentialityAnalysis;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.selectors.*;

import dev.arcovia.mitigation.sat.Constraint;

public class CNFTranslation {
	protected final AnalysisConstraint analysisConstraint;
	protected DataFlowConfidentialityAnalysis analysis;
	protected ArrayList<Constraint> cnf;
    protected BaseFormula baseFormula;

	public CNFTranslation(AnalysisConstraint analysisConstraint) {
		this.analysisConstraint = analysisConstraint;
	}

    private void constructBaseFormula() {
        var root = new ConjunctionNode();
        baseFormula = new BaseFormula(root);

        var hasOutgoingData = !analysisConstraint.getVertexSourceSelectors().getSelectors().isEmpty();
        var selectors = new ArrayList<AbstractSelector>();
        selectors.addAll(analysisConstraint.getDataSourceSelectors().getSelectors());
        selectors.addAll(analysisConstraint.getVertexSourceSelectors().getSelectors());
        selectors.addAll(analysisConstraint.getVertexDestinationSelectors().getSelectors());

        for (AbstractSelector selector: selectors) {
            // Java 17 implementation for pattern matching syntax of class instance, later version enable this with switch

            if (selector instanceof DataCharacteristicsSelector dataCharacteristicsSelector) {
                addDataCharacteristicSelector(root, dataCharacteristicsSelector, hasOutgoingData);
                continue;
            }

            if (selector instanceof DataCharacteristicListSelector dataCharacteristicListSelector) {
                addDataCharacteristicListSelector(root, dataCharacteristicListSelector, hasOutgoingData);
                continue;
            }

            if (selector instanceof VertexCharacteristicsSelector vertexCharacteristicsSelector) {
                addVertexCharacteristicsSelector(root, vertexCharacteristicsSelector);
                continue;
            }

            if (selector instanceof VertexCharacteristicsListSelector vertexCharacteristicsListSelector) {
                addVertexCharacteristicsListSelector(root, vertexCharacteristicsListSelector);
                continue;
            }

            throw new IllegalStateException("Unexpected type:" + selector);
        }
    }

    private void addDataCharacteristicSelector(BranchNode root, DataCharacteristicsSelector dataCharacteristicsSelector, boolean hasOutgoingData) {
        root.addPredicate(LiteralNode.ofIncomingData(dataCharacteristicsSelector));
        if (hasOutgoingData) {
            root.addPredicate(LiteralNode.ofOutgoingData(dataCharacteristicsSelector));
        }
    }

    private void addDataCharacteristicListSelector(BranchNode root, DataCharacteristicListSelector dataCharacteristicListSelector, boolean hasOutgoingData) {
        var inverted = dataCharacteristicListSelector.isInverted();
        var characteristicsSelectorDataList = dataCharacteristicListSelector.getCharacteristicsSelectorDataList();
        var node = inverted ? new ConjunctionNode() : new DisjunctionNode();
        root.addPredicate(node);

        for (CharacteristicsSelectorData characteristicsSelectorData: characteristicsSelectorDataList) {
            var dataCharacteristicsSelector = new DataCharacteristicsSelector(null, characteristicsSelectorData, inverted);
            addDataCharacteristicSelector(node, dataCharacteristicsSelector, hasOutgoingData);
        }
    }

    private void addVertexCharacteristicsSelector(BranchNode root, VertexCharacteristicsSelector vertexCharacteristicsSelector) {
        root.addPredicate(LiteralNode.ofNode(vertexCharacteristicsSelector));
    }

    private void addVertexCharacteristicsListSelector(BranchNode root, VertexCharacteristicsListSelector vertexCharacteristicsListSelector) {
        var inverted = vertexCharacteristicsListSelector.isInverted();
        var characteristicsSelectorDataList = vertexCharacteristicsListSelector.getCharacteristicsSelectorDataList();
        var node = inverted ? new ConjunctionNode() : new DisjunctionNode();
        root.addPredicate(node);

        for (CharacteristicsSelectorData characteristicsSelectorData: characteristicsSelectorDataList) {
            var vertexCharacteristicsSelector = new VertexCharacteristicsSelector(null, characteristicsSelectorData, inverted);
            addVertexCharacteristicsSelector(node, vertexCharacteristicsSelector);
        }
    }
	
	private void constructVariableFormula() {
		
	}
	

	private void constructCNF() {
		
	}
}
