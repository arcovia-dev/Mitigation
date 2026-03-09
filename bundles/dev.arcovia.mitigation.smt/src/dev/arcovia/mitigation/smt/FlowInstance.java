package dev.arcovia.mitigation.smt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;

/**
 * Represents an instance of a flow of a specific Transpose Flow Graph
 */
public class FlowInstance {

    private final Pin sourcePin;
    private final DFDVertex sourceVertex;
    private final Pin destinationPin;
    private final DFDVertex destinationVertex;
    // DFD Flow that this flow instance flows along
    private final Flow flow;
    // List of incoming flow instances to the same vertex that need to be forwarded,
    // grouped by Assignment
    private final Map<ForwardingAssignment, List<FlowInstance>> thisFlowForwards;
    // List of incoming flow instances to the same vertex that each Assignment needs to
    // evaluate on
    private final Map<Assignment, List<FlowInstance>> thisFlowEvaluatesOn;

    private static int counter = 0;
    private final int id;

    /**
     * Constructs a flow instance given the input entities
     * @param sourcePin Source Pin of this Flow Instance
     * @param sourceVertex Source Vertex of this Flow Instance
     * @param destinationPin Destination Pin of this Flow Instance
     * @param destinationVertex Destination Vertex of this Flow Instance
     * @param flow DFD Flow that this Flow Instance represents for a specific TFG
     */
    public FlowInstance(Pin sourcePin, DFDVertex sourceVertex, Pin destinationPin, DFDVertex destinationVertex, Flow flow) {
        this.sourcePin = sourcePin;
        this.sourceVertex = sourceVertex;
        this.destinationPin = destinationPin;
        this.destinationVertex = destinationVertex;
        this.flow = flow;
        this.thisFlowForwards = new HashMap<>();
        this.thisFlowEvaluatesOn = new HashMap<>();
        this.id = counter++;
    }

    /**
     * Returns hash value based on this objects ID
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Compares this flow to another object based on their ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FlowInstance other = (FlowInstance) obj;
        return id == other.id;
    }

    /**
     * Returns a reduced String representation of this Flow instance
     */
    @Override
    public String toString() {
        return "TFGFlow [flow=" + flow.getEntityName() + " sourceNode " + sourceVertex.getName() + " destinationNode " + destinationVertex.getName()
                + ",id=" + id + "]";
    }

    /**
     * Returns the Source pin of this Flow instance
     * @return Source Pin
     */
    public Pin getSourcePin() {
        return sourcePin;
    }

    /**
     * Returns the source vertex of this Flow instance
     * @return Source Vertex
     */
    public DFDVertex getSourceVertex() {
        return sourceVertex;
    }

    /**
     * Returns the destination pin of this Flow instance
     * @return Destination Pin
     */
    public Pin getDestinationPin() {
        return destinationPin;
    }

    /**
     * Returns the destination vertex of this Flow instance
     * @return Destination Vertex
     */
    public DFDVertex getDestinationVertex() {
        return destinationVertex;
    }

    /**
     * Returns the original DFD Flow that this Flow instance represents a TFG-specific instance of
     * @return DFD Flow
     */
    public Flow getFlow() {
        return flow;
    }

    /**
     * Returns a Map of Forwarding Assignments to the respective Flow instances that need to be forwarded
     * @return Forwards Map
     */
    public Map<ForwardingAssignment, List<FlowInstance>> getThisFlowForwards() {
        return thisFlowForwards;
    }

    /**
     * Returns a Map of Assignments to the respective Flow instances that need to be evaluated
     * @return Assignment Map
     */
    public Map<Assignment, List<FlowInstance>> getThisFlowEvaluatesOn() {
        return thisFlowEvaluatesOn;
    }

    /**
     * Returns the id of this Flow Instance
     * @return Flow instance id
     */
    public int getId() {
        return id;
    }
}
