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
public class TFGFlow {

    private final Pin srcPin;
    private final DFDVertex srcVertex;
    private final Pin dstPin;
    private final DFDVertex dstVertex;
    // DFD Flow that this TFGFlow flows along
    private final Flow flow;
    // List of incoming TFG Flows to the same vertex that need to be forwarded,
    // grouped by Assignment
    private final Map<ForwardingAssignment, List<TFGFlow>> thisFlowForwards;
    // List of incoming TFG Flows to the same vertex that each Assignment needs to
    // evaluate on
    private final Map<Assignment, List<TFGFlow>> thisFlowEvaluatesOn;

    private static int counter = 0;
    private final int id;

    /**
     * Constructs a TFG Flow instance given the input entities
     * @param srcP Source Pin of this TFG Flow
     * @param srcVertex Source Vertex of this TFG Flow
     * @param dstP Destination Pin of this TFG Flow
     * @param dstVertex Destination Vertex of this TFG Flow
     * @param flow DFD Flow that this TFG Flow represents for a specific TFG
     */
    public TFGFlow(Pin srcP, DFDVertex srcVertex, Pin dstP, DFDVertex dstVertex, Flow flow) {
        this.srcPin = srcP;
        this.srcVertex = srcVertex;
        this.dstPin = dstP;
        this.dstVertex = dstVertex;
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
        TFGFlow other = (TFGFlow) obj;
        return id == other.id;
    }

    /**
     * Returns a reduced String representation of this Flow
     */
    @Override
    public String toString() {
        return "TFGFlow [flow=" + flow.getEntityName() + " sourceNode " + srcVertex.getName() + " dstNode " + dstVertex.getName() + ",id=" + id + "]";
    }

    /**
     * Returns the Source pin of this TFG Flow
     * @return Source Pin
     */
    public Pin getSrcPin() {
        return srcPin;
    }

    /**
     * Returns the source vertex of this TFG Flow
     * @return Source Vertex
     */
    public DFDVertex getSrcVertex() {
        return srcVertex;
    }

    /**
     * Returns the destination pin of this TFG Flow
     * @return Destination Pin
     */
    public Pin getDstPin() {
        return dstPin;
    }

    /**
     * Returns the destination vertex of this TFG Flow
     * @return Destination Vertex
     */
    public DFDVertex getDstVertex() {
        return dstVertex;
    }

    /**
     * Returns the original DFD Flow that this TFG Flow represents a TFG-specific instance of
     * @return DFD Flow
     */
    public Flow getFlow() {
        return flow;
    }

    /**
     * Returns a Map of Forwarding Assignments to the respective TFG Flows that need to be forwarded
     * @return Forwards Map
     */
    public Map<ForwardingAssignment, List<TFGFlow>> getThisFlowForwards() {
        return thisFlowForwards;
    }

    /**
     * Returns a Map of Assignments to the respective TFG Flows that need to be evaluated
     * @return Assignment Map
     */
    public Map<Assignment, List<TFGFlow>> getThisFlowEvaluatesOn() {
        return thisFlowEvaluatesOn;
    }

    /**
     * Returns the id of this TFG Flow
     * @return TFG Flow Id
     */
    public int getId() {
        return id;
    }
}
