package dev.arcovia.mitigation.sat;

import org.dataflowanalysis.dfd.datadictionary.*;
import org.dataflowanalysis.dfd.datadictionary.impl.AssignmentImpl;
import org.dataflowanalysis.dfd.datadictionary.impl.ForwardingAssignmentImpl;
import org.dataflowanalysis.dfd.datadictionary.impl.LabelTypeImpl;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Vertex {
    public final String name;
    private Behavior behavior;
    private List<org.dataflowanalysis.dfd.datadictionary.Label> properties;
    private List<AbstractAssignment> assignments;
    private List<Assignment> outgoingAssignments = new ArrayList<>();
    private List<ForwardingAssignment> forwardingAssignments = new ArrayList<>();
    private HashMap<Pin,List<Label>> outgoingLabels;
    private List<Label> vertexLabels;
    public List<Pin> inPins;

    public Vertex(Node node) {
        this.behavior = node.getBehavior();
        this.name = node.getEntityName();
        this.properties = node.getProperties();
        this.inPins = getInpins();
        this.assignments = behavior.getAssignment();

        differentiateAssignments();

        this.vertexLabels = getVertexLabels();
        this.outgoingLabels = getOutgoingLabels();
    }
    private List<Pin> getInpins(){
        return behavior.getInPin();
    }
    private List<Label> transformLabels (List<org.dataflowanalysis.dfd.datadictionary.Label> labels){
        var transformedLabels = new ArrayList<Label>();
        for (var label: labels){
            LabelTypeImpl container = (LabelTypeImpl) label.eContainer();
            String type = container.getEntityName();
            transformedLabels.add(new Label(type, label.getEntityName()));
        }
        return transformedLabels;
    }
    private HashMap<Pin, List<Label>> getOutgoingLabels(){
        HashMap<Pin,List<Label>> outgoingCharacteristics = new HashMap<>();
        for (var assignment : outgoingAssignments) {
            var labels = assignment.getOutputLabels();
            var pin = assignment.getOutputPin();
            if (outgoingCharacteristics.containsKey(pin)) {
                outgoingCharacteristics.get(pin).addAll(transformLabels(labels));
            }
            else outgoingCharacteristics.put(pin, transformLabels(labels));
        }
        return outgoingCharacteristics;
    }
    private List<Label> getVertexLabels(){
        List<Label> nodeLabels = new ArrayList<>();
        for (var property : properties) {
            LabelTypeImpl container = (LabelTypeImpl) property.eContainer();
            String type = container.getEntityName();
            nodeLabels.add(new Label(type, property.getEntityName()));
        }
        return nodeLabels;
    }
    private void differentiateAssignments() {
        for (AbstractAssignment assignment : assignments) {
            if (assignment instanceof ForwardingAssignmentImpl) {
                forwardingAssignments.add((ForwardingAssignment) assignment);
            }
            else if (assignment instanceof AssignmentImpl) {
                outgoingAssignments.add((Assignment) assignment);
            }
        }
    }
    public List<Pin> getOutPinsWithLabel(Label label) {
        var pins = new ArrayList<Pin>();
        for (var pin : outgoingLabels.keySet()) {
            if (outgoingLabels.get(pin).contains(label)) {
                pins.add(pin);
            }
        }
        return pins;
    }
    public boolean hasVertexLabel(Label label) {
        return vertexLabels.contains(label);
    }

    public List<Pin> getForwardingOutPins(Pin pin) {
        var forwardingPins = new ArrayList<Pin>();
        for (var forwardingAssignment : forwardingAssignments) {
            if (forwardingAssignment.getInputPins().contains(pin))
                forwardingPins.add(forwardingAssignment.getOutputPin());
        }
        return forwardingPins;
    }

}
