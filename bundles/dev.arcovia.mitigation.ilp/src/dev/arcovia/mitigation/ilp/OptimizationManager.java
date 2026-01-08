package dev.arcovia.mitigation.ilp;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.web2dfd.Web2DFDConverter;
import org.dataflowanalysis.converter.web2dfd.WebEditorConverterModel;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.dfd.datadictionary.SetAssignment;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryFactory;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import org.dataflowanalysis.dfd.dataflowdiagram.dataflowdiagramFactory;

import dev.arcovia.mitigation.sat.CompositeLabel;
import dev.arcovia.mitigation.sat.LabelCategory;
import dev.arcovia.mitigation.sat.NodeLabel;

public class OptimizationManager {
    private final DataFlowDiagramAndDictionary dfd;

    Map<String, String> outPinToAssignmentMap = new HashMap<>();

    private final Logger logger = Logger.getLogger(OptimizationManager.class);

    private final List<Constraint> constraints;

    private Set<Node> violatingNodes = new HashSet<>();

    private List<List<Mitigation>> mitigations = new ArrayList<>();
    private Set<Mitigation> allMitigations = new HashSet<>();
    
    private List<List<Mitigation>> contradictions = new ArrayList<>();

    private List<ActionTerm> actions;

    public OptimizationManager(String dfdLocation, List<AnalysisConstraint> constraints) {
        this.dfd = new Web2DFDConverter().convert(new WebEditorConverterModel(dfdLocation));
        this.constraints = getConstraints(constraints);
    }

    public OptimizationManager(DataFlowDiagramAndDictionary dfd, List<AnalysisConstraint> constraints) {
        this.dfd = dfd;
        this.constraints = getConstraints(constraints);
    }
    
    public OptimizationManager(String dfdLocation, List<Constraint> constraints, boolean addAdditionalMitigations) {
        this.dfd = new Web2DFDConverter().convert(new WebEditorConverterModel(dfdLocation));;
        this.constraints = constraints;
        
        if (addAdditionalMitigations) {
        	for (var constraint: this.constraints) constraint.findAlternativeMitigations();
        }
    }
    
    public OptimizationManager(DataFlowDiagramAndDictionary dfd, List<Constraint> constraints, boolean addAdditionalMitigations) {
        this.dfd = dfd;
        this.constraints = constraints;
        
        if (addAdditionalMitigations) {
            for (var constraint: this.constraints) constraint.findAlternativeMitigations();
        }
    }

    public DataFlowDiagramAndDictionary repair() {
        analyseConstraints();

        analyseDFD();

        for (var node : violatingNodes) {
            addMitigations(node.getPossibleMitigations());
        }
        
        
        
        for (var mitigation : allMitigations) {
            if (mitigation.mitigation().type().toString().startsWith("Remove")) {
                contradictions.addAll(determineContradictions(mitigation));
            }
        }

        var solver = new ILPSolver();
        var result = solver.solve(mitigations, allMitigations, contradictions);

        actions = getActions(result);

        applyActions(dfd, actions);

        return dfd;
    }

    public int getCost() {
        return actions.size();
    }

    public boolean isViolationFree(DataFlowDiagramAndDictionary dfd) {
        var resourceProvider = new DFDModelResourceProvider(dfd.dataDictionary(), dfd.dataFlowDiagram());
        var analysis = new DFDDataFlowAnalysisBuilder().standalone()
                .useCustomResourceProvider(resourceProvider)
                .build();

        analysis.initializeAnalysis();
        var flowGraph = analysis.findFlowGraphs();
        flowGraph.evaluate();
        
        for (var constraint : this.constraints) {
            var result = constraint.determineViolations(flowGraph);
            if (!result.isEmpty())
                return false;
        }
        return true;
    }
    
    private List<List<Mitigation>> determineContradictions(Mitigation domainMitigation){
        List<List<Mitigation>> contradiction = new ArrayList<>();
        for (var mitigation : allMitigations) {
            if (mitigation.mitigation().domain().equals(domainMitigation.mitigation().domain()) && mitigation.mitigation().type().toString().startsWith("Add")) {
                contradiction.add(List.of(domainMitigation,mitigation));
            }
        }
        return contradiction;
    }

