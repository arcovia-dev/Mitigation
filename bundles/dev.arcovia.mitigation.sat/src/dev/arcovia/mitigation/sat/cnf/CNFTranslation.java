package dev.arcovia.mitigation.sat.cnf;

import java.util.*;

import dev.arcovia.mitigation.sat.*;
import dev.arcovia.mitigation.sat.cnf.nodes.ConjunctionNode;
import dev.arcovia.mitigation.sat.cnf.selectors.conditional.DynamicConditionalSelector;
import dev.arcovia.mitigation.sat.cnf.selectors.conditional.DynamicEmptySetOperationConditionalSelector;
import dev.arcovia.mitigation.sat.cnf.selectors.conditional.DynamicVariableConditionalSelector;
import dev.arcovia.mitigation.sat.cnf.selectors.constant.*;
import dev.arcovia.mitigation.sat.cnf.selectors.dynamic.DynamicDataCharacteristicSelector;
import dev.arcovia.mitigation.sat.cnf.selectors.dynamic.DynamicDataSelector;
import dev.arcovia.mitigation.sat.cnf.selectors.dynamic.DynamicVertexCharacteristicSelector;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.selectors.*;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import tools.mdsd.modelingfoundations.identifier.NamedElement;

public class CNFTranslation {
    private final AnalysisConstraint analysisConstraint;
    private final DataFlowDiagramAndDictionary  dataFlowDiagramAndDictionary;
    private final boolean hasOutgoingData;
    private final boolean hasIncomingData;
    private boolean initialized = false;

    private final List<ConstantDataSelector> constantSelectors =  new ArrayList<>();
    private final Map <String, DynamicDataSelector> dynamicSelectors =  new HashMap<>();
    private final List<DynamicConditionalSelector> conditionalSelectors =  new ArrayList<>();

    private final Map<String, List<String>> variables = new HashMap<>();

    private List<Constraint> cnf;
    private BaseFormula baseFormula;
    private BaseFormula conditionalFormula;


    public CNFTranslation(AnalysisConstraint analysisConstraint, DataFlowDiagramAndDictionary  dataFlowDiagramAndDictionary) {
        this.analysisConstraint = analysisConstraint;
        this.dataFlowDiagramAndDictionary = dataFlowDiagramAndDictionary;
        hasOutgoingData = !analysisConstraint.getVertexSourceSelectors().getSelectors().isEmpty();
        hasIncomingData = !analysisConstraint.getVertexDestinationSelectors().getSelectors().isEmpty();
    }

    public List<Constraint> constructCNF() {
        initialiseTranslation();
        cnf = new ArrayList<>();
        var baseCNF = baseFormula.toCNF();
        var conditionalCNF = conditionalFormula.toCNF();

        if (baseCNF != null && !baseCNF.isEmpty()) {
            cnf.addAll(baseCNF);
        }
        if (conditionalCNF != null && !conditionalCNF.isEmpty()) {
            cnf.addAll(conditionalCNF);
        }
        return cnf;
    }

    public void initialiseTranslation() {
        if(initialized) return;

        List<AbstractSelector> selectors = new ArrayList<>();
        selectors.addAll(analysisConstraint.getDataSourceSelectors().getSelectors());
        selectors.addAll(analysisConstraint.getVertexSourceSelectors().getSelectors());
        selectors.addAll(analysisConstraint.getVertexDestinationSelectors().getSelectors());

        selectors.forEach(this::initialiseSelector);
        analysisConstraint.getConditionalSelectors().getSelectors().forEach(this::initialiseConditionalSelector);

        constructBaseFormula();
        constructConditionalFormula();
        initialized = true;
    }

    private void initialiseSelector(AbstractSelector selector) {
        if (selector instanceof DataCharacteristicsSelector dataCharacteristicsSelector) {
            var value = dataCharacteristicsSelector.getCharacteristicsSelectorData().characteristicValue();
            if (value.isConstant()) {
                constantSelectors.add(new ConstantDataCharacteristicSelector(dataCharacteristicsSelector));
            } else {
                dynamicSelectors.put(value.name(), new DynamicDataCharacteristicSelector(dataCharacteristicsSelector));
            }
            return;
        }
        if (selector instanceof VertexCharacteristicsSelector vertexCharacteristicsSelector) {
            var value = vertexCharacteristicsSelector.getCharacteristicsSelectorData().characteristicValue();
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

        initialiseVariables(dataFlowDiagramAndDictionary);
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
        var baseString = baseFormula.toString();
        var conditionalString = conditionalFormula.toString();

        var s = new StringBuilder();
        s.append("\n");
        s.append("!");
        s.append(baseString);
        if (!baseString.isEmpty() && !conditionalString.isEmpty()) {
            s.append("\nAND\n");
        }
        s.append(conditionalString);
        return s.toString();
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
        StringBuilder s = new StringBuilder();
        s.append("\n");

        var literalCount = cnf.stream().map(it -> it.literals().size()).reduce(0, Integer::sum);

        s.append("Clauses: ").append(cnf.size()).append("\n");
        s.append("Literals: ").append(literalCount).append("\n");

        var longest = 0;
        var totalLiterals = 0;
        for (Constraint constraint : cnf) {
            var len =  constraint.literals().size();
            if(len > longest) {
                longest = len;
            }
            totalLiterals += len;
        }

        s.append("Longest Clause: ").append(longest).append("\n");
        s.append("Total Literals: ").append(totalLiterals).append("\n");
        s.append("Literals per Clause (avg): ").append((float) (totalLiterals) / cnf.size()).append("\n");
        return s.toString();
    }
}