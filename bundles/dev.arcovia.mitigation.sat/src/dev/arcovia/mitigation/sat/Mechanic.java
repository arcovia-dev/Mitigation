package dev.arcovia.mitigation.sat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dataflowanalysis.analysis.core.AbstractTransposeFlowGraph;
import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryFactory;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import java.util.HashSet;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;

public class Mechanic {
    Map<String, String> outPinToAss = new HashMap<>();

    private List<Node> nodes = new ArrayList<>();
    private List<Flow> flows = new ArrayList<>();

    public DataFlowDiagramAndDictionary repair(DataFlowDiagramAndDictionary dfd, List<Constraint> constraints)
            throws ContradictionException, TimeoutException, IOException {
        List<AbstractTransposeFlowGraph> violatingTFGs = determineViolatingTFGs(dfd, constraints);
        deriveOutPinsToAssignmentsMap(dfd);

        getNodesAndFlows(violatingTFGs);
        
        
        var solutions = new Sat().solve(nodes, flows, constraints);
        Collections.sort(solutions, (list1, list2) -> Integer.compare(list1.size(), list2.size()));
        var minimalSolution = solutions.get(0);

        List<Term> flatendNodes = getFlatNodes(nodes);

        List<Term> actions = getActions(minimalSolution, flatendNodes);
        applyActions(dfd, actions);

        return dfd;
    }

    private List<AbstractTransposeFlowGraph> determineViolatingTFGs(DataFlowDiagramAndDictionary dfd, List<Constraint> constraints) {
        var ressourceProvider = new DFDModelResourceProvider(dfd.dataDictionary(), dfd.dataFlowDiagram());
        var analysis = new DFDDataFlowAnalysisBuilder().standalone()
                .useCustomResourceProvider(ressourceProvider)
                .build();

        analysis.initializeAnalysis();
        var flowGraph = analysis.findFlowGraphs();
        flowGraph.evaluate();
        Set<AbstractTransposeFlowGraph> violatingTransposeFlowGraphs = new HashSet<>();

        for (var tfg : flowGraph.getTransposeFlowGraphs()) {
            if (checkConstraints(tfg, constraints))
                violatingTransposeFlowGraphs.add(tfg);
        }
        return new ArrayList<AbstractTransposeFlowGraph>(violatingTransposeFlowGraphs);
    }

    private boolean checkConstraints(AbstractTransposeFlowGraph tfg, List<Constraint> constraints) {
        for (var constraint : constraints) {
            if (checkConstraint(tfg, constraint.literals()))
                return true;
        }
        return false;
    }

    private boolean checkConstraint(AbstractTransposeFlowGraph tfg, List<Literal> constraint) {
        List<String> negativeLiterals = new ArrayList<>();
        List<String> positveLiterals = new ArrayList<>();
        for (var literal : constraint) {
            if (literal.positive())
                positveLiterals.add( literal.label()
                        .toString());
            else
                negativeLiterals.add(literal.label()
                        .toString());
        }

        for (var node : tfg.getVertices()) {
            Set<String> nodeLiterals = new HashSet<>();
            for (var nodeChar : node.getAllVertexCharacteristics()) {
                nodeLiterals.add(new NodeLabel(nodeChar.getTypeName(), nodeChar.getValueName()).toString());
            }
            for (var variables : node.getAllIncomingDataCharacteristics()) {
                for (var dataChar : variables.getAllCharacteristics()) {
                    nodeLiterals.add(new IncomingDataLabel(dataChar.getTypeName(), dataChar.getValueName()).toString());
                }
            }
            
            
            if (nodeLiterals.stream()
                    .anyMatch(positveLiterals::contains)) {
                continue;
            } else if (!nodeLiterals.containsAll(negativeLiterals)) {
                continue;
            } else {
                return true;
            }
        }
        return false;
    }

