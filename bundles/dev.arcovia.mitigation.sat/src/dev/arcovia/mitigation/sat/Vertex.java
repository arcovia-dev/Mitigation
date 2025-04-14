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
    private HashMap<Pin,List<Label>> outgoingLabels = new HashMap<>();
    private List<Label> vertexLabels = new ArrayList<>();
    public List<Pin> inPins;

    public Vertex(Node vertex) {
        this.behavior = vertex.getBehavior();
        this.assignments = behavior.getAssignment();
        this.name = vertex.getEntityName();
        this.properties = vertex.getProperties();
        getInpins();
        differentiateAssignments();
        getVertexLabels();
        getOutgoingLabels();
    }
    private void getInpins(){
        inPins = behavior.getInPin();
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
    private void getOutgoingLabels(){
        for (var assignment : outgoingAssignments) {
            var labels = assignment.getOutputLabels();
            var pin = assignment.getOutputPin();
            if (outgoingLabels.containsKey(pin)) {
                outgoingLabels.get(pin).addAll(transformLabels(labels));
            }
            else outgoingLabels.put(pin, transformLabels(labels));
        }
    }
    private void getVertexLabels(){
        for (var property : properties) {
            LabelTypeImpl container = (LabelTypeImpl) property.eContainer();
            String type = container.getEntityName();
            vertexLabels.add(new Label(type, property.getEntityName()));
        }
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
    public List<Pin> hasOutgoingLabel(Label label) {
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

    public List<Pin> isForwarding(Pin pin) {
        var forwardingPins = new ArrayList<Pin>();
        for (var forwardingAssignment : forwardingAssignments) {
            if (forwardingAssignment.getInputPins().contains(pin))
                forwardingPins.add(forwardingAssignment.getOutputPin());
        }
        return forwardingPins;
    }

}
