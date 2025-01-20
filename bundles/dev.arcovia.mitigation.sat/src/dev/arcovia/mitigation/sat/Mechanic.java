package dev.arcovia.mitigation.sat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.core.AbstractTransposeFlowGraph;
import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.WebEditorConverter;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryFactory;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import java.util.HashSet;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;

public class Mechanic {
    Map<String, String> outPinToAss = new HashMap<>();

    private DataFlowDiagramAndDictionary dfd;
    private List<Constraint> constraints;
    private Map<Label, Integer> costs;
    private List<Node> nodes;
    private List<Flow> flows;

    private final Logger logger = Logger.getLogger(Mechanic.class);

    public Mechanic(String dfdLocation, List<Constraint> constraints, Map<Label, Integer> costs) {
        this.dfd = new WebEditorConverter().webToDfd(dfdLocation);
        this.constraints = constraints;
        this.costs = costs;
        this.nodes = new ArrayList<>();
        this.flows = new ArrayList<>();
    }

    public Mechanic(String dfdLocation, List<Constraint> constraints) {
        this(dfdLocation, constraints, null);
    }

    public DataFlowDiagramAndDictionary repair() throws ContradictionException, TimeoutException, IOException {
        List<AbstractTransposeFlowGraph> violatingTFGs = determineViolatingTFGs(dfd, constraints);
        deriveOutPinsToAssignmentsMap(dfd);

        getNodesAndFlows(violatingTFGs);
        var solutions = new Sat().solve(nodes, flows, constraints);

        List<Term> chosenSolution = getChosenSolution(solutions);

        List<Term> flatendNodes = getFlatNodes(nodes);

        List<Term> actions = getActions(chosenSolution, flatendNodes);
        applyActions(dfd, actions);

        return dfd;
    }

    private List<Term> getChosenSolution(List<List<Term>> solutions) {
        if(costs != null) {
            for(var constraint : constraints) {
                for(var term : constraint.literals()) {
                    if(!costs.keySet().contains(term.compositeLabel().label())) {
                        logger.warn("Cost of " + term.compositeLabel().label().toString() + " is missing. Defaulting to minimal solution.");
                        return getMinimalSolution(solutions);
                    }
                }
            }
            return getCheapestSolution(solutions, costs);
        }
        else {
            return getMinimalSolution(solutions);
        }
    }

    private List<AbstractTransposeFlowGraph> determineViolatingTFGs(DataFlowDiagramAndDictionary dfd, List<Constraint> constraints) {
        var ressourceProvider = new DFDModelResourceProvider(dfd.dataDictionary(), dfd.dataFlowDiagram());
        var analysis = new DFDDataFlowAnalysisBuilder().standalone()
                .useCustomResourceProvider(ressourceProvider)
                .build();

        analysis.initializeAnalysis();
        var flowGraph = analysis.findFlowGraphs();
        flowGraph.evaluate();
        Set<AbstractTransposeFlowGraph> violatingTransposeFlowGraphs = new HashSet<>();

        for (var tfg : flowGraph.getTransposeFlowGraphs()) {
            if (checkConstraints(tfg, constraints))
                violatingTransposeFlowGraphs.add(tfg);
        }
        return new ArrayList<AbstractTransposeFlowGraph>(violatingTransposeFlowGraphs);
    }

    private boolean checkConstraints(AbstractTransposeFlowGraph tfg, List<Constraint> constraints) {
        for (var constraint : constraints) {
            if (checkConstraint(tfg, constraint.literals()))
                return true;
        }
        return false;
    }