    private List<ActionTerm> getActions(List<Mitigation> result) {
        List<Mitigation> additional = new ArrayList<>();
        result.addAll(additional);
        List<ActionTerm> actions = new ArrayList<>();
        for (var mit : result) {
            actions.add(mit.mitigation());
        }
        return actions;
    }

    private void analyseConstraints() {
        for (var constraint : constraints) {
            for (var mitigation : constraint.getMitigations()) {
                if (mitigation.type.toString()
                        .startsWith("Delete")) {
                    setAdditonalConstraints(mitigation);
                }

                else {
                    var additionalMitigations = getAdditionalMitigations(mitigation.label);
                    if (additionalMitigations != null) {
                        mitigation.addRequired(additionalMitigations);
                    }
                }

            }
        }
    }

    private void setAdditonalConstraints(MitigationStrategy mitigation) {
        for (var constraint : constraints) {
            for (var mit : constraint.getMitigations()) {
                if (!mitigation.type.toString()
                        .startsWith("Delete") && mit.label.equals(mitigation.label)) {
                    mitigation.addConstraint(constraint);
                }
            }
        }
    }

    private List<List<MitigationStrategy>> getAdditionalMitigations(List<CompositeLabel> labels) {
        List<List<MitigationStrategy>> required = new ArrayList<>();
        for (var label : labels) {
            for (var constraint : constraints) {
                if (constraint.isPrecondition(label))
                    if (required.isEmpty()) {
                        for (var mitigation : constraint.getMitigations()) {
                            if(mitigation.label.contains(label)) continue;
                            required.add(List.of(mitigation));
                        }
                    }
                    else {
                        List<List<MitigationStrategy>> newRequired = new ArrayList<>();
                        for (var requieredMitgation : required) {
                            for (MitigationStrategy mitigation : constraint.getMitigations()) {
                                if(mitigation.label.contains(label)) continue;
                                List<MitigationStrategy> temp = new ArrayList<>(requieredMitgation);
                                temp.add(mitigation);
                                newRequired.add(temp);
                            } 
                        }
                        required = newRequired;
                    }

            }
        }
        return required;
    }

    private void addMitigations(List<Mitigation> mitigation) {
        // done to prevent having the same Mitigation twice by replacing duplicates with
        // the original/first appearance
        
        
        List<Mitigation> merged = mitigation.stream()
                .map(u -> allMitigations.stream()
                        .filter(u::equals)
                        .findFirst()
                        .orElse(u))
                .collect(Collectors.toList());

        mitigations.add(merged);
        
        List<Mitigation> mitigationAdding = new ArrayList<>();
        for (var mit : mitigation) {
            mitigationAdding.add(mit);
            for (var required : mit.required()) {
                mitigationAdding.addAll(required);
            }
        }
        
        merged = mitigationAdding.stream()
                .map(u -> allMitigations.stream()
                        .filter(u::equals)
                        .findFirst()
                        .orElse(u))
                .collect(Collectors.toList());
        
        allMitigations.addAll(merged);
    }

    private List<Constraint> getConstraints(List<AnalysisConstraint> constraints) {
        List<Constraint> constraintList = new ArrayList<>();

        for (var constraint : constraints)
            constraintList.add(new Constraint(constraint));

        return constraintList;
    }

    private void analyseDFD() {
        var resourceProvider = new DFDModelResourceProvider(dfd.dataDictionary(), dfd.dataFlowDiagram());
        var analysis = new DFDDataFlowAnalysisBuilder().standalone()
                .useCustomResourceProvider(resourceProvider)
                .build();

        analysis.initializeAnalysis();
        var flowGraph = analysis.findFlowGraphs();
        flowGraph.evaluate();

        for (var constraint : constraints) {        	
            violatingNodes.addAll(constraint.determineViolations(flowGraph));
        }
    }

