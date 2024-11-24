package dev.abunai.confidentiality.mitigation.sat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

public class Mechanic {

    public DataFlowDiagramAndDictionary repair(DataFlowDiagramAndDictionary dfd, List<List<Constraint>> constraints)
            throws ContradictionException, TimeoutException, IOException {
        List<Node> nodes = getNodes(dfd);

        List<Edge> edges = getEdges(dfd);

        var solutions = new Sat().solve(nodes, edges, constraints);

        Collections.sort(solutions, (list1, list2) -> Integer.compare(list1.size(), list2.size()));
        var minimalSolution = solutions.get(0);

        List<Delta> flatNodes = getFlatNodes(nodes);

        List<Delta> actions = getActions(minimalSolution, flatNodes);

        applyActions(dfd, actions);

        return dfd;
    }

    private List<Node> getNodes(DataFlowDiagramAndDictionary dfd) {
        List<Node> nodes = new ArrayList<>();
        Map<String, String> outPinToAss = new HashMap<>();
        for (var node : dfd.dataFlowDiagram()
                .getNodes()) {

            List<InPin> inPins = new ArrayList<>();
            for (var inPin : node.getBehaviour()
                    .getInPin()) {
                inPins.add(new InPin(inPin.getId()));
            }

            Map<OutPin, List<Label>> outPins = new HashMap<>();
            for (var assignment : node.getBehaviour()
                    .getAssignment()) {
                var outPin = assignment.getOutputPin();
                outPinToAss.put(outPin.getId(), assignment.getId());
                List<Label> outLabels = new ArrayList<>();
                if (assignment instanceof Assignment cast) {
                    for (var label : cast.getOutputLabels()) {
                        var type = ((LabelType) label.eContainer()).getEntityName();
                        var value = label.getEntityName();
                        outLabels.add(new Label(type, value));
                    }
                }
                outPins.put(new OutPin(assignment.getId()), outLabels);
            }

            List<Label> nodeChars = new ArrayList<>();
            for (var property : node.getProperties()) {
                var type = ((LabelType) property.eContainer()).getEntityName();
                var value = property.getEntityName();
                nodeChars.add(new Label(type, value));
            }

            nodes.add(new Node(node.getEntityName(), inPins, outPins, nodeChars));
        }
        return nodes;
    }

    private List<Edge> getEdges(DataFlowDiagramAndDictionary dfd) {
        Map<String, String> outPinToAss = new HashMap<>();
        for (var node : dfd.dataFlowDiagram()
                .getNodes()) {
            for (var assignment : node.getBehaviour()
                    .getAssignment()) {
                var outPin = assignment.getOutputPin();
                outPinToAss.put(outPin.getId(), assignment.getId());
            }
        }
        List<Edge> edges = new ArrayList<>();

        for (var flow : dfd.dataFlowDiagram()
                .getFlows()) {
            edges.add(new Edge(new OutPin(outPinToAss.get(flow.getSourcePin()
                    .getId())), new InPin(flow.getDestinationPin()
                            .getId())));
        }
        return edges;
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
                    for (var assignment : behavior.getAssignment()) {
                        if (assignment.getId()
                                .equals(action.where())) {
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
                        }
                    }
                }
            }
        }
    }
}
