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

        var solver = new ILPSolver();
        var result = solver.solve(mitigations, allMitigations);

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

    private List<ActionTerm> getActions(List<Mitigation> result) {
        List<Mitigation> additional = new ArrayList<>();
        for (var mit : result) {
            additional.addAll(mit.required());
        }
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

    private List<MitigationStrategy> getAdditionalMitigations(List<CompositeLabel> labels) {
        for (var label : labels) {
            for (var constraint : constraints) {
                if (constraint.isPrecondition(label))
                    return constraint.getMitigations();
            }
        }
        return null;
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

            else {
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
                    
                    else continue;
                    
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