    private void applyActions(DataFlowDiagramAndDictionary dfd, List<ActionTerm> actions) {
        deriveOutPinsToAssignmentsMap(dfd);
        
        addAndRemoveLabels(dfd, actions);
      //Adding Nodes
        addNodes(dfd, actions);
        
        addSinks(dfd, actions);
        
        
        //Removing Nodes & flows

        removeNodes(dfd, actions);
        
        removeFlows(dfd, actions);
        
        
    }
    
    private void addAndRemoveLabels(DataFlowDiagramAndDictionary dfd, List<ActionTerm> actions) {
        var dd = dfd.dataDictionary();

        for (var action : actions) {
            if (action.type()
                    .equals(ActionType.Adding)) {
                if (action.compositeLabels().get(0)
                        .category()
                        .equals(LabelCategory.OutgoingData)) {
                    for (var behavior : dd.getBehavior()) {
                        List<Assignment> newAssignments = new ArrayList<>();
                        for (var assignment : behavior.getAssignment()) {
                            if (assignment.getId()
                                    .equals(outPinToAssignmentMap.get(action.domain()))) {
                                var type = action.compositeLabels().get(0)
                                        .label()
                                        .type();
                                var value = action.compositeLabels().get(0)
                                        .label()
                                        .value();
                                var label = getOrCreateLabel(dd, type, value);

                                if (assignment instanceof Assignment cast) {
                                    cast.getOutputLabels()
                                            .add(label);
                                }
                                if (assignment instanceof SetAssignment cast) {
                                    cast.getOutputLabels()
                                            .add(label);
                                }
                                if (assignment instanceof ForwardingAssignment) {
                                    var ddFactory = datadictionaryFactory.eINSTANCE;
                                    var assign = ddFactory.createAssignment();
                                    assign.getOutputLabels()
                                            .add(label);
                                    assign.setOutputPin(assignment.getOutputPin());
                                    var ddTrue = ddFactory.createTRUE();
                                    assign.setTerm(ddTrue);
                                    newAssignments.add(assign);
                                }
                            }
                        }
                        if (!newAssignments.isEmpty())
                            behavior.getAssignment()
                                    .addAll(newAssignments);
                    }
                } else if (action.compositeLabels().get(0)
                        .category()
                        .equals(LabelCategory.Node)) {
                    for (var node : dfd.dataFlowDiagram()
                            .getNodes()) {
                        if (node.getId()
                                .equals(action.domain())) {
                            var type = action.compositeLabels().get(0)
                                    .label()
                                    .type();
                            var value = action.compositeLabels().get(0)
                                    .label()
                                    .value();
                            var label = getOrCreateLabel(dd, type, value);

                            node.getProperties()
                                    .add(label);
                        }
                    }
                }
            }

            else if (action.type()
            .equals(ActionType.Removing)){
                if (action.compositeLabels().get(0)
                        .category()
                        .equals(LabelCategory.OutgoingData)) {
                    for (var behavior : dd.getBehavior()) {
                        List<Assignment> newAssignments = new ArrayList<>();
                        for (var assignment : behavior.getAssignment()) {
                            if (assignment.getId()
                                    .equals(outPinToAssignmentMap.get(action.domain()))) {
                                var type = action.compositeLabels().get(0)
                                        .label()
                                        .type();
                                var value = action.compositeLabels().get(0)
                                        .label()
                                        .value();
                                var label = getOrCreateLabel(dd, type, value);

                                if (assignment instanceof Assignment cast) {
                                    cast.getOutputLabels()
                                            .remove(label);
                                }
                                if (assignment instanceof SetAssignment cast) {
                                    cast.getOutputLabels()
                                            .remove(label);
                                }
                                if (assignment instanceof ForwardingAssignment) {
                                    var ddFactory = datadictionaryFactory.eINSTANCE;
                                    var assign = ddFactory.createAssignment();
                                    assign.getOutputLabels()
                                            .add(label);
                                    assign.setOutputPin(assignment.getOutputPin());
                                    var ddNOT = ddFactory.createNOT();
                                    assign.setTerm(ddNOT);
                                    newAssignments.add(assign);
                                }
                            }
                        }
                        if (!newAssignments.isEmpty())
                            behavior.getAssignment()
                                    .addAll(newAssignments);
                    }
                } else if (action.compositeLabels().get(0)
                        .category()
                        .equals(LabelCategory.Node)) {
                    for (var node : dfd.dataFlowDiagram()
                            .getNodes()) {
                        if (node.getId()
                                .equals(action.domain())) {
                            var type = action.compositeLabels().get(0)
                                    .label()
                                    .type();
                            var value = action.compositeLabels().get(0)
                                    .label()
                                    .value();
                            var label = getOrCreateLabel(dd, type, value);

                            node.getProperties()
                                    .remove(label);
                        }
                    }
                }

            }

        }
    }
    
