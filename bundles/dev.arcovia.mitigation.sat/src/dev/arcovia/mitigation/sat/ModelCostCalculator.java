package dev.arcovia.mitigation.sat;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.SetAssignment;
import org.dataflowanalysis.dfd.datadictionary.impl.LabelTypeImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ModelCostCalculator is responsible for computing the total cost of a data flow diagram (DFD) based on predefined
 * constraints and label costs. The calculator traverses the DFD, identifies relevant labels, and evaluates their
 * occurrences in various nodes and connections within the diagram.
 */
public class ModelCostCalculator {
    DataFlowDiagramAndDictionary dfd;
    List<Constraint> constraints;
    Map<Label, Integer> costs;
    List<Vertex> nodes;
    HashMap<Label, Set<String>> allRelevantLabels = new HashMap<>();
    HashMap<Pin, Vertex> inPinToVertex;
    HashMap<Pin, Pin> sourcePinToDestinationPin;

    int cost = 0;

    /**
     * Constructs a ModelCostCalculator that computes the cost of evaluating a data flow model with provided constraints and
     * associated costs for labels.
     * @param dfd the DataFlowDiagramAndDictionary instance representing the data flow diagram and its dictionary used in
     * the evaluation.
     * @param constraints the list of constraints to be applied during the cost calculation process.
     * @param costs a map that associates a label with its corresponding integer cost.
     */
    public ModelCostCalculator(DataFlowDiagramAndDictionary dfd, List<Constraint> constraints, Map<Label, Integer> costs) {
        this.dfd = dfd;
        this.constraints = constraints;
        this.costs = costs;
        this.nodes = extractVertecies();
        this.inPinToVertex = inPinToVertex();
        this.sourcePinToDestinationPin = sourcePinToDestinationPin();
    }

    /**
     * Calculates the cost of evaluating a data flow model by computing the combined cost of all relevant labels and their
     * associated operations within the model. It identifies relevant labels based on constraints, evaluates vertices, and
     * processes outgoing pins linked to those labels. The cost for a label is determined by multiplying the number of
     * related operations by the predefined cost of the label.
     * @return the total calculated cost of the model evaluation as an integer
     */
    public int calculateCost() {
        determineRelevantLabels();
        for (var label : allRelevantLabels.keySet()) {
            for (var vertex : nodes) {
                if (vertex.hasVertexLabel(label))
                    allRelevantLabels.get(label)
                            .add("Vertex: " + vertex.name);
                for (var outPin : vertex.getOutPinsWithLabel(label)) {
                    allRelevantLabels.get(label)
                            .add("Outgoing: " + outPin.getId() + " from: " + vertex.name);
                    pushLabel(label, outPin);
                }
            }
            cost += allRelevantLabels.get(label)
                    .size() * costs.get(label);
        }

        return cost;
    }
    public int calculateCostWithoutForwarding() {
        determineRelevantLabels();
        for (var label : allRelevantLabels.keySet()) {
            for (var vertex : nodes) {
                if (vertex.hasVertexLabel(label))
                    allRelevantLabels.get(label)
                            .add("Vertex: " + vertex.name);
                
                var assignments = vertex.getAssignments();
                
                for (var assignment : assignments) {
                    
                    List<org.dataflowanalysis.dfd.datadictionary.Label> assignmentLabel = null;
                    
                    if (assignment instanceof Assignment cast) {
                        assignmentLabel = cast.getOutputLabels();
                                
                    }
                    else if (assignment instanceof SetAssignment cast) {
                        assignmentLabel = cast.getOutputLabels();
                    }
                    if (assignmentLabel == null) continue;
                    
                    for (var l : assignmentLabel) {
                        LabelTypeImpl labelType = (LabelTypeImpl) l.eContainer();
                        
                        var tempLabel = new Label(labelType.getEntityName(),l.getEntityName());
                        
                        if (tempLabel.toString().equals(label.toString())) {
                            Pin outpin = assignment.getOutputPin();
                            allRelevantLabels.get(label)
                            .add("Outgoing: " + outpin.getId() + " from: " + vertex.name);
                        }
                    }
                }
            }
            cost += allRelevantLabels.get(label)
                    .size() * costs.get(label);
        }

        return cost;
    }

    private void pushLabel(Label label, Pin sourcePin) {
        var destinationPin = sourcePinToDestinationPin.get(sourcePin);
        var destinationNode = inPinToVertex.get(destinationPin);
        for (var outPin : destinationNode.getForwardingOutPins(destinationPin)) {
            if (!allRelevantLabels.get(label)
                    .contains("Outgoing: " + outPin.getId() + " from: " + destinationNode.name)) {
                allRelevantLabels.get(label)
                        .add("Outgoing: " + outPin.getId() + " from: " + destinationNode.name);
                pushLabel(label, outPin);
            }
        }
    }

    private void determineRelevantLabels() {
        for (var label : getConstraintLabel()) {
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

    private List<Vertex> extractVertecies() {
        List<Vertex> vertices = new ArrayList<>();
        for (var vertex : dfd.dataFlowDiagram()
                .getNodes()) {
            vertices.add(new Vertex(vertex));
        }
        return vertices;
    }

    private HashMap<Pin, Vertex> inPinToVertex() {
        HashMap<Pin, Vertex> pinVertexHashMap = new HashMap<>();
        for (var node : nodes) {
            for (var inPin : node.inPins)
                pinVertexHashMap.put(inPin, node);
        }
        return pinVertexHashMap;
    }

    private HashMap<Pin, Pin> sourcePinToDestinationPin() {
        HashMap<Pin, Pin> sourcePinToDestinationPinMap = new HashMap<>();
        for (var flow : dfd.dataFlowDiagram()
                .getFlows()) {
            sourcePinToDestinationPinMap.put(flow.getSourcePin(), flow.getDestinationPin());
        }
        return sourcePinToDestinationPinMap;
    }
}
