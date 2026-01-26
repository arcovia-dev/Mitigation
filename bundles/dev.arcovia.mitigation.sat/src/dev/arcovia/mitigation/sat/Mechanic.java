package dev.arcovia.mitigation.sat;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;
import java.util.Comparator;

import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.core.AbstractTransposeFlowGraph;
import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.web2dfd.WebEditorConverterModel;
import org.dataflowanalysis.converter.web2dfd.Web2DFDConverter;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryFactory;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;

/**
 * The Mechanic class is designed to operate on a Data Flow Diagram (DFD) and its associated dictionary, applying
 * constraints and repairing violations found within the DFD. It provides the ability to assess whether a DFD and its
 * constraints are consistent and suggests corrections to resolve violations.
 */
public class Mechanic {
    Map<String, String> outPinToAss = new HashMap<>();

    private final DataFlowDiagramAndDictionary dfd;
    private final List<Constraint> constraints;
    private final Map<Label, Integer> costs;
    private List<Node> nodes;
    private List<Flow> flows;
    private final String dfdName;
    private int violations = 0;

    private boolean deactivateViolatingTFGsOnly = false;
    private boolean deactivateOnlyRepairingLabels = false;
    private boolean deactivateSimultaneouseRepair = false;
    private boolean deactivateSubsumption = false;

    private final Logger logger = Logger.getLogger(Mechanic.class);

    /**
     * Constructs a new Mechanic instance with the specified Web Data Flow Diagram (DFD) location, constraints, and mapping
     * of costs for specific labels. This constructor initializes the DFD, its associated name, constraints, costs, and
     * prepares the internal data structures for nodes and flows.
     * @param dfdLocation the file location of the Web Data Flow Diagram (DFD)
     * @param constraints the list of constraints to be applied to the DFD
     * @param costs the map associating labels with their respective integer costs
     */
    public Mechanic(DataFlowDiagramAndDictionary dfd, String dfdName, List<Constraint> constraints, Map<Label, Integer> costs,
            List<Boolean> complexityReductions) {
        this.dfd = dfd;
        this.dfdName = dfdName;
        this.constraints = constraints;
        this.costs = costs;
        this.nodes = new ArrayList<>();
        this.flows = new ArrayList<>();
        this.deactivateViolatingTFGsOnly = complexityReductions.get(0);
        this.deactivateOnlyRepairingLabels = complexityReductions.get(1);
        this.deactivateSimultaneouseRepair = complexityReductions.get(2);
        this.deactivateSubsumption = complexityReductions.get(3);
    }

    /**
     * Constructs a new Mechanic instance with the specified Web Data Flow Diagram (DFD) location, constraints, and mapping
     * of costs for specific labels. This constructor initializes the DFD, its associated name, constraints, costs, and
     * prepares the internal data structures for nodes and flows.
     * @param dfdLocation the file location of the Web Data Flow Diagram (DFD)
     * @param constraints the list of constraints to be applied to the DFD
     * @param costs the map associating labels with their respective integer costs
     */
    public Mechanic(String dfdLocation, List<Constraint> constraints, Map<Label, Integer> costs) {
        this.dfd = new Web2DFDConverter().convert(new WebEditorConverterModel(dfdLocation));
        var name = Paths.get(dfdLocation)
                .getFileName()
                .toString();
        this.dfdName = name.substring(0, name.lastIndexOf('.'));
        this.constraints = constraints;
        this.costs = costs;
        this.nodes = new ArrayList<>();
        this.flows = new ArrayList<>();
    }

    /**
     * Constructs a new Mechanic instance with the specified Web Data Flow Diagram (DFD) location and constraints. This
     * constructor initializes the DFD, its associated name, and constraints. Internal data structures for nodes and flows
     * are also prepared.
     * @param dfdLocation the file location of the Web Data Flow Diagram (DFD)
     * @param constraints the list of constraints to be applied to the DFD
     */
    public Mechanic(String dfdLocation, List<Constraint> constraints) {
        this(dfdLocation, constraints, null);
    }