    private void addNodes(DataFlowDiagramAndDictionary dfd, List<ActionTerm> actions) {
        var dd = dfd.dataDictionary();
        for (var action : actions) {
            if (action.type()
                    .equals(ActionType.AddNode)) {
                
                org.dataflowanalysis.dfd.dataflowdiagram.Node node = dfd.dataFlowDiagram().getNodes()
                .stream()
                .filter(n -> n.getId().equals(action.domain()))
                .findFirst()
                .orElseThrow();
                
                var name = action.compositeLabels().get(0).label().value();
                var behaviorOld = node.getBehavior();
                
                var dfdFactory = dataflowdiagramFactory.eINSTANCE;
                
                var vertex = dfdFactory.createProcess();
                vertex.setEntityName(name);
                
                List<Label> outgoingLabels = new ArrayList<>();
                
                for (var label : action.compositeLabels() ) {
                    var type = label.label().type();
                    var value = label.label().value();
                    var dfdLabel = getOrCreateLabel(dd, type, value);
                    
                    if (label instanceof NodeLabel) {
                        vertex.getProperties().add(dfdLabel);
                    }
                    else {
                        outgoingLabels.add(dfdLabel);
                    }
                }              
                
                var ddFactory = datadictionaryFactory.eINSTANCE;
                
                var behaviorNew = ddFactory.createBehavior();    
                
                var inPin = ddFactory.createPin();
                
                behaviorNew.getInPin().add(inPin);
                
                Set<Label> allLabels = new HashSet<>();
                
                for (var assignment: behaviorOld.getAssignment()) {
                    
                    if (assignment instanceof Assignment cast)
                        allLabels.addAll(cast.getOutputLabels());
                    
                    else if (assignment instanceof SetAssignment cast) 
                        allLabels.addAll(cast.getOutputLabels());
                    
                    var assign = ddFactory.createForwardingAssignment();
                    
                    assign.setOutputPin(assignment.getOutputPin());
                    
                    assign.getInputPins().add(inPin);
                    
                    if (!outgoingLabels.isEmpty()) {
                        var setAssign = ddFactory.createAssignment();
                        setAssign.setOutputPin(assignment.getOutputPin());
                        setAssign.getInputPins().add(inPin);
                        setAssign.getOutputLabels().addAll(outgoingLabels);
                        setAssign.setTerm(ddFactory.createTRUE());
                        
                        behaviorNew.getAssignment().add(setAssign);
                    }
                    
                    behaviorNew.getAssignment().add(assign);
                    behaviorNew.getOutPin().add(assignment.getOutputPin());
                    
                }
                
                vertex.setBehavior(behaviorNew);
                
                
                for (var flow: dfd.dataFlowDiagram().getFlows()) {
                    if (flow.getSourceNode() == node) {
                        flow.setSourceNode(vertex);
                        flow.setEntityName(name);
                    }
                }
                
                var flow = dfdFactory.createFlow();
                
                var outPin = ddFactory.createPin();
                flow.setEntityName(name);
                flow.setDestinationNode(vertex);
                flow.setDestinationPin(inPin);
                flow.setSourceNode(node);
                flow.setSourcePin(outPin);
                
                var assignmentNew = ddFactory.createAssignment();
                
                assignmentNew.setOutputPin(outPin);
                
                assignmentNew.getOutputLabels().addAll(allLabels);
                
                var ddTrue = ddFactory.createTRUE();
                
                assignmentNew.setTerm(ddTrue);
                
                behaviorOld.getAssignment().retainAll(List.of(assignmentNew));
                behaviorOld.getAssignment().add(assignmentNew);
                behaviorOld.getOutPin().retainAll(List.of(outPin));
                behaviorOld.getOutPin().add(outPin);
                
                dfd.dataFlowDiagram().getNodes().add(vertex);
                dd.getBehavior().add(behaviorNew);
                dfd.dataFlowDiagram().getFlows().add(flow);
            }
        }
    }
    
