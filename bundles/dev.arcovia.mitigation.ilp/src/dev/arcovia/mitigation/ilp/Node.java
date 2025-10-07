package dev.arcovia.mitigation.ilp;

import java.util.List;

import org.dataflowanalysis.analysis.core.AbstractTransposeFlowGraph;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;

public class Node {
    private final String name;
    private List<Constraint> violatingConstraints;
    private AbstractTransposeFlowGraph tfg;
    private final boolean isForwarding;
    
    public Node(DFDVertex vertex, AbstractTransposeFlowGraph tfg, Constraint constraint){
        this.tfg = tfg;
        violatingConstraints.add(constraint);
        
        name = vertex.getName();
        
        isForwarding = true;
    }
}