    private boolean checkConstraint(AbstractTransposeFlowGraph tfg, List<Literal> constraint) {
        List<String> negativeLiterals = new ArrayList<>();
        List<String> positveLiterals = new ArrayList<>();
        for (var literal : constraint) {
            if (literal.positive())
                positveLiterals.add(literal.compositeLabel()
                        .toString());
            else
                negativeLiterals.add(literal.compositeLabel()
                        .toString());
        }

        for (var node : tfg.getVertices()) {
            Set<String> nodeLiterals = new HashSet<>();
            for (var nodeChar : node.getAllVertexCharacteristics()) {
                nodeLiterals.add(new NodeLabel(new Label(nodeChar.getTypeName(), nodeChar.getValueName())).toString());
            }
            for (var variables : node.getAllIncomingDataCharacteristics()) {
                for (var dataChar : variables.getAllCharacteristics()) {
                    nodeLiterals.add(new IncomingDataLabel(new Label(dataChar.getTypeName(), dataChar.getValueName())).toString());
                }
            }

            if (nodeLiterals.stream()
                    .anyMatch(positveLiterals::contains)) {
                continue;
            } else if (!nodeLiterals.containsAll(negativeLiterals)) {
                continue;
            } else {
                return true;
            }
        }
        return false;
    }

    private void getNodesAndFlows(List<AbstractTransposeFlowGraph> violatingTFGs) {
        for (var tfg : violatingTFGs) {
            for (var vertex : tfg.getVertices()) {

                DFDVertex node = (DFDVertex) vertex;

                Map<InPin, List<Label>> inPins = new HashMap<>();
                for (var inPin : node.getAllIncomingDataCharacteristics()) {
                    List<Label> pinChars = new ArrayList<>();
                    for (var property : inPin.getAllCharacteristics()) {
                        var type = property.getTypeName();
                        var value = property.getValueName();
                        pinChars.add(new Label(type, value));
                    }
                    inPins.put(new InPin(inPin.getVariableName()), pinChars);
                }

                List<Label> nodeLabels = new ArrayList<>();
                for (var property : node.getAllVertexCharacteristics()) {
                    var type = property.getTypeName();
                    var value = property.getValueName();
                    nodeLabels.add(new Label(type, value));
                }

                Map<OutPin, List<Label>> outPinLabelMap = new HashMap<>();
                for (var outPin : node.getAllOutgoingDataCharacteristics()) {
                    List<Label> pinLabel = new ArrayList<>();
                    for (var property : outPin.getAllCharacteristics()) {
                        var type = property.getTypeName();
                        var value = property.getValueName();
                        pinLabel.add(new Label(type, value));
                    }
                    outPinLabelMap.put(new OutPin(outPin.getVariableName()), pinLabel);
                }

                nodes.add(new Node(node.getName(), inPins, outPinLabelMap, nodeLabels));

                for (var pin : node.getPinFlowMap()
                        .keySet()) {
                    var flow = node.getPinFlowMap()
                            .get(pin);
                    flows.add(new Flow(new OutPin(flow.getSourcePin()
                            .getId()), new InPin(pin.getId())));
                }
            }
        }
    }

    private void deriveOutPinsToAssignmentsMap(DataFlowDiagramAndDictionary dfd) {
        for (var node : dfd.dataFlowDiagram()
                .getNodes()) {
            for (var assignment : node.getBehavior()
                    .getAssignment()) {
                var outPin = assignment.getOutputPin();
                outPinToAss.put(outPin.getId(), assignment.getId());
            }
        }
    }

    private List<Term> getMinimalSolution(List<List<Term>> solutions) {
        Collections.sort(solutions, (list1, list2) -> Integer.compare(list1.size(), list2.size()));
        return solutions.get(0);
    }

    private List<Term> getCheapestSolution(List<List<Term>> solutions, Map<Label, Integer> costs) {
        int minCost = Integer.MAX_VALUE;
        List<Term> cheapestSolution = null;
        for (var solution : solutions) {
            int cost = 0;
            for (var term : solution) {                
                cost += costs.get(term.compositeLabel()
                        .label());
            }
            if (cost < minCost) {
                minCost = cost;
                cheapestSolution = solution;
            }
        }
        return cheapestSolution;
    }