    private void addSinks(DataFlowDiagramAndDictionary dfd, List<ActionTerm> actions) {
        var dd = dfd.dataDictionary();
        List<org.dataflowanalysis.dfd.dataflowdiagram.Node> sinks = new ArrayList<>();
        
        for (var action : actions) {
            if (action.type()
                    .equals(ActionType.AddSink)) {
                
                org.dataflowanalysis.dfd.dataflowdiagram.Node node = dfd.dataFlowDiagram().getNodes()
                .stream()
                .filter(n -> n.getId().equals(action.domain()))
                .findFirst()
                .orElseThrow();

                var behavior = node.getBehavior();
                
                var dfdFactory = dataflowdiagramFactory.eINSTANCE;
                var ddFactory = datadictionaryFactory.eINSTANCE;
                
                var name = action.compositeLabels().get(0).label().value();
                
                org.dataflowanalysis.dfd.dataflowdiagram.Node vertex;
                
                if(sinks.stream().anyMatch(sink -> sink.getEntityName().equals(name))) {
                    vertex = sinks.stream()
                            .filter(sink -> sink.getEntityName().equals(name))
                            .findFirst().get();
                }
                
                else {
                    vertex = dfdFactory.createProcess();
                    vertex.setEntityName(name);
                    
                    var behaviorNew = ddFactory.createBehavior();    
                    
                    var inPin = ddFactory.createPin();
                    
                    behaviorNew.getInPin().add(inPin);
                    
                    vertex.setBehavior(behaviorNew);
                    
                    dd.getBehavior().add(behaviorNew);
                    
                    sinks.add(vertex);
                }
                
                for (var label : action.compositeLabels() ) {
                    var type = label.label().type();
                    var value = label.label().value();
                    var dfdLabel = getOrCreateLabel(dd, type, value);
                    
                    if (label instanceof NodeLabel) {
                        vertex.getProperties().add(dfdLabel);
                    }
                }              
                
                Set<Label> allLabels = new HashSet<>();
                
                var isForwarding = false;
                
                for (var assignment: behavior.getAssignment()) {
                    
                    if (assignment instanceof Assignment cast)
                        allLabels.addAll(cast.getOutputLabels());
                    
                    else if (assignment instanceof SetAssignment cast) 
                        allLabels.addAll(cast.getOutputLabels());
                    
                    else isForwarding = true;
                    
                }
                
                
                var flow = dfdFactory.createFlow();
                
                var outPin = ddFactory.createPin();
                flow.setEntityName(name);
                flow.setDestinationNode(vertex);
                flow.setDestinationPin(vertex.getBehavior().getInPin().get(0));
                flow.setSourceNode(node);
                flow.setSourcePin(outPin);
                
                if (!allLabels.isEmpty()) {
                    var assignmentNew = ddFactory.createAssignment();
                    
                    assignmentNew.setOutputPin(outPin);
                    
                    assignmentNew.getOutputLabels().addAll(allLabels);
                    
                    var ddTrue = ddFactory.createTRUE();
                    
                    assignmentNew.setTerm(ddTrue);
                    behavior.getAssignment().add(assignmentNew);
                }
                
                
                if (isForwarding) {
                    var forwardingAssignment = ddFactory.createForwardingAssignment();
                    forwardingAssignment.getInputPins().addAll(node.getBehavior().getInPin());
                    forwardingAssignment.setOutputPin(outPin);
                    behavior.getAssignment().add(forwardingAssignment);
                }
                
                
                behavior.getOutPin().add(outPin);
                
                dfd.dataFlowDiagram().getNodes().add(vertex);
                dfd.dataFlowDiagram().getFlows().add(flow);
            }
        }
    }
    
