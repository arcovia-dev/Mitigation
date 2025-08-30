package dev.arcovia.mitigation.sat.dsl;

import java.util.*;

import dev.arcovia.mitigation.sat.*;
import dev.arcovia.mitigation.sat.dsl.nodes.ConjunctionNode;
import dev.arcovia.mitigation.sat.dsl.selectors.conditional.DynamicConditionalSelector;
import dev.arcovia.mitigation.sat.dsl.selectors.conditional.DynamicEmptySetOperationConditionalSelector;
import dev.arcovia.mitigation.sat.dsl.selectors.conditional.DynamicVariableConditionalSelector;
import dev.arcovia.mitigation.sat.dsl.selectors.constant.*;
import dev.arcovia.mitigation.sat.dsl.selectors.dynamic.DynamicDataCharacteristicSelector;
import dev.arcovia.mitigation.sat.dsl.selectors.dynamic.DynamicDataSelector;
import dev.arcovia.mitigation.sat.dsl.selectors.dynamic.DynamicVertexCharacteristicSelector;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.selectors.*;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import tools.mdsd.modelingfoundations.identifier.NamedElement;

public class CNFTranslation {
    private final AnalysisConstraint analysisConstraint;
    private final boolean hasOutgoingData;
    private final boolean hasIncomingData;

    private final List<ConstantDataSelector> constantSelectors =  new ArrayList<>();
    private final Map <String, DynamicDataSelector> dynamicSelectors =  new HashMap<>();
    private final List<DynamicConditionalSelector> conditionalSelectors =  new ArrayList<>();

    private final Map<String, List<String>> variables;

    private List<Constraint> cnf;
    private BaseFormula baseFormula;
    private BaseFormula conditionalFormula;
    private BaseFormula cnfFormula;

    public CNFTranslation(AnalysisConstraint analysisConstraint, Map<String, List<String>> variables) {
        this.analysisConstraint = Objects.requireNonNull(analysisConstraint);
        this.variables = Objects.requireNonNull(variables);
//        hasOutgoingData = !analysisConstraint.getVertexSourceSelectors().getSelectors().isEmpty();
//        hasIncomingData = !analysisConstraint.getVertexDestinationSelectors().getSelectors().isEmpty();
        hasOutgoingData = false;
        hasIncomingData = true;
    }

    public CNFTranslation(AnalysisConstraint analysisConstraint) {
        this(analysisConstraint, new HashMap<>());
    }

    public CNFTranslation(AnalysisConstraint analysisConstraint, DataFlowDiagramAndDictionary dataFlowDiagramAndDictionary) {
        this(analysisConstraint);
        initialiseVariables(dataFlowDiagramAndDictionary);
    }

    public List<Constraint> constructCNF() {
        initialiseTranslation();
        var root = new ConjunctionNode();
        cnfFormula = new BaseFormula(root);
        root.addPredicate(baseFormula.getRoot());
        root.addPredicate(conditionalFormula.getRoot());
        cnf = cnfFormula.toCNF();
        return cnf;
    }

    public void initialiseTranslation() {
        List<AbstractSelector> selectors = new ArrayList<>();
        selectors.addAll(analysisConstraint.getDataSourceSelectors().getSelectors());
        selectors.addAll(analysisConstraint.getVertexSourceSelectors().getSelectors());
        selectors.addAll(analysisConstraint.getVertexDestinationSelectors().getSelectors());

        selectors.forEach(this::initialiseSelector);
        analysisConstraint.getConditionalSelectors().getSelectors().forEach(this::initialiseConditionalSelector);

        constructBaseFormula();
        constructConditionalFormula();
    }

    private void initialiseSelector(AbstractSelector selector) {
        if (selector instanceof DataCharacteristicsSelector dataCharacteristicsSelector) {
            var value = dataCharacteristicsSelector.getDataCharacteristic().characteristicValue();
            if (value.isConstant()) {
                constantSelectors.add(new ConstantDataCharacteristicSelector(dataCharacteristicsSelector));
            } else {
                dynamicSelectors.put(value.name(), new DynamicDataCharacteristicSelector(dataCharacteristicsSelector));
            }
            return;
        }
        if (selector instanceof VertexCharacteristicsSelector vertexCharacteristicsSelector) {
            var value = vertexCharacteristicsSelector.getVertexCharacteristics().characteristicValue();
            if (value.isConstant()) {
                constantSelectors.add(new ConstantVertexCharacteristicSelector(vertexCharacteristicsSelector));
            } else {
                dynamicSelectors.put(value.name(), new DynamicVertexCharacteristicSelector(vertexCharacteristicsSelector));
            }
            return;
        }
        if (selector instanceof DataCharacteristicListSelector dataCharacteristicListSelector) {
            constantSelectors.add(new ConstantDataCharacteristicListSelector(dataCharacteristicListSelector));
            return;
        }
        if (selector instanceof VertexCharacteristicsListSelector vertexCharacteristicsListSelector) {
            constantSelectors.add(new ConstantVertexCharacteristicListSelector(vertexCharacteristicsListSelector));
            return;
        }
        throw new IllegalArgumentException("Unexpected selector type: " + selector);
    }