    /**
     * Constructs a new Mechanic instance with the specified Data Flow Diagram and Dictionary (DFD), DFD name, constraints,
     * and label costs. This constructor initializes the DFD, its associated name, constraints, costs, and prepares the
     * internal data structures for nodes and flows.
     * @param dfd the Data Flow Diagram and Dictionary (DFD) to be processed
     * @param dfdName the name associated with the Data Flow Diagram
     * @param constraints the list of constraints to be applied to the DFD
     * @param costs the map associating labels with their respective integer costs
     */
    public Mechanic(DataFlowDiagramAndDictionary dfd, String dfdName, List<Constraint> constraints, Map<Label, Integer> costs) {
        this.dfd = dfd;
        this.dfdName = dfdName;
        this.constraints = constraints;
        this.costs = costs;
        this.nodes = new ArrayList<>();
        this.flows = new ArrayList<>();
    }

    /**
     * Constructs a new Mechanic instance with the specified Data Flow Diagram and Dictionary (DFD), DFD name, and
     * constraints. This constructor initializes the DFD, its associated name, constraints, and prepares the internal data
     * structures for nodes and flows.
     * @param dfd the Data Flow Diagram and Dictionary (DFD) to be processed
     * @param dfdName the name associated with the Data Flow Diagram
     * @param constraints the list of constraints to be applied to the DFD
     */
    public Mechanic(DataFlowDiagramAndDictionary dfd, String dfdName, List<Constraint> constraints) {
        this(dfd, dfdName, constraints, null);
    }

