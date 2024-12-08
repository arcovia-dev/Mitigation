package dev.abunai.confidentiality.mitigation.sat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dataflowanalysis.analysis.core.AbstractTransposeFlowGraph;
import org.dataflowanalysis.analysis.core.AbstractVertex;
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
    private List<Edge> edges = new ArrayList<>();

    public DataFlowDiagramAndDictionary repair(DataFlowDiagramAndDictionary dfd, List<List<Constraint>> constraints)
            throws ContradictionException, TimeoutException, IOException {
        List<AbstractTransposeFlowGraph> violatingTFGs = determineViolatingTFGs(dfd, constraints);

        mapOutPinsToAssignments(dfd);

        getNodesAndEdges(violatingTFGs);

        var solutions = new Sat().solve(nodes, edges, constraints);
        Collections.sort(solutions, (list1, list2) -> Integer.compare(list1.size(), list2.size()));
        var minimalSolution = solutions.get(0);

        List<Delta> flatNodes = getFlatNodes(nodes);

        List<Delta> actions = getActions(minimalSolution, flatNodes);
        applyActions(dfd, actions);

        return dfd;
    }

    private List<AbstractTransposeFlowGraph> determineViolatingTFGs(DataFlowDiagramAndDictionary dfd, List<List<Constraint>> constraints) {
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

    private boolean checkConstraints(AbstractTransposeFlowGraph tfg, List<List<Constraint>> constraints) {
        for (var constraint : constraints) {
            if (checkConstraint(tfg, constraint))
                return true;
        }
        return false;
    }

    private boolean checkConstraint(AbstractTransposeFlowGraph tfg, List<Constraint> constraint) {
        List<String> negativeLiterals = new ArrayList<>();
        List<String> positveLiterals = new ArrayList<>();
        for (var literal : constraint) {
            if (literal.positive())
                positveLiterals.add(literal.what()+literal.label().toString());
            else
                negativeLiterals.add(literal.what()+literal.label().toString());
        }

        for (var node : tfg.getVertices()) {
            Set<String> nodeLiterals = new HashSet<>();
            for (var nodeChar:node.getAllVertexCharacteristics()) {
                nodeLiterals.add("Node"+new Label(nodeChar.getTypeName(),nodeChar.getValueName()).toString());
            }
            for (var variables:node.getAllIncomingDataCharacteristics()) {
                for(var dataChar:variables.getAllCharacteristics()) {
                    nodeLiterals.add("Data"+new Label(dataChar.getTypeName(),dataChar.getValueName()).toString());
                }    
            }
            if(nodeLiterals.stream().anyMatch(positveLiterals::contains)) {
                continue;
            }
            else if(!nodeLiterals.containsAll(negativeLiterals)) {
                continue;
            }
            else {
                return true;
            }
        }
        return false;
    }

    private void getNodesAndEdges(List<AbstractTransposeFlowGraph> violatingTFGs) {
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

                List<Label> nodeChars = new ArrayList<>();
                for (var property : node.getAllVertexCharacteristics()) {
                    var type = property.getTypeName();
                    var value = property.getValueName();
                    nodeChars.add(new Label(type, value));
                }

                Map<OutPin, List<Label>> outPins = new HashMap<>();
                for (var outPin : node.getAllOutgoingDataCharacteristics()) {
                    List<Label> pinChars = new ArrayList<>();
                    for (var property : outPin.getAllCharacteristics()) {
                        var type = property.getTypeName();
                        var value = property.getValueName();
                        pinChars.add(new Label(type, value));
                    }
                    outPins.put(new OutPin(outPin.getVariableName()), pinChars);
                }

                nodes.add(new Node(node.getName(), inPins, outPins, nodeChars));

                for (var pin : node.getPinFlowMap()
                        .keySet()) {
                    var flow = node.getPinFlowMap()
                            .get(pin);
                    edges.add(new Edge(new OutPin(flow.getSourcePin()
                            .getId()), new InPin(pin.getId())));
                }
            }
        }
    }

    private void mapOutPinsToAssignments(DataFlowDiagramAndDictionary dfd) {
        for (var node : dfd.dataFlowDiagram()
                .getNodes()) {
            for (var assignment : node.getBehaviour()
                    .getAssignment()) {
                var outPin = assignment.getOutputPin();
                outPinToAss.put(outPin.getId(), assignment.getId());
            }
        }
    }

    private List<Delta> getFlatNodes(List<Node> nodes) {
        List<Delta> flatNodes = new ArrayList<>();
        for (var node : nodes) {
            for (var outPin : node.outPins()
                    .keySet()) {
                for (var label : node.outPins()
                        .get(outPin)) {
                    flatNodes.add(new Delta(outPin.id(), new OutDataChar(label.type(), label.value())));
                }
            }
            for (var property : node.nodeChars()) {
                flatNodes.add(new Delta(node.name(), new NodeChar(property.type(), property.value())));
            }
        }
        return flatNodes;
    }

    private List<Delta> getActions(List<Delta> minimalSolution, List<Delta> flatNodes) {
        List<Delta> actions = new ArrayList<>();
        for (var delta : minimalSolution) {
            if (delta.characteristic()
                    .what()
                    .equals("InData"))
                continue;
            if (flatNodes.contains(delta))
                continue;
            actions.add(delta);
        }
        return actions;
    }

    private void applyActions(DataFlowDiagramAndDictionary dfd, List<Delta> actions) {
        var dd = dfd.dataDictionary();

        for (var action : actions) {
            if (action.characteristic()
                    .what()
                    .equals("OutData")) {
                for (var behavior : dd.getBehaviour()) {
                    List<Assignment> newAssignments = new ArrayList<>();
                    for (var assignment : behavior.getAssignment()) {
                        if (assignment.getId()
                                .equals(outPinToAss.get(action.where()))) {
                            var type = action.characteristic()
                                    .type();
                            var value = action.characteristic()
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
