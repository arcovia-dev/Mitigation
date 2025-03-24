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

    public List<List<Term>> solve(List<Node> nodes, List<Flow> flows, List<Constraint> constraints, String dfdName)
            throws ContradictionException, TimeoutException, IOException {
        this.nodes = nodes;
        this.flows = flows;
        this.constraints = constraints;

        termToLiteral = new BiMap<>();
        flowToLiteral = new BiMap<>();
        flowDataToLit = new BiMap<>();
        solver = SolverFactory.newDefault();
        dimacsClauses = new ArrayList<>();

        extractUniqueLabels();

        buildClauses();

        if (dfdName != null) {
            writeDimacsFile(("testresults/" + dfdName + ".cnf"), dimacsClauses);

            writeLiteralMapping("testresults/" + dfdName + "-literalMapping.json");
        }

        return solveClauses();
    }

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
            if (!solutions.contains(deltaTerms)) {
                solutions.add(deltaTerms);
            }

            // Prohibit current solution
            var negated = new VecInt();
            for (var literal : deltaTerms) {
                negated.push(-termToLiteral.getValue(literal));
            }
            if (!negated.isEmpty()) {
                addClause(negated);
            }

            if (solutions.size() > 10000) {
                throw new TimeoutException("Solving needed to be terminated after finding 10.000 solutions");
            }
        }
        return solutions;
    }

    private void buildClauses() throws ContradictionException {
        // Apply constraints
        for (Node node : nodes) {
            for (Constraint constraint : constraints) {
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
                } else {
                    for (InPin inPin : node.inPins()
                            .keySet()) {
                        var incomingFlows = flows.stream()
                                .filter(flow -> flow.sink()
                                        .equals(inPin)).toList();
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

        // Require node and outgoing data chars
        for (Node node : nodes) {
            for (Label characteristic : node.nodeChars()) {
                addClause(clause(term(node.id(), new NodeLabel(new Label(characteristic.type(), characteristic.value())))));
            }
            for (OutPin outPin : node.outPins()
                    .keySet()) {
                for (Label outgoingCharacteristic : node.outPins()
                        .get(outPin)) {
                    addClause(clause(term(outPin.id(),
                            new OutgoingDataLabel(new Label(outgoingCharacteristic.type(), outgoingCharacteristic.value())))));
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
        // Node has only incoming data labels that are received via all incoming flows
        // --> (Not Node x has Label L or (Flow A with Label L and Flow B with Label L and ... Flow Z))
        for (Label label : extractPositiveUniqueLabels()) {
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
        for (Label label : labels) {
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
    private List<OutPin> determineRequiredPins(InPin sinkPin, Label label){
        var relevantFlows = flows.stream()
                .filter(flow -> flow.sink()
                        .equals(sinkPin))
                .toList();
        var pins = new ArrayList<OutPin>();
        for (Flow flow : relevantFlows) {
            var sourcePin = flow.source();
            Node sourceNode = nodes.stream()
                    .filter(node -> node.outPins().containsKey(sourcePin))
                    .findFirst()
                    .orElse(null);
            if(violatesConstraintWithLabel(label,sourceNode, sourcePin)) pins.add(sourcePin);

        }

        return pins;
    }
    private Boolean violatesConstraintWithLabel(Label enforcedLabel, Node sourceNode, OutPin sourcePin){
        List<Label> outgoingData = sourceNode.outPins().get(sourcePin);

        for (var constraint : constraints) {
            List<Label> negativeLiterals = new ArrayList<>();
            List<Label> positiveLiterals = new ArrayList<>();
            for (var literal : constraint.literals()) {
                if (literal.positive())
                    positiveLiterals.add(literal.compositeLabel().label());
                else
                    negativeLiterals.add(literal.compositeLabel()
                            .label());
            }
            if (positiveLiterals.contains(enforcedLabel)) {
                if (outgoingData.containsAll(negativeLiterals)) return true;
            }
        }

        return false;
    }

    private List<Label> extractPositiveUniqueLabels() {
        var labelPos = new HashSet<Label>();
        for (Constraint constraint : constraints) {
            for (Literal literal : constraint.literals()) {
                if (literal.positive()){
                    labelPos.add(literal.compositeLabel()
                            .label());
                }
            }
        }
        return List.copyOf(labelPos);
    }

    private void extractUniqueLabels() {
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