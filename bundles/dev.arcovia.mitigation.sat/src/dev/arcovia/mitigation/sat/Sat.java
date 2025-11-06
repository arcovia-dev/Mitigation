package dev.arcovia.mitigation.sat;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.IntStream;

import java.util.StringJoiner;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * The Sat class provides a framework for solving satisfiability problems. It models and solves a set of constraints
 * related to nodes, flows, and terms in a data flow diagram (DFD).
 */
public class Sat {

    private BiMap<Term, Integer> termToLiteral;
    private BiMap<Flow, Integer> flowToLiteral;
    private BiMap<FlowDataLabel, Integer> flowDataToLit;
    private ISolver solver;
    private Set<Label> labels;
    private List<Node> nodes;
    private List<Flow> flows;
    private List<Constraint> constraints;
    private List<VecInt> dimacsClauses;
    private int maxLiteral;
    private boolean deactivateSubsumption;
    private Set<Label> allLabels = null;

    /**
     * Solves a constraint satisfaction problem based on the given nodes, flows, and constraints. The method builds the
     * necessary clauses, writes related output files for debugging or analysis if a DFD name is provided, and then solves
     * the problem using a SAT solver.
     * @param nodes the list of nodes involved in the system.
     * @param flows the list of flows between nodes in the system.
     * @param constraints the list of constraints that must be satisfied.
     * @param dfdName the optional name for the data flow diagram; used for output file naming.
     * @return a list of solutions, where each solution is represented as a list of terms.
     * @throws ContradictionException if the constraints contain a direct contradiction.
     * @throws TimeoutException if the solver exceeds the allocated time without finding a solution.
     * @throws IOException if an error occurs during file writing operations.
     */
    public List<List<Term>> solve(List<Node> nodes, List<Flow> flows, List<Constraint> constraints, String dfdName, boolean deactivateSubsumption,
            Set<Label> allLabel) throws ContradictionException, TimeoutException, IOException {
        this.nodes = nodes;
        this.flows = flows;
        this.constraints = constraints;
        this.deactivateSubsumption = deactivateSubsumption;
        this.allLabels = allLabel;

        termToLiteral = new BiMap<>();
        flowToLiteral = new BiMap<>();
        flowDataToLit = new BiMap<>();
        solver = SolverFactory.newDefault();
        dimacsClauses = new ArrayList<>();

        extractConstraintLabels();

        buildClauses();

        if (dfdName != null) {
            writeDimacsFile(("testresults/" + dfdName + ".cnf"), dimacsClauses);

            writeLiteralMapping("testresults/" + dfdName + "-literalMapping.json");
        }

        return solveClauses();
    }

    /**
     * Solves a system of logical clauses using a SAT solver and returns a list of unique solutions. The method iteratively
     * finds solutions to the given SAT problem, extracts relevant terms from each solution while excluding terms associated
     * with incoming data, and ensures each solution is unique. It also prohibits previously found solutions to ensure the
     * discovery of distinct results. The solver halts if more than 10,000 solutions are found, throwing a
     * {@link TimeoutException}. Additional constraints are dynamically added to avoid revisiting the same solutions.
     * @return a list of unique solutions, where each solution is represented as a list of {@link Term} objects.
     * @throws TimeoutException if the solver exceeds the allocated time or reaches the maximum number of allowed solutions.
     * @throws ContradictionException if a contradiction is encountered in the constraints.
     */
    private List<List<Term>> solveClauses() throws TimeoutException, ContradictionException {
        IProblem problem = solver;

        List<List<Term>> solutions = new ArrayList<>();

        while (problem.isSatisfiable()) {
            int[] model = problem.model();

            // Map literals to relevant Deltas
            var deltaTerms = IntStream.of(model)
                    .filter(lit -> lit > 0)
                    .filter(lit -> termToLiteral.containsValue(lit))
                    .mapToObj(lit -> termToLiteral.getKey(lit))
                    .filter(lit -> !lit.compositeLabel()
                            .category()
                            .equals(LabelCategory.IncomingData))
                    .toList();

            // Store unique solutions
            if (!solutions.contains(deltaTerms) || deactivateSubsumption) {
                solutions.add(deltaTerms);
            }

            // Prohibit current solution
            var negated = new VecInt();

            for (var literal : deltaTerms) {
                negated.push(-termToLiteral.getValue(literal));
            }

            if (!negated.isEmpty() && !deactivateSubsumption)
                addClause(negated);

            if (solutions.size() > 10000) {
                if (deactivateSubsumption)
                    return solutions;

                throw new TimeoutException("Solving needed to be terminated after finding 10.000 solutions");
            }
        }
        return solutions;
    }