    /**
     * Repairs the given Data Flow Diagram and Dictionary (DFD) by analyzing violations against constraints, identifying
     * necessary corrective actions, and applying those corrections to provide an updated and valid DFD. The method first
     * determines the violating Transpose Flow Graphs (TFGs) in the DFD and analyzes the nodes and flows to be adjusted. If
     * no violations are found, the original DFD is returned. Otherwise, it solves for feasible corrective actions, chooses
     * the best solution, and applies the appropriate changes to the DFD.
     * @return the updated Data Flow Diagram and Dictionary (DFD) that adheres to the specified constraints
     * @throws ContradictionException if the constraint solving process encounters a contradiction
     * @throws TimeoutException if the constraint solving process exceeds the predefined time limit
     * @throws IOException if there is an error reading or writing the DFD data during the repair
     */
    public DataFlowDiagramAndDictionary repair() throws ContradictionException, TimeoutException, IOException {
        List<AbstractTransposeFlowGraph> violatingTFGs = determineViolatingTFGs(dfd, constraints);

        if (!violatingTFGs.isEmpty() && this.deactivateViolatingTFGsOnly) {
            violatingTFGs = getAllTFGs(dfd);
        }

        // If there are no violations repairs are not needed
        if (violatingTFGs.isEmpty()) {
            logger.warn("Analysis has no violations found in DFD");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(("testresults/" + dfdName + ".cnf")))) {
                writer.write("p cnf " + 0 + " " + 0);
                writer.newLine();
            }
            return dfd;
        }

        deriveOutPinsToAssignmentsMap(dfd);

        if (deactivateSimultaneouseRepair)
            return TFGBasedRepair(violatingTFGs);

        getNodesAndFlows(violatingTFGs);
        sortNodesAndFlows();

        Set<Label> allLabels = null;

        if (deactivateOnlyRepairingLabels) {
            allLabels = getAllLabels();
        }

        var solutions = new Sat().solve(nodes, flows, constraints, dfdName, deactivateSubsumption, allLabels);
        List<Term> flatendNodes = getFlatNodes(nodes);

        List<Term> chosenSolution = getChosenSolution(solutions, flatendNodes);

        List<Term> actions = getActions(chosenSolution, flatendNodes);

        applyActions(dfd, actions);

        return dfd;
    }

    /**
     * Determines whether the given Data Flow Diagram and Dictionary (DFD) adheres to all specified constraints. This method
     * evaluates the DFD by analyzing its Transpose Flow Graphs (TFGs) against the provided constraints. If no violations
     * are found, the DFD is deemed violation-free.
     * @param dfd the Data Flow Diagram and Dictionary (DFD) to be evaluated
     * @param constraints the list of constraints that the DFD must satisfy
     * @return true if the DFD satisfies all constraints and is violation-free, false otherwise
     */
    public Boolean isViolationFree(DataFlowDiagramAndDictionary dfd, List<Constraint> constraints) {
        return (determineViolatingTFGs(dfd, constraints).isEmpty());
    }

    /**
     * Calculates the number of violations in a given Data Flow Diagram and Dictionary (DFD) based on the provided
     * constraints. This method uses the internal logic to determine the Transpose Flow Graphs (TFGs) within the DFD that do
     * not adhere to the specified constraints, and returns the count of such violations.
     * @param dfd the Data Flow Diagram and Dictionary (DFD) to be analyzed for violations
     * @param constraints the list of constraints that the DFD should satisfy
     * @return the number of violations found in the DFD based on the provided constraints
     */
    public int amountOfViolations(DataFlowDiagramAndDictionary dfd, List<Constraint> constraints) {
        return (determineViolatingTFGs(dfd, constraints).size());
    }

    public List<AbstractTransposeFlowGraph> getViolatingTFGs(DataFlowDiagramAndDictionary dfd, List<Constraint> constraints) {
        return determineViolatingTFGs(dfd, constraints);
    }

    /**
     * Retrieves the current number of violations identified in the Data Flow Diagram and Dictionary (DFD).
     * @return the number of violations as an integer
     */
    public int getViolations() {
        return violations;
    }

    private DataFlowDiagramAndDictionary TFGBasedRepair(List<AbstractTransposeFlowGraph> violatingTFGs)
            throws ContradictionException, TimeoutException, IOException {
        Set<Term> ALLsolutions = new HashSet<>();

        for (var tfg : violatingTFGs) {
            if (!checkConstraints(tfg, constraints))
                continue;

            getNodesAndFlows(List.of(tfg));
            sortNodesAndFlows();

            Set<Label> allLabels = null;

            if (deactivateOnlyRepairingLabels) {
                allLabels = getAllLabels();
            }

            var solutions = new Sat().solve(nodes, flows, constraints, dfdName, deactivateSubsumption, allLabels);
            List<Term> flatendNodes = getFlatNodes(nodes);

            ALLsolutions.addAll(getChosenSolution(solutions, flatendNodes));

        }
        getNodesAndFlows(violatingTFGs);
        sortNodesAndFlows();
        List<Term> flatendNodes = getFlatNodes(nodes);

        List<Term> solutions = new ArrayList<Term>();

        for (var solution : ALLsolutions)
            solutions.add(solution);

        List<Term> actions = getActions(solutions, flatendNodes);

        applyActions(dfd, actions);

        return dfd;
    }

    private Set<Label> getAllLabels() {
        Set<Label> labels = new HashSet<>();

        for (var labelType : dfd.dataDictionary()
                .getLabelTypes()) {
            for (var label : labelType.getLabel()) {
                labels.add(new Label(labelType.getEntityName(), label.getEntityName()));
            }
        }
        return labels;
    }

    private List<Term> getChosenSolution(List<List<Term>> solutions, List<Term> flatendNodes) {
        if (costs != null) {
            for (var constraint : constraints) {
                for (var term : constraint.literals()) {
                    if (term.positive() && !costs.containsKey(term.compositeLabel()
                            .label())) {
                        logger.warn("Cost of " + term.compositeLabel()
                                .label()
                                .toString() + " is missing. Defaulting to minimal solution.");
                        return getMinimalSolution(solutions);
                    }
                }
            }
            return getCheapestSolution(solutions, costs, flatendNodes);
        } else {
            return getMinimalSolution(solutions);
        }
    }

    private List<AbstractTransposeFlowGraph> getAllTFGs(DataFlowDiagramAndDictionary dfd) {
        var resourceProvider = new DFDModelResourceProvider(dfd.dataDictionary(), dfd.dataFlowDiagram());
        var analysis = new DFDDataFlowAnalysisBuilder().standalone()
                .useCustomResourceProvider(resourceProvider)
                .build();

        analysis.initializeAnalysis();
        var flowGraph = analysis.findFlowGraphs();
        flowGraph.evaluate();
        Set<AbstractTransposeFlowGraph> violatingTransposeFlowGraphs = new HashSet<>();

        for (var tfg : flowGraph.getTransposeFlowGraphs()) {
            violatingTransposeFlowGraphs.add(tfg);
        }
        return new ArrayList<>(violatingTransposeFlowGraphs);
    }

    private List<AbstractTransposeFlowGraph> determineViolatingTFGs(DataFlowDiagramAndDictionary dfd, List<Constraint> constraints) {
        var resourceProvider = new DFDModelResourceProvider(dfd.dataDictionary(), dfd.dataFlowDiagram());
        var analysis = new DFDDataFlowAnalysisBuilder().standalone()
                .useCustomResourceProvider(resourceProvider)
                .build();

        analysis.initializeAnalysis();
        var flowGraph = analysis.findFlowGraphs();
        flowGraph.evaluate();
        Set<AbstractTransposeFlowGraph> violatingTransposeFlowGraphs = new HashSet<>();

        for (var tfg : flowGraph.getTransposeFlowGraphs()) {
            if (checkAllConstraints(tfg, constraints))
                violatingTransposeFlowGraphs.add(tfg);
        }
        return new ArrayList<>(violatingTransposeFlowGraphs);
    }

    private boolean checkConstraints(AbstractTransposeFlowGraph tfg, List<Constraint> constraints) {
        for (var constraint : constraints) {
            if (checkConstraint(tfg, constraint.literals()))
                return true;
        }
        return false;
    }

    private boolean checkAllConstraints(AbstractTransposeFlowGraph tfg, List<Constraint> constraints) {
        boolean violation = false;
        for (var constraint : constraints) {
            if (checkConstraint(tfg, constraint.literals())) {
                violation = true;
                violations++;
            }
        }
        return violation;
    }

    private boolean checkConstraint(AbstractTransposeFlowGraph tfg, List<Literal> constraint) {
        List<String> negativeLiterals = new ArrayList<>();
        List<String> positiveLiterals = new ArrayList<>();
        for (var literal : constraint) {
            if (literal.positive())
                positiveLiterals.add(literal.compositeLabel()
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
                    .anyMatch(positiveLiterals::contains)) {
                continue;
            } else if (!nodeLiterals.containsAll(negativeLiterals)) {
                continue;
            } else if (node.getAllIncomingDataCharacteristics()
                    .isEmpty()) {
                var missingLiterals = new HashSet<>(positiveLiterals);
                missingLiterals.removeAll(nodeLiterals);
                for (var lit : missingLiterals) {
                    if (!lit.contains("IncomingData"))
                        return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    Comparator<OutPin> outPinComparator = (s1, s2) -> {
        boolean isNumeric1 = s1.id()
                .matches("\\d+");
        boolean isNumeric2 = s2.id()
                .matches("\\d+");

        if (isNumeric1 && isNumeric2) {
            return Integer.compare(Integer.parseInt(s1.id()), Integer.parseInt(s2.id())); // Sort numerically
        } else if (!isNumeric1 && !isNumeric2) {
            return s1.id()
                    .compareTo(s2.id()); // Sort lexicographically
        } else {
            return isNumeric1 ? -1 : 1; // Numbers first, then UUIDs
        }
    };

    private void getNodesAndFlows(List<AbstractTransposeFlowGraph> violatingTFGs) {
        for (var tfg : violatingTFGs) {
            for (var vertex : tfg.getVertices()) {
                DFDVertex node = (DFDVertex) vertex;
                final String id = node.getReferencedElement()
                        .getId();
                Map<InPin, List<Label>> inPinLabelMap = new HashMap<>();

                for (var inPin : node.getAllIncomingDataCharacteristics()) {
                    List<Label> pinChars = new ArrayList<>();
                    for (var property : inPin.getAllCharacteristics()) {
                        var type = property.getTypeName();
                        var value = property.getValueName();
                        pinChars.add(new Label(type, value));
                    }
                    inPinLabelMap.put(new InPin(inPin.getVariableName()), pinChars);
                }

                TreeMap<OutPin, List<Label>> outPinLabelMap = new TreeMap<>(outPinComparator);
                for (var outPin : node.getAllOutgoingDataCharacteristics()) {
                    List<Label> pinLabel = new ArrayList<>();
                    for (var property : outPin.getAllCharacteristics()) {
                        var type = property.getTypeName();
                        var value = property.getValueName();
                        pinLabel.add(new Label(type, value));
                    }
                    outPinLabelMap.put(new OutPin(outPin.getVariableName()), pinLabel);
                }
                // if not in list add otherwise add missing pins

                if (nodes.stream()
                        .noneMatch(n -> n.id()
                                .equals(id))) {
                    List<Label> nodeLabels = new ArrayList<>();
                    for (var property : node.getAllVertexCharacteristics()) {
                        var type = property.getTypeName();
                        var value = property.getValueName();
                        nodeLabels.add(new Label(type, value));
                    }
                    nodes.add(new Node(id, inPinLabelMap, outPinLabelMap, nodeLabels));

                } else {
                    var satNode = nodes.stream()
                            .filter(n -> n.id()
                                    .equals(id))
                            .findFirst()
                            .get();
                    for (var inPin : inPinLabelMap.keySet()) {
                        if (satNode.inPins()
                                .containsKey(inPin)) {
                            Set<Label> inPinLabel = new HashSet<>(inPinLabelMap.get(inPin));
                            inPinLabel.addAll(satNode.inPins()
                                    .get(inPin));
                            satNode.inPins()
                                    .put(inPin, new ArrayList<>(inPinLabel));
                        } else
                            satNode.inPins()
                                    .put(inPin, inPinLabelMap.get(inPin));
                    }
                    for (var outPin : outPinLabelMap.keySet()) {
                        if (satNode.outPins()
                                .containsKey(outPin)) {
                            Set<Label> outPinLabel = new HashSet<>(outPinLabelMap.get(outPin));
                            var enforcingLabels = constraints.stream()
                                    .flatMap(c -> c.literals()
                                            .stream()) // Flatten all literals from all constraints
                                    .filter(Literal::positive) // Keep only positive literals
                                    .map(literal -> literal.compositeLabel()
                                            .label()) // Extract their labels
                                    .toList();
                            var newLabels = new ArrayList<>(satNode.outPins()
                                    .get(outPin));
                            var sameLabels = outPinLabel.stream()
                                    .filter(newLabels::contains)
                                    .toList();

                            outPinLabel.addAll(newLabels);
                            outPinLabel.removeAll(enforcingLabels);
                            outPinLabel.addAll(sameLabels);

                            satNode.outPins()
                                    .put(outPin, new ArrayList<>(outPinLabel));
                        } else
                            satNode.outPins()
                                    .put(outPin, outPinLabelMap.get(outPin));
                    }
                }

                for (var pin : node.getPinFlowMap()
                        .keySet()) {
                    var flow = node.getPinFlowMap()
                            .get(pin);

                    var SatFlow = new Flow(new OutPin(flow.getSourcePin()
                            .getId()), new InPin(pin.getId()));
                    if (!flows.contains(SatFlow)) {
                        flows.add(SatFlow);
                    }
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

    private void sortNodesAndFlows() {
        nodes.sort(Comparator.comparing(node -> node.id()));
        flows.sort(Comparator.comparing((Flow flow) -> flow.source()
                .id())
                .thenComparing(flow -> flow.sink()
                        .id()));
        for (var node : nodes) {
            for (var labels : node.inPins()
                    .values()) {
                labels.sort(Comparator.comparing(label -> label.toString()));
            }
            for (var labels : node.outPins()
                    .values()) {
                labels.sort(Comparator.comparing(label -> label.toString()));
            }
            node.nodeChars()
                    .sort(Comparator.comparing(label -> label.toString()));
        }
    }

    private List<Term> getMinimalSolution(List<List<Term>> solutions) {
        solutions.sort(Comparator.comparingInt(List::size));
        return solutions.get(0);
    }

    private List<Term> getCheapestSolution(List<List<Term>> solutions, Map<Label, Integer> costs, List<Term> flatendNodes) {
        int minCost = Integer.MAX_VALUE;
        List<Term> cheapestSolution = null;
        for (var solution : solutions) {
            int cost = 0;
            for (var term : solution) {
                if (flatendNodes.contains(term))
                    continue;
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
                flatendNodes.add(new Term(node.id(), new NodeLabel(new Label(property.type(), property.value()))));
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
            } else if (action.compositeLabel()
                    .category()
                    .equals(LabelCategory.Node)) {
                for (var node : dfd.dataFlowDiagram()
                        .getNodes()) {
                    if (node.getId()
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
}