    private void removeNodes(DataFlowDiagramAndDictionary dfd, List<ActionTerm> actions) {
        var dd = dfd.dataDictionary();
        var dataFlowDiagram = dfd.dataFlowDiagram();
        var ddFactory = datadictionaryFactory.eINSTANCE;
        var dfdFactory = dataflowdiagramFactory.eINSTANCE;
        for (var action : actions) {
            if (action.type()
                    .equals(ActionType.RemoveNode)){
                org.dataflowanalysis.dfd.dataflowdiagram.Node node = dfd.dataFlowDiagram().getNodes()
                        .stream()
                        .filter(n -> n.getId().equals(action.domain()))
                        .findFirst()
                        .orElseThrow();
                
                var nodeBehavior = node.getBehavior();
                
                List<Flow> flowsToNode = dataFlowDiagram.getFlows().stream().filter(n -> n.getDestinationNode().equals(node)).toList();
                
                List<org.dataflowanalysis.dfd.dataflowdiagram.Node> previouseNodes = new ArrayList<>();
                
                for (Flow flow : flowsToNode) {
                    previouseNodes.add(flow.getSourceNode());
                }
                
                
                for (var assignment : nodeBehavior.getAssignment()) {
                    if (assignment instanceof ForwardingAssignment) {
                        var outPin = assignment.getOutputPin();
                        
                        var destinationFlow = dataFlowDiagram.getFlows().stream().filter(n -> n.getSourcePin().equals(outPin)).findFirst().orElseThrow();
                        
                        var destinationNode = destinationFlow.getDestinationNode();
                        
                        var destinationPin = destinationFlow.getDestinationPin();
                        
                        for (var vertex : previouseNodes) {
                            var relevantFlow = dataFlowDiagram.getFlows().stream().filter(n -> n.getSourceNode().equals(vertex)&& n.getDestinationNode().equals(node)).findFirst().orElseThrow();
                            
                            var vertexBehavior = vertex.getBehavior();
                            
                            var relevantAssignments = vertexBehavior.getAssignment().stream().filter(n -> n.getOutputPin().equals(relevantFlow.getSourcePin())).toList();
                            
                            
                            for (var relevantAssignment : relevantAssignments) {
                                
                                if (relevantAssignment instanceof ForwardingAssignment cast) {

                                    var newOutPin = ddFactory.createPin();
                                    
                                    
                                    var assignmentNew = ddFactory.createForwardingAssignment();
                                    
                                    assignmentNew.setOutputPin(newOutPin);
                                    
                                    assignmentNew.getInputPins().addAll(cast.getInputPins());
                                    
                                    vertexBehavior.getOutPin().add(newOutPin);
                                   
                                    vertexBehavior.getAssignment().add(assignmentNew); 
                                    
                                    vertexBehavior.getOutPin().add(newOutPin);
                                    
                                    
                                    
                                    var flow = dfdFactory.createFlow();
                                    flow.setDestinationNode(destinationNode);
                                    flow.setDestinationPin(destinationPin);
                                    flow.setSourceNode(vertex);
                                    flow.setSourcePin(newOutPin);
                                    flow.setEntityName(vertex.getEntityName() + "_" + destinationNode.getEntityName());
                                    dataFlowDiagram.getFlows().add(flow);
                                    
                                }
                                
                                else {
                                    Set<Label> outputLabels = new HashSet<>();
                                    
                                    if (relevantAssignment instanceof Assignment cast)
                                        outputLabels.addAll(cast.getOutputLabels());
                                    
                                    else if (relevantAssignment instanceof SetAssignment cast) 
                                        outputLabels.addAll(cast.getOutputLabels());
                                    
                                    var newOutPin = ddFactory.createPin();
                                    
                                    var assignmentNew = ddFactory.createAssignment();
                                    
                                    assignmentNew.setOutputPin(newOutPin);
                                    
                                    vertexBehavior.getOutPin().add(newOutPin);
                                    
                                    assignmentNew.getOutputLabels().addAll(outputLabels);
                                    
                                    var ddTrue = ddFactory.createTRUE();
                                    
                                    assignmentNew.setTerm(ddTrue);
                                    
                                    vertexBehavior.getAssignment().add(assignmentNew);
                                    
                                    var flow = dfdFactory.createFlow();
                                    flow.setDestinationNode(destinationNode);
                                    flow.setDestinationPin(destinationPin);
                                    flow.setSourceNode(vertex);
                                    flow.setSourcePin(newOutPin);
                                    flow.setEntityName(vertex.getEntityName() + "_" + destinationNode.getEntityName());
                                    dataFlowDiagram.getFlows().add(flow);
                                }
                                
                                vertexBehavior.getAssignment().remove(relevantAssignment);
                            }
                            
                            vertexBehavior.getOutPin().remove(relevantFlow.getSourcePin());
                        }
                        
                    }
                    
                }
                
                for (var flow : dataFlowDiagram.getFlows().stream().filter(n -> n.getDestinationNode().equals(node)).toList()) {
                    dfd.dataFlowDiagram().getFlows().remove(flow);
                }
                for (var flow : dataFlowDiagram.getFlows().stream().filter(n -> n.getSourceNode().equals(node)).toList()) {
                    dfd.dataFlowDiagram().getFlows().remove(flow);
                }
                
                
                dd.getBehavior().remove(nodeBehavior);
                dataFlowDiagram.getNodes().remove(node);                                                                                          
                
            }
        }
    }
    