    /**
     * Builds the logical clauses required for solving the constraint satisfaction problem using a SAT solver. This method
     * encodes various constraints and relationships between nodes, flows, and labels into clausal form, ensuring the
     * problem's logical structure can be processed by the solver. The clauses include constraints for enforcing node
     * characteristics, handling data flows, prohibiting invalid edges, and managing label propagation and interactions. The
     * method performs the following tasks: 1. Encodes node-only constraints. 2. Applies constraints to flows entering a
     * node. 3. Ensures that each node and its outgoing data align with predefined characteristics. 4. Prohibits the
     * creation of unauthorized edges between nodes. 5. Encodes label propagation rules, ensuring labels and data attributes
     * are correctly transferred along defined flows. 6. Ensures nodes have repairing incoming data labels via all necessary
     * violating flows. 7. Ensures nodes receive incoming data labels if any relevant flows are active. The clauses are
     * constructed and added to the SAT solver iteratively by processing nodes, pins, flows, and constraints within the
     * system.
     * @throws ContradictionException if the constraints result in any logical contradictions during clause addition,
     * rendering the problem unsolvable.
     */
    private void buildClauses() throws ContradictionException {
        // Force constraints at each node and Flow
        for (Node node : nodes) {
            for (Constraint constraint : constraints) {
                // Apply node only constraints
                if (constraint.literals()
                        .stream()
                        .allMatch(literal -> literal.compositeLabel()
                                .category()
                                .equals(LabelCategory.Node))) {

                    var clause = new VecInt();
                    for (Literal literal : constraint.literals()) {
                        var label = literal.compositeLabel();
                        var sign = literal.positive() ? 1 : -1;
                        clause.push(sign * term(node.id(), label));
                    }
                    addClause(clause);
                }

                else {
                    for (InPin inPin : node.inPins()
                            .keySet()) {

                        var incomingFlows = flows.stream()
                                .filter(flow -> flow.sink()
                                        .equals(inPin))
                                .toList();
                        // Apply all other constraints per IncomingFlow
                        for (Flow flow : incomingFlows) {

                            var clause = new VecInt();
                            for (Literal literal : constraint.literals()) {

                                var label = literal.compositeLabel();
                                var sign = literal.positive() ? 1 : -1;
                                if (literal.compositeLabel()
                                        .category()
                                        .equals(LabelCategory.Node)) {
                                    clause.push(sign * term(node.id(), label));
                                } else if (literal.compositeLabel()
                                        .category()
                                        .equals(LabelCategory.IncomingData)) {
                                    var data = flowData(flow, new IncomingDataLabel(label.label()));
                                    clause.push(sign * data);
                                }
                            }
                            addClause(clause);
                        }

                    }
                }
            }

        }

        // Require node and outgoing data characteristics
        for (Node node : nodes) {
            for (Label characteristic : node.nodeChars()) {
                addClause(clause(term(node.id(), new NodeLabel(new Label(characteristic.type(), characteristic.value())))));
            }
            for (OutPin outPin : node.outPins()
                    .keySet()) {
                for (Label outgoingCharacteristic : node.outPins()
                        .get(outPin)) {
                    addClause(clause(
                            term(outPin.id(), new OutgoingDataLabel(new Label(outgoingCharacteristic.type(), outgoingCharacteristic.value())))));
                }
            }
        }

        // Prohibit creation of new edges
        for (Node sourceNode : nodes) {
            for (OutPin sourcePin : sourceNode.outPins()
                    .keySet()) {
                for (Node sinkNode : nodes) {
                    for (InPin sinkPin : sinkNode.inPins()
                            .keySet()) {
                        var sign = flows.contains(new Flow(sourcePin, sinkPin)) ? 1 : -1;
                        addClause(clause(sign * flow(sourcePin, sinkPin)));
                    }
                }
            }
        }

        // Make clauses for label propagation
        for (Node sourceNode : nodes) {
            for (OutPin sourcePin : sourceNode.outPins()
                    .keySet()) {
                var relevantInPins = flows.stream()
                        .filter(flow -> flow.source()
                                .equals(sourcePin))
                        .map(Flow::sink)
                        .toList();
                for (InPin sinkPin : relevantInPins) {
                    for (Label label : labels) {
                        var incomingFlowData = flowData(new Flow(sourcePin, sinkPin), new IncomingDataLabel(label));
                        var outgoingDataTerm = term(sourcePin.id(), new OutgoingDataLabel(label));

                        // (Source.outData AND Flow(Source,Sink)) <=> Sink.incomingData
                        // --> ((¬Source.outData ∨ ¬Flow(Source,Sink) ∨ Sink.incomingData) ∧ (¬To.incomingData ∨ Source.outData) ∧
                        // (¬Sink.incomingData ∨ Flow(Source,Sink))
                        // <--> (A ∧ B ↔ C --> (¬C ∨ A) ∧ (¬C ∨ B) ∧ (¬A ∨ ¬B ∨ C))
                        addClause(clause(-outgoingDataTerm, -flow(sourcePin, sinkPin), incomingFlowData));
                        addClause(clause(-incomingFlowData, outgoingDataTerm));
                        addClause(clause(-incomingFlowData, flow(sourcePin, sinkPin)));

                    }
                }

            }
        }

        // Node has repairing incoming data labels only if received via all violating incoming flows
        for (Label label : extractRepairingConstrainLabels()) {
            for (Node sinkNode : nodes) {
                for (InPin sinkPin : sinkNode.inPins()
                        .keySet()) {
                    int incomingDataTerm = term(sinkPin.id(), new IncomingDataLabel(label));

                    var relevantOutPins = determineRequiredPins(sinkPin, label);

                    for (OutPin sourcePin : relevantOutPins) {
                        var flowData = flowData(new Flow(sourcePin, sinkPin), new IncomingDataLabel(label));
                        addClause(clause(-flowData, incomingDataTerm));
                        addClause(clause(-incomingDataTerm, flowData));
                    }
                }
            }
        }
        // Node has incoming data if received via at least one flow (Above needs not be excluded since above clauses need to be
        // fulfilled)
        var labelsToUse = allLabels != null ? allLabels : labels;

        for (Label label : labelsToUse) {
            for (Node sinkNode : nodes) {
                for (InPin sinkPin : sinkNode.inPins()
                        .keySet()) {
                    int incomingDataTerm = term(sinkPin.id(), new IncomingDataLabel(label));

                    var relevantOutPins = flows.stream()
                            .filter(flow -> flow.sink()
                                    .equals(sinkPin))
                            .map(Flow::source)
                            .toList();
                    var clause = new VecInt();
                    clause.push(-incomingDataTerm);
                    for (OutPin sourcePin : relevantOutPins) {
                        var flowData = flowData(new Flow(sourcePin, sinkPin), new IncomingDataLabel(label));
                        addClause(clause(-flowData, incomingDataTerm));
                        clause.push(flowData);
                    }
                    addClause(clause);
                }
            }
        }
    }

