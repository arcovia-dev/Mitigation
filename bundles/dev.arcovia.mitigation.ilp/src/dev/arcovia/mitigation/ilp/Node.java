package dev.arcovia.mitigation.ilp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dataflowanalysis.analysis.core.AbstractTransposeFlowGraph;
import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;

import dev.arcovia.mitigation.sat.OutgoingDataLabel;
import dev.arcovia.mitigation.sat.Term;

public class Node {
    private final double Epsilon = 0.01; 
    private final String name;
    private List<Constraint> violatingConstraints = new ArrayList<>();
    protected AbstractTransposeFlowGraph tfg;
    private final boolean isForwarding;
    private List<AbstractVertex<?>> previous;
    private Map<Pin, Flow> pinFlowMap;
    private Map<Pin, DFDVertex> pinDFDVertexMap;

    public Node(DFDVertex vertex, AbstractTransposeFlowGraph tfg, Constraint constraint) {
        this.tfg = tfg;

        if (constraint != null)
            violatingConstraints.add(constraint);

        name = vertex.getReferencedElement()
                .getId();

        isForwarding = determineForwarding(vertex);

        previous = vertex.getPreviousElements();

        pinFlowMap = vertex.getPinFlowMap();

        pinDFDVertexMap = vertex.getPinDFDVertexMap();

    }

    public Node(DFDVertex vertex, AbstractTransposeFlowGraph tfg) {
        this(vertex, tfg, null);
    }

    public boolean determineForwarding(DFDVertex vertex) {
        var assignments = vertex.getReferencedElement()
                .getBehavior()
                .getAssignment();
        for (var assignment : assignments) {
            if (assignment.getClass()
                    .toString()
                    .contains("Forwarding"))
                return true;
        }
        return false;
    }

    public List<AbstractVertex<?>> getPrevious() {
        return previous;
    }

    public List<Mitigation> getPossibleMitigations() {
        List<Mitigation> mitigations = new ArrayList<>();
        for (var constraint : violatingConstraints) {
            for (var mitigation : constraint.getMitigations()) {
                switch (mitigation.type) {
                    case Node -> {
                        mitigations.add(new Mitigation(new Term(this.name, mitigation.label), mitigation.cost, getAllRequiredMitigations(mitigation)));
                    }
                    case Data -> {
                        mitigations.addAll(getDataMitigations(mitigation));
                    }
                }
            }
        }

        return mitigations;
    }

    private List<Mitigation> getDataMitigations(MitigationStrategy mitigation) {
        List<Mitigation> mitigations = new ArrayList<>();

        for (var vertex : previous) {
            Node node = new Node((DFDVertex) vertex, tfg);
            
            mitigations.add(
                    new Mitigation(new Term(getOutpin((DFDVertex) vertex), new OutgoingDataLabel(mitigation.label.label())), mitigation.cost, getAllRequiredMitigations(mitigation)));

            if (node.isForwarding) {
                // need to discuss whether forwarding should be prioritized or not & if the user should decide --> Impact set
                mitigations.addAll(node.getDataMitigations(new MitigationStrategy(mitigation.label, mitigation.cost-Epsilon, MitigationType.Data)));
            }
        }

        return mitigations;
    }
    
    private List<Mitigation> getAllRequiredMitigations(MitigationStrategy mitigation){
    	List<Mitigation> requiredMitgations = new ArrayList<>();
        for (var requiredMitgation : mitigation.required)
        	requiredMitgations.add(new Mitigation(new Term(this.name, requiredMitgation.label), requiredMitgation.cost, getAllRequiredMitigations(requiredMitgation)));
        
        return requiredMitgations;
    }

    private String getOutpin(DFDVertex vertex) {
        Pin pin = pinDFDVertexMap.entrySet()
                .stream()
                .filter(e -> e.getValue()
                        .equals(vertex))
                .map(Map.Entry::getKey)
                .findFirst()
                .get();

        var flow = pinFlowMap.get(pin);

        return flow.getSourcePin()
                .getId();
    }
}
