package dev.arcovia.mitigation.sat.cnf;

import java.util.*;
import java.util.stream.Stream;

import dev.arcovia.mitigation.sat.*;
import dev.arcovia.mitigation.sat.cnf.nodes.BranchNode;
import dev.arcovia.mitigation.sat.cnf.nodes.ConjunctionNode;
import dev.arcovia.mitigation.sat.cnf.nodes.DisjunctionNode;
import dev.arcovia.mitigation.sat.cnf.nodes.LiteralNode;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.selectors.*;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import tools.mdsd.modelingfoundations.identifier.NamedElement;

public class CNFTranslation {
    private final AnalysisConstraint analysisConstraint;
    private final DataFlowDiagramAndDictionary  dataFlowDiagramAndDictionary;
    private final List<AbstractSelector> constantSelectors =  new ArrayList<>();
    private final Map <String, AbstractSelector> dynamicSelectors =  new HashMap<>();
    private final List<ConditionalSelector> conditionalSelectors =  new ArrayList<>();
    private final boolean hasOutgoingData;
    private final Map<String, List<String>> variables = new HashMap<>();
    private final Map<String, List<String>> conditionalVariables = new HashMap<>();
    private List<Constraint> cnf;
    private BaseFormula baseFormula;
    private BaseFormula conditionalFormula;
    private boolean initialized = false;

	public CNFTranslation(AnalysisConstraint analysisConstraint, DataFlowDiagramAndDictionary  dataFlowDiagramAndDictionary) {
        this.analysisConstraint = analysisConstraint;
        this.dataFlowDiagramAndDictionary = dataFlowDiagramAndDictionary;
        hasOutgoingData = !analysisConstraint.getVertexSourceSelectors().getSelectors().isEmpty();
	}

    public void constructCNF() {
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
    }

    public void initialiseTranslation() {
        if(initialized) return;

        List<AbstractSelector> selectors = new ArrayList<>();
        selectors.addAll(analysisConstraint.getDataSourceSelectors().getSelectors());
        selectors.addAll(analysisConstraint.getVertexSourceSelectors().getSelectors());
        selectors.addAll(analysisConstraint.getVertexDestinationSelectors().getSelectors());
        conditionalSelectors.addAll(analysisConstraint.getConditionalSelectors().getSelectors());

        selectors.forEach(this::initializeSelector);

        constructBaseFormula();
        constructConditionalFormula();
        initialized = true;
    }

    private void initializeSelector(AbstractSelector selector) {
        if (selector instanceof DataCharacteristicsSelector dataCharacteristicsSelector) {
            var value = dataCharacteristicsSelector.getCharacteristicsSelectorData().characteristicValue();
            if (value.isConstant()) {
                constantSelectors.add(dataCharacteristicsSelector);
            } else {
                dynamicSelectors.put(value.name(), dataCharacteristicsSelector);
            }
            return;
        }
        if (selector instanceof VertexCharacteristicsSelector vertexCharacteristicsSelector) {
            var value = vertexCharacteristicsSelector.getCharacteristicsSelectorData().characteristicValue();
            if (value.isConstant()) {
                constantSelectors.add(vertexCharacteristicsSelector);
            } else {
                dynamicSelectors.put(value.name(), vertexCharacteristicsSelector);
            }
            return;
        }
        if (selector instanceof DataCharacteristicListSelector
                || selector instanceof VertexCharacteristicsListSelector) {
            constantSelectors.add(selector);
            return;
        }
        throw new IllegalArgumentException("Unexpected selector type: " + selector);
    }