    private List<Term> getFlatNodes(List<Node> nodes) {
        List<Term> flatendNodes = new ArrayList<>();
        for (var node : nodes) {
            for (var outPin : node.outPins()
                    .keySet()) {
                for (var label : node.outPins()
                        .get(outPin)) {
                    flatendNodes.add(new Term(outPin.id(), new OutgoingDataLabel(label)));
                }
            }
            for (var property : node.nodeChars()) {
                flatendNodes.add(new Term(node.name(), new NodeLabel(new Label(property.type(), property.value()))));
            }
        }
        return flatendNodes;
    }

    private List<Term> getActions(List<Term> minimalSolution, List<Term> flatendNodes) {
        List<Term> actions = new ArrayList<>();
        for (var delta : minimalSolution) {
            if (delta.compositeLabel()
                    .category()
                    .equals(LabelCategory.IncomingData))
                continue;
            if (flatendNodes.contains(delta))
                continue;
            actions.add(delta);
        }
        return actions;
    }

    private void applyActions(DataFlowDiagramAndDictionary dfd, List<Term> actions) {
        var dd = dfd.dataDictionary();

        for (var action : actions) {
            if (action.compositeLabel()
                    .category()
                    .equals(LabelCategory.OutgoingData)) {
                for (var behavior : dd.getBehavior()) {
                    List<Assignment> newAssignments = new ArrayList<>();
                    for (var assignment : behavior.getAssignment()) {
                        if (assignment.getId()
                                .equals(outPinToAss.get(action.domain()))) {
                            var type = action.compositeLabel()
                                    .label()
                                    .type();
                            var value = action.compositeLabel()
                                    .label()
                                    .value();
                            var label = getOrCreateLabel(dd, type, value);
                               
                            if (assignment instanceof Assignment cast) {
                                cast.getOutputLabels()
                                        .add(label);
                            }
                            if (assignment instanceof ForwardingAssignment cast) {
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
            } else if (action.compositeLabel()
                    .category()
                    .equals(LabelCategory.Node)) {
                for (var node : dfd.dataFlowDiagram()
                        .getNodes()) {
                    if (node.getEntityName()
                            .equals(action.domain())) {
                        var type = action.compositeLabel()
                                .label()
                                .type();
                        var value = action.compositeLabel()
                                .label()
                                .value();
                        var label = getOrCreateLabel(dd, type, value);

                        node.getProperties()
                                .add(label);
                    }
                }
            }
        }
    }

    private org.dataflowanalysis.dfd.datadictionary.Label getOrCreateLabel(DataDictionary dd, String type, String value) {
        var optionalLabel = dd.getLabelTypes()
                .stream()
                .filter(labelType -> labelType.getEntityName()
                        .equals(type))
                .flatMap(labelType -> labelType.getLabel()
                        .stream())
                .filter(labelValue -> labelValue.getEntityName()
                        .equals(value))
                .findAny();
        
        org.dataflowanalysis.dfd.datadictionary.Label label;
        
        if(!optionalLabel.isEmpty()) {
            label = optionalLabel.get();
        }                 
        else {
            logger.warn("Couldn´t find label " + type + "." + value + " in Dictionary. Therefore creating this label.");
            var ddFactory = datadictionaryFactory.eINSTANCE;
            label = ddFactory.createLabel();
            label.setEntityName(value);
            label.setId(UUID.nameUUIDFromBytes(value.getBytes()).toString());

            var optionalLabelType = dd.getLabelTypes()
                    .stream()
                    .filter(lt -> lt.getEntityName()
                            .equals(type))
                    .findFirst();

            LabelType labelType; 

            if (!optionalLabelType.isEmpty()) {
                labelType = optionalLabelType.get();
            }
            else {
                labelType = ddFactory.createLabelType(); 
                labelType.setEntityName(type);
                labelType.setId(UUID.nameUUIDFromBytes(type.getBytes()).toString());
                dd.getLabelTypes().add(labelType);
            }
            
            labelType.getLabel().add(label);   
        }
        return label;
    }
}