    private List<OutPin> determineRequiredPins(InPin sinkPin, Label label) {
        var relevantFlows = flows.stream()
                .filter(flow -> flow.sink()
                        .equals(sinkPin))
                .toList();
        var pins = new ArrayList<OutPin>();
        for (Flow flow : relevantFlows) {
            var sourcePin = flow.source();
            Node sourceNode = nodes.stream()
                    .filter(node -> node.outPins()
                            .containsKey(sourcePin))
                    .findFirst()
                    .orElse(null);
            if (violatesConstraintWithLabel(label, sourceNode, sourcePin))
                pins.add(sourcePin);

        }

        return pins;
    }

    private Boolean violatesConstraintWithLabel(Label enforcedLabel, Node sourceNode, OutPin sourcePin) {
        List<Label> outgoingData = sourceNode.outPins()
                .get(sourcePin);

        for (var constraint : constraints) {
            List<Label> negativeLiterals = new ArrayList<>();
            List<Label> positiveLiterals = new ArrayList<>();
            for (var literal : constraint.literals()) {
                if (literal.positive())
                    positiveLiterals.add(literal.compositeLabel()
                            .label());
                else
                    negativeLiterals.add(literal.compositeLabel()
                            .label());
            }
            if (positiveLiterals.contains(enforcedLabel)) {
                if (outgoingData.containsAll(negativeLiterals))
                    return true;
            }
        }

        return false;
    }

