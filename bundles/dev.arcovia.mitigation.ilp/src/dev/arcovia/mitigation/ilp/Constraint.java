package dev.arcovia.mitigation.ilp;

import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.result.DSLResult;

import dev.arcovia.mitigation.sat.CompositeLabel;
import dev.arcovia.mitigation.sat.IncomingDataLabel;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.LabelCategory;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

public class Constraint {
    private final AnalysisConstraint dsl;
    private EvaluationFunction evalFunction;
    private final List<MitigationStrategy> mitigations;

    public Constraint(AnalysisConstraint dsl, List<MitigationStrategy> mitigations) {
        this.dsl = dsl;
        this.evalFunction = this::getDSLViolations;
        this.mitigations = mitigations;
    }

    public Constraint(AnalysisConstraint dsl) {
        this.dsl = dsl;
        this.evalFunction = this::getDSLViolations;
        this.mitigations = determineMitigations();

    }
    
    public Constraint(List<MitigationStrategy> mitigations) {
    	this.dsl = null;
    	this.evalFunction = null;
    	this.mitigations = mitigations;
    }
    
    public void addEvalFunction(EvaluationFunction evaluate) {
        this.evalFunction = evaluate;
    }

    public List<MitigationStrategy> getMitigations() {
        return new ArrayList<>(mitigations);
    }

    public boolean isPrecondition(CompositeLabel label) {
    	if (dsl == null) return false;
        var translation = new CNFTranslation(dsl);
        var literals = translation.constructCNF()
                .get(0)
                .literals();

        for (var lit : literals) {
            if (!lit.positive() && lit.compositeLabel()
                    .equals(label))
                return true;
        }
        return false;
    }

    public void removeMitigation(MitigationStrategy mitgation) {
        mitigations.remove(mitgation);
    }
    
    public void findAlternativeMitigations() {
    	if (dsl == null) return;
    	
    	var mitigations = determineMitigations();
    	
    	for (var mitigation : mitigations) {
    		if (!this.mitigations.contains(mitigation)) this.mitigations.add(mitigation);
    	}
    		
    }
    
    public Set<Node> determineViolations(DFDFlowGraphCollection flowGraph){
    	return evalFunction.evaluate(flowGraph);
    }
    
    private Set<Node> getDSLViolations(DFDFlowGraphCollection flowGraph) {
    	Set<Node> violatingNodes = new HashSet<>();
    	List<DSLResult> results = this.dsl.findViolations(flowGraph);
        for (var result : results) {
            var tfg = result.getTransposeFlowGraph();
            for (var vertex : result.getMatchedVertices())
                violatingNodes.add(new Node((DFDVertex) vertex, tfg, this));
        }
        return violatingNodes;
    }

    /***
     * This functions determines whether a Node matches the Antecedent of this constraint.
     * @param node
     * @return
     */
    public boolean isMatched(DFDVertex node) {
    	//protection if no dsl is provided
        var translation = new CNFTranslation(dsl);
        List<String> negativeLiterals = new ArrayList<>();
        List<String> positiveLiterals = new ArrayList<>();
        for (var literal : translation.constructCNF()
                .get(0)
                .literals()) {
            if (literal.positive())
                positiveLiterals.add(literal.compositeLabel()
                        .toString());
            else
                negativeLiterals.add(literal.compositeLabel()
                        .toString());
        }

        Set<String> nodeLiterals = new HashSet<>();
        for (var nodeChar : node.getAllVertexCharacteristics()) {
            nodeLiterals.add(new NodeLabel(new Label(nodeChar.getTypeName(), nodeChar.getValueName())).toString());
        }
        for (var variables : node.getAllIncomingDataCharacteristics()) {
            for (var dataChar : variables.getAllCharacteristics()) {
                nodeLiterals.add(new IncomingDataLabel(new Label(dataChar.getTypeName(), dataChar.getValueName())).toString());
            }
        }

        if (nodeLiterals.containsAll(negativeLiterals))
            return true;

        return false;
    }

    private List<MitigationStrategy> determineMitigations() {
        var translation = new CNFTranslation(dsl);

        if (translation.constructCNF()
                .size() > 1) {
            System.out.println("this constraint is not supportet - yet");
        }

        var literals = translation.constructCNF()
                .get(0)
                .literals();

        List<MitigationStrategy> mitigations = new ArrayList<>();

        var neverFlows = dsl.getVertexDestinationSelectors()
                .getSelectors()
                .toString();

        for (var lit : literals) {
            if (lit.positive()) {
                MitigationType type;

                if (lit.compositeLabel()
                        .category() == LabelCategory.Node)
                    type = MitigationType.NodeLabel;
                else
                    type = MitigationType.DataLabel;

                mitigations.add(new MitigationStrategy(lit.compositeLabel(), 1, type));
            } else {
                var label = lit.compositeLabel()
                        .label()
                        .type() + "."
                        + lit.compositeLabel()
                                .label()
                                .value();

                if (neverFlows.contains(label)) {
                    MitigationType type;

                    if (lit.compositeLabel()
                            .category() == LabelCategory.Node)
                        type = MitigationType.DeleteNodeLabel;
                    else
                        type = MitigationType.DeleteDataLabel;

                    mitigations.add(new MitigationStrategy(lit.compositeLabel(), 1000, type));

                }

            }
        }

        return mitigations;
    }

}