    private void constructBaseFormula() {
        var root = new ConjunctionNode();
        baseFormula = new BaseFormula(root);
        var outgoingData = new ConjunctionNode();
        if (hasOutgoingData) { root.addPredicate(outgoingData); }

        for (var selector: constantSelectors) {
            // Java 17 implementation for pattern matching syntax, later version enable this with switch

            // data AND single
            if (selector instanceof DataCharacteristicsSelector dataCharacteristicsSelector) {
                var labels = getLabels(dataCharacteristicsSelector.getCharacteristicsSelectorData());
                addIncomingDataLabels(root, labels, dataCharacteristicsSelector.isInverted());
                addOutgoingDataLabels(outgoingData, labels, dataCharacteristicsSelector.isInverted());
                continue;
            }

            // data OR list
            if (selector instanceof DataCharacteristicListSelector dataCharacteristicListSelector) {
                var inverted = dataCharacteristicListSelector.isInverted();
                var characteristicsSelectorDataList = dataCharacteristicListSelector.getCharacteristicsSelectorDataList();
                var node = inverted ? new ConjunctionNode() : new DisjunctionNode();
                var outGoingDataNode = inverted ? new ConjunctionNode() : new DisjunctionNode();
                root.addPredicate(node);
                outgoingData.addPredicate(outGoingDataNode);

                for (var characteristicsSelectorData: characteristicsSelectorDataList) {
                    var dataCharacteristicsSelector = new DataCharacteristicsSelector(null, characteristicsSelectorData, inverted);
                    var labels =  getLabels(dataCharacteristicsSelector.getCharacteristicsSelectorData());
                    addIncomingDataLabels(node, labels, inverted);
                    addOutgoingDataLabels(outGoingDataNode, labels, inverted);
                }
                continue;
            }

            // node AND single
            if (selector instanceof VertexCharacteristicsSelector vertexCharacteristicsSelector) {
                var labels = getLabels(vertexCharacteristicsSelector.getCharacteristicsSelectorData());
                addNodeLabels(root, labels, vertexCharacteristicsSelector.isInverted());
                continue;
            }

            // node OR list
            if (selector instanceof VertexCharacteristicsListSelector vertexCharacteristicsListSelector) {
                var inverted = vertexCharacteristicsListSelector.isInverted();
                var characteristicsSelectorDataList = vertexCharacteristicsListSelector.getCharacteristicsSelectorDataList();
                var node = inverted ? new ConjunctionNode() : new DisjunctionNode();
                root.addPredicate(node);

                for (var characteristicsSelectorData: characteristicsSelectorDataList) {
                    var vertexCharacteristicsSelector = new VertexCharacteristicsSelector(null, characteristicsSelectorData, inverted);
                    var labels = getLabels(vertexCharacteristicsSelector.getCharacteristicsSelectorData());
                    addNodeLabels(node, labels, inverted);
                }
                continue;
            }

            throw new IllegalStateException("Unexpected type:" + selector);
        }
    }



    private void constructConditionalFormula() {
        var root = new ConjunctionNode();
        conditionalFormula = new BaseFormula(root);
        initialiseVariables(dataFlowDiagramAndDictionary);

        for (var selector: conditionalSelectors) {

            if (selector instanceof VariableConditionalSelector variableConditionalSelector) {

                var variableReference = variableConditionalSelector.getConstraintVariable().name();
                var dynamicSelector = dynamicSelectors.get(variableReference);
                conditionalVariables.put(variableReference, variables.get(variableReference));
                var inverted = variableConditionalSelector.isInverted();
                var node = inverted ? new ConjunctionNode() : new DisjunctionNode();
                var outgoingDataNode = inverted ? new ConjunctionNode() : new DisjunctionNode();
                root.addPredicate(node);
                if (hasOutgoingData) {
                    root.addPredicate(outgoingDataNode);
                }


                if (dynamicSelector instanceof DataCharacteristicsSelector dataCharacteristicsSelector) {
                    if (dataCharacteristicsSelector.isInverted()) {
                        throw new IllegalStateException("CharacteristicSelector must be positive");
                    }
                    var labels = getLabels(dataCharacteristicsSelector.getCharacteristicsSelectorData());
                    addIncomingDataLabels(node, labels, inverted);
                    addOutgoingDataLabels(outgoingDataNode, labels, inverted);
                    continue;
                }

                if (dynamicSelector instanceof VertexCharacteristicsSelector vertexCharacteristicsSelector) {
                    if (vertexCharacteristicsSelector.isInverted()) {
                        throw new IllegalStateException("CharacteristicSelector must be positive");
                    }
                    var labels = getLabels(vertexCharacteristicsSelector.getCharacteristicsSelectorData());
                    addNodeLabels(node, labels, inverted);
                    continue;
                }

                throw new IllegalArgumentException("Unexpected VariableConditionalSelector type:" + selector);
            }

            if (selector instanceof EmptySetOperationConditionalSelector emptySetOperationConditionalSelector) {
                var setOperation = emptySetOperationConditionalSelector.getSetOperation();

                if (setOperation instanceof Intersection intersection) {
                    var firstVariableReference = intersection.getFirstVariable().name();
                    var secondVariableReference = intersection.getSecondVariable().name();
                    var firstDynamicSelector = dynamicSelectors.get(firstVariableReference);
                    var secondDynamicSelector = dynamicSelectors.get(secondVariableReference);
                    var variableSet = Stream.concat(
                                    variables.get(firstVariableReference).stream(),
                                    variables.get(secondVariableReference).stream())
                                    .distinct().toList();

                    if (variableSet.isEmpty()) { throw new IllegalStateException("No Intersection found."); }

                    conditionalVariables.put(firstVariableReference, variableSet);
                    conditionalVariables.put(secondVariableReference, variableSet);

                    if (firstDynamicSelector instanceof DataCharacteristicsSelector dataCharacteristicsSelector &&
                            secondDynamicSelector instanceof VertexCharacteristicsSelector vertexCharacteristicsSelector
                    ) {
                        var dataLabels = getLabels(dataCharacteristicsSelector.getCharacteristicsSelectorData());
                        var nodeLabels = getLabels(vertexCharacteristicsSelector.getCharacteristicsSelectorData());

                        addIntersectionLabels(root, dataLabels, nodeLabels);
                        continue;
                    }

                    if (firstDynamicSelector instanceof VertexCharacteristicsSelector vertexCharacteristicsSelector &&
                            secondDynamicSelector instanceof DataCharacteristicsSelector dataCharacteristicsSelector
                    ) {
                        var dataLabels = getLabels(dataCharacteristicsSelector.getCharacteristicsSelectorData());
                        var nodeLabels = getLabels(vertexCharacteristicsSelector.getCharacteristicsSelectorData());

                        addIntersectionLabels(root, dataLabels, nodeLabels);
                        continue;
                    }

                    throw new IllegalArgumentException("Unexpected dynamic selectors" + firstDynamicSelector + secondDynamicSelector);
                }

                throw new IllegalArgumentException("Unexpected EmptySetOperationConditionalSelector type: " + selector);
            }

            throw new IllegalArgumentException("Unexpected ConditionalSelector type: " + selector);
        }
    }