    private void initialiseConditionalSelector(ConditionalSelector conditionalSelector) {
        if(conditionalSelector instanceof VariableConditionalSelector variableConditionalSelector) {
            conditionalSelectors.add(new DynamicVariableConditionalSelector(variableConditionalSelector));
            return;
        }
        if(conditionalSelector instanceof EmptySetOperationConditionalSelector emptySetOperationConditionalSelector) {
            conditionalSelectors.add(new DynamicEmptySetOperationConditionalSelector(emptySetOperationConditionalSelector));
            return;
        }
        throw new IllegalArgumentException("Unexpected conditional selector type: " + conditionalSelector);
    }

    private void constructBaseFormula() {
        var root = new ConjunctionNode();
        baseFormula = new BaseFormula(root);
        constantSelectors.forEach(it -> it.addLiterals(root, hasOutgoingData, hasIncomingData));
    }

    private void constructConditionalFormula() {
        var root = new ConjunctionNode();
        conditionalFormula = new BaseFormula(root);

        if (conditionalSelectors.isEmpty()) { return; }
        if (variables.isEmpty()) { throw new IllegalStateException("Variables are empty."); }

        conditionalSelectors.forEach(it -> it.addLiterals(
                root,
                dynamicSelectors,
                variables,
                hasOutgoingData,
                hasIncomingData
        ));
    }

    private void initialiseVariables(DataFlowDiagramAndDictionary dfd){
        dfd.dataDictionary().getLabelTypes().forEach(it -> variables.put(
                it.getEntityName(),
                it.getLabel().stream().map(NamedElement::getEntityName).toList()));
    }

    public String formulaToString() {
        var formulaString = cnfFormula.toString();
        return "\n!%s".formatted(formulaString);
    }

    public String cnfToString() {
        var s = new StringBuilder();
        s.append("\n");
        for (var constraint : cnf) {
            s.append("( ");
            for (var literal : constraint.literals()) {
                var positive = literal.positive() ? "" : "!";
                var label = literal.compositeLabel().label();
                s.append("%s[%s.%s]".formatted(positive, label.type(), label.value()));
//                s.append("%s[%s %s.%s]".formatted(positive, literal.compositeLabel().category().name(), label.type(), label.value()));
                s.append(" OR ");
            }
            s.delete(s.length() - 3, s.length());
            s.append(") AND");
            s.append("\n");
        }
        s.delete(s.length() - 5, s.length());
        return s.toString();
    }

    public String simpleCNFToString() {
        var s = new StringBuilder();
        var literals = new ArrayList<Literal>();
        s.append("\n");
        for (var constraint : cnf) {
            s.append("[");
            for (var literal : constraint.literals()) {
                if (literals.contains(literal)) {
                    s.append(literal.positive() ? "" : "!");
                    s.append(literals.indexOf(literal));
                    s.append(" ");
                } else {
                    s.append(literal.positive() ? "" : "!").append(literals.size()).append(" ");
                    literals.add(literal);
                }
            }
            s.append("]");
            s.append("\n");
        }
        return s.toString();
    }

    public String getCNFStatistics() {
        return "\n" +
                "Clauses: " + outputClauses() + "\n" +
                "Literals: " + outputLiterals() + "\n" +
                "Longest Clause: " + outputLongestClause() + "\n" +
                "Literals per Clause (avg): " + (float) (outputLiterals()) / outputClauses() + "\n";
    }

    public int outputClauses() {
        return cnf.size();
    }

    public int outputLiterals() {
        return cnf.stream().map(it -> it.literals().size()).reduce(0, Integer::sum);
    }

    public int outputLongestClause() {
        var longest = 0;
        for (Constraint constraint : cnf) {
            var len =  constraint.literals().size();
            if(len > longest) {
                longest = len;
            }
        }
        return longest;
    }
}