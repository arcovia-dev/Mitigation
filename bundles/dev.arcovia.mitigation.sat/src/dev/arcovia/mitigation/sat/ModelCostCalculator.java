package dev.arcovia.mitigation.sat;


import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Pin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModelCostCalculator {
    DataFlowDiagramAndDictionary dfd;
    List<Constraint> constraints;
    Map<Label, Integer> costs;
    List<Vertex> nodes;
    HashMap<Label, Set<String>> allRelevantLabels = new HashMap<>();
    HashMap<Pin, Vertex> inPinToVertex;
    HashMap<Pin, Pin> sourcePinToDestinationPin;

    int cost = 0;

    public ModelCostCalculator(DataFlowDiagramAndDictionary dfd, List<Constraint> constraints, Map<Label, Integer> costs) {
        this.dfd = dfd;
        this.constraints = constraints;
        this.costs = costs;
        this.nodes = extractVertecies();
        this.inPinToVertex = inPinToVertex();
        this.sourcePinToDestinationPin = sourcePinToDestinationPin();
    }


    public int calculateCost() {
        determineRelevantLabels();
        for (var label : allRelevantLabels.keySet()){
            for (var vertex : nodes){
                if (vertex.hasVertexLabel(label))
                    allRelevantLabels.get(label).add("Vertex: " + vertex.name);
                for (var outPin : vertex.getOutPinsWithLabel(label)){
                        allRelevantLabels.get(label).add("Outgoing: " + outPin.getId() + " from: " + vertex.name );
                        pushLabel(label, outPin);
                }
            }
            cost += allRelevantLabels.get(label).size() * costs.get(label);
        }

        return cost;
    }
    private void pushLabel(Label label, Pin sourcePin){
        var destinationPin = sourcePinToDestinationPin.get(sourcePin);
        var destinationNode = inPinToVertex.get(destinationPin);
        for(var outPin : destinationNode.getForwardingOutPins(destinationPin)){
            if (!allRelevantLabels.get(label).contains("Outgoing: " + outPin.getId() + " from: " + destinationNode.name)){
                allRelevantLabels.get(label).add("Outgoing: " + outPin.getId() + " from: " + destinationNode.name);
                pushLabel(label, outPin);
            }
        }
    }

    private void determineRelevantLabels() {
        for (var label : getConstraintLabel()){
            if (costs.containsKey(label))
                allRelevantLabels.put(label, new HashSet<>());
        }
    }

    private List<Label> getConstraintLabel() {
        var label = new HashSet<Label>();
        for (var constraint : constraints) {
            for (var literal : constraint.literals()) {
                label.add(literal.compositeLabel()
                        .label());
            }
        }
        return List.copyOf(label);
    }

    private List<Vertex> extractVertecies(){
        List<Vertex> vertices = new ArrayList<>();
        for (var vertex : dfd.dataFlowDiagram().getNodes()){
            vertices.add(new Vertex(vertex));
        }
        return vertices;
    }

    private HashMap<Pin, Vertex> inPinToVertex() {
        HashMap<Pin, Vertex> pinVertexHashMap = new HashMap<>();
        for (var node : nodes){
            for (var inPin : node.inPins)
                pinVertexHashMap.put(inPin, node);
        }
        return pinVertexHashMap;
    }

    private HashMap<Pin, Pin> sourcePinToDestinationPin() {
        HashMap<Pin, Pin> sourcePinToDestinationPinMap = new HashMap<>();
        for (var flow : dfd.dataFlowDiagram().getFlows()){
            sourcePinToDestinationPinMap.put(flow.getSourcePin(),flow.getDestinationPin());
        }
        return sourcePinToDestinationPinMap;
    }
}