    private void addIncomingDataLabels(BranchNode root, List<Label> labels, boolean inverted) {
        labels.forEach(it -> root.addPredicate(new LiteralNode(inverted, new IncomingDataLabel(it))));
    }

    private void addOutgoingDataLabels(BranchNode root, List<Label> labels, boolean inverted) {
        labels.forEach(it -> root.addPredicate(new LiteralNode(inverted, new OutgoingDataLabel(it))));
    }

    private void addNodeLabels(BranchNode root, List<Label> labels, boolean inverted) {
        labels.forEach(it -> root.addPredicate(new LiteralNode(inverted, new NodeLabel(it))));
    }

    private void addIntersectionLabels(BranchNode root, List<Label> dataLabels, List<Label> nodeLabels) {
        // (not data1 OR not node1) AND (not data2 OR not node2) AND ...
        // (not node1 OR (not data_in1 and not data_out1) AND ...
        for (int i = 0; i < dataLabels.size(); i++) {
            var node = new DisjunctionNode();
            root.addPredicate(node);
            node.addPredicate(new LiteralNode(true, new NodeLabel(nodeLabels.get(i))));
            if (hasOutgoingData) {
                var dataNode = new ConjunctionNode();
                root.addPredicate(dataNode);
                dataNode.addPredicate(new LiteralNode(true, new IncomingDataLabel(dataLabels.get(i))));
                dataNode.addPredicate(new LiteralNode(true, new OutgoingDataLabel(dataLabels.get(i))));
            } else {
                node.addPredicate(new LiteralNode(true, new IncomingDataLabel(dataLabels.get(i))));
            }
        }
    }

    private List<Label> getLabels(CharacteristicsSelectorData characteristicsSelectorData) {
        var characteristicType = characteristicsSelectorData.characteristicType();
        var characteristicValue = characteristicsSelectorData.characteristicValue();

        if(!characteristicType.isConstant()) {
            throw new IllegalStateException("Selector Type not constant:" + characteristicsSelectorData);
        }

        var type = characteristicType.values().orElseThrow().get(0);
        var values = new ArrayList<String>();

        if(characteristicValue.isConstant()) {
            values.add(characteristicValue.values().orElseThrow().get(0));
        } else {
            values.addAll(conditionalVariables.get(characteristicValue.name()));
        }

        return values.stream().map(it -> new Label(type, it)).toList();
    }

    private void initialiseVariables(DataFlowDiagramAndDictionary dfd){
        dfd.dataDictionary().getLabelTypes().forEach(it -> variables.put(
                it.getEntityName(),
                it.getLabel().stream().map(NamedElement::getEntityName).toList()));
    }

    public String formulaToString() {
        return "\n" + baseFormula.toString() + "\nAND\n" + conditionalFormula.toString();
    }

    public String cnfToString() {
        var s = new StringBuilder();
        for (var constraint : cnf) {
            s.append(constraint.toString()).append("\n");
        }
        return s.toString();
    }

    public String simpleCNFToString() {
        var s = new StringBuilder();
        var literals = new ArrayList<Literal>();
        s.append("\n");
        for (var constraint : cnf) {
            for (var literal : constraint.literals()) {
                if (literals.contains(literal)) {
                    s.append(literal.positive() ? "" : "!").append(literals.indexOf(literal)).append(" ");
                } else {
                    s.append(literal.positive() ? "" : "!").append(literals.size()).append(" ");
                    literals.add(literal);
                }
            }
            s.append("\n");
        }
        s.append(getCNFStatistics(literals));
        return s.toString();
    }

    public String getCNFStatistics(List<Literal> literals) {
        StringBuilder s = new StringBuilder();

        s.append("Clauses: ").append(cnf.size()).append("\n");
        s.append("Literals: ").append(literals.size()).append("\n");

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