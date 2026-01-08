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
import dev.arcovia.mitigation.sat.CompositeLabel;

public class Node {
    private final double Epsilon = 0.01;
    private final String name;
    private List<Constraint> violatingConstraints = new ArrayList<>();
    protected AbstractTransposeFlowGraph tfg;
    private final boolean isForwarding;
    private List<AbstractVertex<?>> previous;
    private Map<Pin, Flow> pinFlowMap;
    private Map<Pin, DFDVertex> pinDFDVertexMap;
    private final DFDVertex vertex;

    public Node(DFDVertex vertex, AbstractTransposeFlowGraph tfg, Constraint constraint) {
        this.tfg = tfg;

        if (constraint != null)
            violatingConstraints.add(constraint);

        this.vertex = vertex;
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
                    case NodeLabel -> {
                        mitigations.add(new Mitigation(new ActionTerm(this.name, mitigation.label, ActionType.Adding), mitigation.cost,
                                getAllRequiredMitigations(mitigation)));
                    }
                    case DataLabel -> {
                        mitigations.addAll(getDataMitigations(mitigation, ActionType.Adding));
                    }
                    case DeleteNodeLabel -> {
                        mitigation.checkIfAllowed(vertex);
                        mitigations.add(new Mitigation(new ActionTerm(this.name, mitigation.label, ActionType.Removing), mitigation.cost,
                                getAllRequiredMitigations(mitigation)));
                    }
                    case DeleteDataLabel -> {
                        mitigations.addAll(getDataMitigations(mitigation, ActionType.Removing));
                    }
                    case AddNode -> {
                        mitigations.add(new Mitigation(new ActionTerm(this.name, mitigation.label, ActionType.AddNode), mitigation.cost,
                                getAllRequiredMitigations(mitigation)));
                        mitigations.addAll(getNodeAdditionMitigations(mitigation));
                    }
                    case AddSink -> {
                        mitigations.add(new Mitigation(new ActionTerm(this.name, mitigation.label, ActionType.AddSink), mitigation.cost,
                                getAllRequiredMitigations(mitigation)));
                        mitigations.addAll(getSinkAdditionMitigations(mitigation));
                    }
                    case DeleteNode -> {
                        mitigations.add(new Mitigation(new ActionTerm(this.name, mitigation.label, ActionType.RemoveNode), mitigation.cost,
                                getAllRequiredMitigations(mitigation)));
                    }                    
                    case DeleteFlow -> {
                        for (var incomingFlow : this.vertex.getPinFlowMap().values()) {
                            mitigations.add(new Mitigation(new ActionTerm(incomingFlow.getEntityName(), null, ActionType.RemoveFlow), mitigation.cost,
                                    getAllRequiredMitigations(mitigation)));
                        }
                    }
                    
                    default -> throw new IllegalArgumentException("Unexpected value: " + mitigation.type);
                    
                }
            }
        }

        return mitigations;
    }
    
    private List<Mitigation> getNodeAdditionMitigations(MitigationStrategy mitigation){
        List<Mitigation> mitigations = new ArrayList<>();

        for (var vertex : previous) {
            Node node = new Node((DFDVertex) vertex, tfg);
            mitigations.add(new Mitigation(new ActionTerm(node.name, mitigation.label, ActionType.AddNode), mitigation.cost,
                    getAllRequiredMitigations(mitigation)));
            mitigations.addAll(
                    node.getNodeAdditionMitigations(mitigation));
        }
        return mitigations;
    }
    
    private List<Mitigation> getSinkAdditionMitigations(MitigationStrategy mitigation){
        List<Mitigation> mitigations = new ArrayList<>();      
        
        for (var flow : vertex.getPinFlowMap().values()) {
            if (vertex.getAllOutgoingDataCharacteristics().isEmpty()) continue;
            Node node = new Node((DFDVertex) flow.getDestinationNode(), tfg);
            mitigations.add(new Mitigation(new ActionTerm(node.name, mitigation.label, ActionType.AddSink), mitigation.cost,
                    getAllRequiredMitigations(mitigation)));
            mitigations.addAll(
                    node.getSinkAdditionMitigations(mitigation));
        }
        return mitigations;
    }

    private List<Mitigation> getDataMitigations(MitigationStrategy mitigation, ActionType type) {
        List<Mitigation> mitigations = new ArrayList<>();

        for (var vertex : previous) {
            Node node = new Node((DFDVertex) vertex, tfg);
            
            List <CompositeLabel> outGoingLabels = new ArrayList<>();
            
            for (var label : mitigation.label) {
                outGoingLabels.add(new OutgoingDataLabel(label.label()));
            }

            mitigations.add(new Mitigation(new ActionTerm(getOutpin((DFDVertex) vertex), outGoingLabels, type),
                    mitigation.cost, getAllRequiredMitigations(mitigation)));

            if (node.isForwarding) {
                mitigations.addAll(
                        node.getDataMitigations(new MitigationStrategy(mitigation.label, mitigation.cost - Epsilon, MitigationType.DataLabel), type));
            }
        }

        return mitigations;
    }

    private List<List<Mitigation>> getAllRequiredMitigations(MitigationStrategy mitigation) {
        List<List<Mitigation>> requiredMitgations = new ArrayList<>();
        for (var mitgations : mitigation.required) {
            List<Mitigation> requiredMitgation = new ArrayList<>();
            for (var mitgation : mitgations) {
                ActionType type;
                if (mitgation.type.toString()
                        .startsWith("Delete"))
                    type = ActionType.Removing;
                
                else if (mitgation.type == MitigationType.AddNode) {
                    type = ActionType.AddNode;;   
                }
                else if (mitgation.type == MitigationType.AddSink) {
                    type = ActionType.AddSink;;   
                }
                
                else {
                    type = ActionType.Adding;
                }
                
                requiredMitgation.add(new Mitigation(new ActionTerm(this.name, mitgation.label, type), mitgation.cost,
                        getAllRequiredMitigations(mitgation)));    
            }
            requiredMitgations.add(requiredMitgation);

        }
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
