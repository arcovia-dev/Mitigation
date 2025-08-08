package dev.arcovia.mitigation.sat;

import org.dataflowanalysis.dfd.datadictionary.*;
import org.dataflowanalysis.dfd.datadictionary.impl.AssignmentImpl;
import org.dataflowanalysis.dfd.datadictionary.impl.ForwardingAssignmentImpl;
import org.dataflowanalysis.dfd.datadictionary.impl.LabelTypeImpl;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The Vertex class represents an entity in a data flow analysis graph.
 * It encapsulates properties, behaviors, and relationships with other entities,
 * such as incoming and outgoing pins and assignments.
 */
public class Vertex {
    public final String name;
    private final Behavior behavior;
    private final List<org.dataflowanalysis.dfd.datadictionary.Label> properties;
    private final List<AbstractAssignment> assignments;
    private final List<Assignment> outgoingAssignments = new ArrayList<>();
    private final List<ForwardingAssignment> forwardingAssignments = new ArrayList<>();
    private final HashMap<Pin,List<Label>> outgoingLabels;
    private final List<Label> vertexLabels;
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

    /**
     * Retrieves a list of outgoing pins associated with a specific label.
     *
     * This method iterates through the set of outgoing labels and selects pins
     * that contain the given label in their corresponding list of labels.
     *
     * @param label the label used to filter the outgoing pins; must not be null
     * @return a list of pins associated with the specified label;
     *         returns an empty list if no pins match the given label
     */
    public List<Pin> getOutPinsWithLabel(Label label) {
        var pins = new ArrayList<Pin>();
        for (var pin : outgoingLabels.keySet()) {
            if (outgoingLabels.get(pin).contains(label)) {
                pins.add(pin);
            }
        }
        return pins;
    }

    /**
     * Checks if the specified label exists in the collection of vertex labels.
     *
     * @param label the label to check for existence; must not be null
     * @return true if the label exists in the vertex labels; false otherwise
     */
    public boolean hasVertexLabel(Label label) {
        return vertexLabels.contains(label);
    }

    /**
     * Retrieves a list of output pins that are forward-connected to the specified input pin.
     *
     * This method iterates through the forwarding assignments and identifies output pins
     * connected to the provided input pin through forwarding logic.
     *
     * @param pin the input pin for which forwarding output pins are to be retrieved; must not be null
     * @return a list of output pins that are forward-connected to the specified input pin;
     *         returns an empty list if no forward connections exist for the input pin
     */
    public List<Pin> getForwardingOutPins(Pin pin) {
        var forwardingPins = new ArrayList<Pin>();
        for (var forwardingAssignment : forwardingAssignments) {
            if (forwardingAssignment.getInputPins().contains(pin))
                forwardingPins.add(forwardingAssignment.getOutputPin());
        }
        return forwardingPins;
    }

}