    private List<Label> extractRepairingConstrainLabels() {
        Set<Label> positiveLabels = new HashSet<>();
        for (Constraint constraint : constraints) {
            for (Literal literal : constraint.literals()) {
                if (literal.positive()) {
                    positiveLabels.add(literal.compositeLabel()
                            .label());
                }
            }
        }
        return List.copyOf(positiveLabels);
    }

    private void extractConstraintLabels() {
        labels = new HashSet<>();
        for (Constraint constraint : constraints) {
            for (Literal literal : constraint.literals()) {
                labels.add(literal.compositeLabel()
                        .label());
            }
        }
    }

    private VecInt clause(int... literals) {
        return new VecInt(literals);
    }

    private void addClause(VecInt clause) throws ContradictionException {
        solver.addClause(clause);
        dimacsClauses.add(clause);
    }

    private int flow(OutPin source, InPin sink) {
        var edge = new Flow(source, sink);
        if (!flowToLiteral.containsKey(edge)) {
            flowToLiteral.put(edge, solver.nextFreeVarId(true));
        }
        return flowToLiteral.getValue(edge);
    }

    private int flowData(Flow edge, IncomingDataLabel incomingDataLabel) {
        var flowDataLabel = new FlowDataLabel(edge, incomingDataLabel);
        if (!flowDataToLit.containsKey(flowDataLabel)) {
            flowDataToLit.put(flowDataLabel, solver.nextFreeVarId(true));
        }
        return flowDataToLit.getValue(flowDataLabel);
    }

    private int term(String domain, CompositeLabel label) {
        var term = new Term(domain, label);
        if (!termToLiteral.containsKey(term)) {
            termToLiteral.put(term, solver.nextFreeVarId(true));
        }
        return termToLiteral.getValue(term);
    }

    private void writeDimacsFile(String filePath, List<VecInt> clauses) throws IOException {

        maxLiteral = 0;
        for (var terms : clauses) {
            for (var literal : terms.toArray()) {
                int var = Math.abs(literal);
                if (var > maxLiteral) {
                    maxLiteral = var;
                }
            }
        }

        int numClauses = clauses.size();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("p cnf " + maxLiteral + " " + numClauses);
            writer.newLine();

            for (var literals : clauses) {
                writer.write(formatClauseLine(literals));
                writer.newLine();
            }
        }
    }

    private static String formatClauseLine(VecInt literals) {
        StringJoiner joiner = new StringJoiner(" ");

        for (int i = 0; i < literals.size(); i++) {
            int literal = literals.get(i);
            if (literal != 0) {
                joiner.add(Integer.toString(literal));
            }
        }

        return joiner + " 0";
    }

    private void writeLiteralMapping(String outputFile) {
        Map<Integer, String> literalMap = new HashMap<>();

        for (int literal = 1; literal <= maxLiteral; literal++) {
            if (termToLiteral.containsValue(literal)) {
                literalMap.put(literal, termToLiteral.getKey(literal)
                        .toString());
            } else if (flowToLiteral.containsValue(literal)) {
                literalMap.put(literal, flowToLiteral.getKey(literal)
                        .toString());
            } else if (flowDataToLit.containsValue(literal)) {
                literalMap.put(literal, flowDataToLit.getKey(literal)
                        .toString());
            } else {
                System.out.println("Unidentified literal");
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            objectMapper.writeValue(new File(outputFile), literalMap);
        } catch (IOException e) {
            System.out.println("Could not store literal mapping:" + e);
        }
    }
}