    private void removeFlows(DataFlowDiagramAndDictionary dfd, List<ActionTerm> actions) {
        var dd = dfd.dataDictionary();
        var dataFlowDiagram = dfd.dataFlowDiagram();
        for (var action : actions) {
            if (action.type()
                    .equals(ActionType.RemoveFlow)){
                var flow = dataFlowDiagram.getFlows().stream().filter(n -> n.getEntityName().equals(action.domain())).findFirst().orElseThrow();
                
                var assignment = flow.getSourceNode().getBehavior().getAssignment().stream().filter(n -> n.getOutputPin().equals(flow.getSourcePin())).findFirst().orElseThrow();
                
                flow.getSourceNode().getBehavior().getAssignment().remove(assignment);
                flow.getSourceNode().getBehavior().getOutPin().remove(flow.getSourcePin());
                
         
                dataFlowDiagram.getFlows().remove(flow);
            }
        }
    }
    
    private Label getOrCreateLabel(DataDictionary dd, String type, String value) {
        var optionalLabel = dd.getLabelTypes()
                .stream()
                .filter(labelType -> labelType.getEntityName()
                        .equals(type))
                .flatMap(labelType -> labelType.getLabel()
                        .stream())
                .filter(labelValue -> labelValue.getEntityName()
                        .equals(value))
                .findAny();

        Label label;

        if (optionalLabel.isPresent()) {
            label = optionalLabel.get();
        } else {
            logger.warn("Could not find label " + type + "." + value + " in Dictionary. Therefore creating this label.");
            var ddFactory = datadictionaryFactory.eINSTANCE;
            label = ddFactory.createLabel();
            label.setEntityName(value);
            label.setId(UUID.nameUUIDFromBytes(value.getBytes())
                    .toString());

            var optionalLabelType = dd.getLabelTypes()
                    .stream()
                    .filter(lt -> lt.getEntityName()
                            .equals(type))
                    .findFirst();

            LabelType labelType;

            if (optionalLabelType.isPresent()) {
                labelType = optionalLabelType.get();
            } else {
                labelType = ddFactory.createLabelType();
                labelType.setEntityName(type);
                labelType.setId(UUID.nameUUIDFromBytes(type.getBytes())
                        .toString());
                dd.getLabelTypes()
                        .add(labelType);
            }

            labelType.getLabel()
                    .add(label);
        }
        return label;
    }

    private void deriveOutPinsToAssignmentsMap(DataFlowDiagramAndDictionary dfd) {
        for (var node : dfd.dataFlowDiagram()
                .getNodes()) {
            for (var assignment : node.getBehavior()
                    .getAssignment()) {
                var outPin = assignment.getOutputPin();
                outPinToAssignmentMap.put(outPin.getId(), assignment.getId());
            }
        }
    }
}