    private void getNodesAndFlows(List<AbstractTransposeFlowGraph> violatingTFGs) {
        for (var tfg : violatingTFGs) {
            for (var vertex : tfg.getVertices()) {

                DFDVertex node = (DFDVertex) vertex;

                Map<InPin, List<Label>> inPins = new HashMap<>();
                for (var inPin : node.getAllIncomingDataCharacteristics()) {
                    List<Label> pinChars = new ArrayList<>();
                    for (var property : inPin.getAllCharacteristics()) {
                        var type = property.getTypeName();
                        var value = property.getValueName();
                        pinChars.add(new Label(type, value));
                    }
                    inPins.put(new InPin(inPin.getVariableName()), pinChars);
                }

                List<Label> nodeLabels = new ArrayList<>();
                for (var property : node.getAllVertexCharacteristics()) {
                    var type = property.getTypeName();
                    var value = property.getValueName();
                    nodeLabels.add(new Label(type, value));
                }

                Map<OutPin, List<Label>> outPinLabelMap = new HashMap<>();
                for (var outPin : node.getAllOutgoingDataCharacteristics()) {
                    List<Label> pinLabel = new ArrayList<>();
                    for (var property : outPin.getAllCharacteristics()) {
                        var type = property.getTypeName();
                        var value = property.getValueName();
                        pinLabel.add(new Label(type, value));
                    }
                    outPinLabelMap.put(new OutPin(outPin.getVariableName()), pinLabel);
                }

                nodes.add(new Node(node.getName(), inPins, outPinLabelMap, nodeLabels));

                for (var pin : node.getPinFlowMap()
                        .keySet()) {
                    var flow = node.getPinFlowMap()
                            .get(pin);
                    flows.add(new Flow(new OutPin(flow.getSourcePin()
                            .getId()), new InPin(pin.getId())));
                }
            }
        }
    }

    private void deriveOutPinsToAssignmentsMap(DataFlowDiagramAndDictionary dfd) {
        for (var node : dfd.dataFlowDiagram()
                .getNodes()) {
            for (var assignment : node.getBehaviour()
                    .getAssignment()) {
                var outPin = assignment.getOutputPin();
                outPinToAss.put(outPin.getId(), assignment.getId());
            }
        }
    }

    private List<Term> getFlatNodes(List<Node> nodes) {
        List<Term> flatendNodes = new ArrayList<>();
        for (var node : nodes) {
            for (var outPin : node.outPins()
                    .keySet()) {
                for (var label : node.outPins()
                        .get(outPin)) {
                    flatendNodes.add(new Term(outPin.id(), new OutgoingDataLabel(label.type(), label.value())));
                }
            }
            for (var property : node.nodeChars()) {
                flatendNodes.add(new Term(node.name(), new NodeLabel(property.type(), property.value())));
            }
        }
        return flatendNodes;
    }

    private List<Term> getActions(List<Term> minimalSolution, List<Term> flatendNodes) {
        List<Term> actions = new ArrayList<>();
        for (var delta : minimalSolution) {
            if (delta.label()
                    .category()
                    .equals(LabelCategory.IncomingData))
                continue;
            if (flatendNodes.contains(delta))
                continue;
            actions.add(delta);
        }
        return actions;
    }

    private void applyActions(DataFlowDiagramAndDictionary dfd, List<Term> actions) {
        var dd = dfd.dataDictionary();

        for (var action : actions) {
            if (action.label()
                    .category()
                    .equals(LabelCategory.OutgoingData)) {
                for (var behavior : dd.getBehaviour()) {
                    List<Assignment> newAssignments = new ArrayList<>();
                    for (var assignment : behavior.getAssignment()) {
                        if (assignment.getId()
                                .equals(outPinToAss.get(action.domain()))) {
                            var type = action.label()
                                    .type();
                            var value = action.label()
                                    .value();
                            var label = dd.getLabelTypes()
                                    .stream()
                                    .filter(labelType -> labelType.getEntityName()
                                            .equals(type))
                                    .flatMap(labelType -> labelType.getLabel()
                                            .stream())
                                    .filter(labelValue -> labelValue.getEntityName()
                                            .equals(value))
                                    .findAny()
                                    .get();
                            if (assignment instanceof Assignment cast) {
                                cast.getOutputLabels()
                                        .add(label);
                            }
                            if (assignment instanceof ForwardingAssignment cast) {
                                var ddFactory = datadictionaryFactory.eINSTANCE;
                                var assign = ddFactory.createAssignment();
                                assign.getOutputLabels()
                                        .add(label);
                                assign.setOutputPin(assignment.getOutputPin());
                                var ddTrue = ddFactory.createTRUE();
                                assign.setTerm(ddTrue);
                                newAssignments.add(assign);
                            }
                        }
                    }
                    if (!newAssignments.isEmpty())
                        behavior.getAssignment()
                                .addAll(newAssignments);
                }
            }
        }
    }
}
