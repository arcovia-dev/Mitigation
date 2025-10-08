package dev.arcovia.mitigation.ilp;

import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.analysis.core.AbstractTransposeFlowGraph;
import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;

import dev.arcovia.mitigation.sat.Term;

public class Node {
    private final String name;
    private List<Constraint> violatingConstraints = new ArrayList<>();
    protected AbstractTransposeFlowGraph tfg;
    private final boolean isForwarding;
    private List<AbstractVertex<?>> previous;
    
    public Node(DFDVertex vertex, AbstractTransposeFlowGraph tfg, Constraint constraint){
        this.tfg = tfg;
        violatingConstraints.add(constraint);
        
        name = vertex.getName();
        
        isForwarding = determineForwarding(vertex);
        
        previous = vertex.getPreviousElements();
    }
    
    public Node(DFDVertex vertex, AbstractTransposeFlowGraph tfg){
        this.tfg = tfg;
        
        name = vertex.getName();
        
        isForwarding = determineForwarding(vertex);
        
        previous = vertex.getPreviousElements();
    }
    
    public boolean determineForwarding(DFDVertex vertex) {
        var assignments = vertex.getReferencedElement().getBehavior().getAssignment();
        for (var assignment : assignments) {
            if (assignment.getClass().toString().contains("Forwarding"))
                return true;
        }        
        return false;
    }
    
    public List<AbstractVertex<?>> getPrevious() {
        return previous;
    }
    
    public List<Mitigation> getpossibleMitigations(){
        List<Mitigation> mitigations = new ArrayList<>();
        for (var constraint : violatingConstraints) {
            for (var mitigation : constraint.getMitigations()) {
                switch (mitigation.type()) {
                    case Node -> {
                        mitigations.add(new Mitigation(new Term(this.name, mitigation.label()), mitigation.cost()));
                    }
                    case Data -> {
                        mitigations.addAll(getDataMitigations(mitigation));
                    }
                }
            }
        }
        
        return mitigations;
    }
    
    private List<Mitigation> getDataMitigations(MitigationStrategy mitigation){
        List<Mitigation> mitigations = new ArrayList<>();
        
        for (var vertex : previous) {
            Node node = new Node((DFDVertex) vertex, tfg);
            mitigations.add(new Mitigation(new Term(node.name, mitigation.label()), mitigation.cost()));
            
            if (node.isForwarding) {
                
                mitigations.addAll(node.getDataMitigations(new MitigationStrategy(mitigation.label(), mitigation.cost()-0.1, MitigationType.Data)));
            }
        }
        
        return mitigations;
    }
}
