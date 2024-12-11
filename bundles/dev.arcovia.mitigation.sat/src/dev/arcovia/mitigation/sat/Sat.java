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
    private BiMap<Flow, Integer> flowToLit;
    private BiMap<FlowDataLabel, Integer> flowDataToLit;
    private ISolver solver;
    private Set<Label> labels;
    private List<Node> nodes;
    private List<Flow> flows;
    private List<Constraint> constraints;
    private List<VecInt> dimacsClauses;
    private int maxLiteral;

    public List<List<Term>> solve(List<Node> nodes, List<Flow> edges, List<Constraint> constraints)
            throws ContradictionException, TimeoutException, IOException {
        this.nodes = nodes;
        this.flows = edges;
        this.constraints = constraints;

        termToLiteral = new BiMap<>();
        flowToLit = new BiMap<>();
        flowDataToLit = new BiMap<>();
        solver = SolverFactory.newDefault();
        dimacsClauses = new ArrayList<>();

        extractUniqueLabels();

        buildClauses();

        writeDimacsFile("dimacs.cnf", dimacsClauses);

        writeLiteralMapping("literalMapping.json");

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
                    .toList();

            // Store unique solutions
            if (!solutions.contains(deltaTerms)) {
                solutions.add(deltaTerms);
            }

            // Prohibit current solution
            var negated = new VecInt();
            for (var literal : model) {
                negated.push(-literal);
            }
            addClause(negated);
        }
        return solutions;
    }

    private void buildClauses() throws ContradictionException {
        // Apply constraints
        for (Node node : nodes) {
            for (InPin inPin : node.inPins()
                    .keySet()) {
                for (Constraint constraint : constraints) {
                    var clause = new VecInt();
                    for (Literal literal : constraint.literals()) {
                        var type = literal.label()
                                .type();
                        var value = literal.label()
                                .value();
                        var sign = literal.positive() ? 1 : -1;
                        if (literal.category()
                                .equals(LabelCategory.Node)) {
                            clause.push(sign * term(node.name(), new NodeLabel(type, value)));
                        } else if (literal.category()
                                .equals(LabelCategory.IncomingData)) {
                            clause.push(sign * term(inPin.id(), new IncomingDataLabel(type, value)));
                        }
                    }
                    addClause(clause);
                }

            }
        }

        // Require node and outgoing data chars
        for (Node node : nodes) {
            for (Label characteristic : node.nodeChars()) {
                addClause(clause(term(node.name(), new NodeLabel(characteristic.type(), characteristic.value()))));
            }
            for (OutPin outPin : node.outPins()
                    .keySet()) {
                for (Label outgoingCharacteristic : node.outPins()
                        .get(outPin)) {
                    addClause(clause(term(outPin.id(), new OutgoingDataLabel(outgoingCharacteristic.type(), outgoingCharacteristic.value()))));
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
                for (Node sinkNode : nodes) {
                    for (InPin sinkPin : sinkNode.inPins()
                            .keySet()) {
                        for (Label label : labels) {
                            var incomingFlowData = flowData(new Flow(sourcePin, sinkPin), new IncomingDataLabel(label.type(), label.value()));
                            var outgoingDataTerm = term(sourcePin.id(), new OutgoingDataLabel(label.type(), label.value()));
                            
                            // (Source.outData AND Flow(Source,Sink)) <=> Sink.incomingData 
                            // --> ((¬Source.outData ∨ ¬Flow(Source,Sink) ∨ Sink.incomingData) ∧ (¬To.incomingData ∨ Source.outData) ∧ (¬Sink.incomingData ∨ Flow(Source,Sink))
                            // <--> (A ∧ B ↔ C --> (¬C ∨ A) ∧ (¬C ∨ B) ∧ (¬A ∨ ¬B ∨ C)) 
                            addClause(clause(-outgoingDataTerm, -flow(sourcePin, sinkPin), incomingFlowData));
                            addClause(clause(-incomingFlowData, outgoingDataTerm));
                            addClause(clause(-incomingFlowData, flow(sourcePin, sinkPin)));
                        }
                    }
                }
            }
        }

        // Node has only incoming data labels that are received via at least one flow --> (Not Node x has Label L or Flow A with Label L or Flow B with Label L or ... Flow Z)
        for (Label label : labels) {
            for (Node sinkNode : nodes) {
                for (InPin sinkPin : sinkNode.inPins()
                        .keySet()) {
                    int incomingDataTerm = term(sinkPin.id(), new IncomingDataLabel(label.type(), label.value()));
                    var clause = new VecInt();
                    clause.push(-incomingDataTerm);
                    for (Node sourceNode : nodes) {
                        for (OutPin sourcePin : sourceNode.outPins()
                                .keySet()) {
                            var flowData = flowData(new Flow(sourcePin, sinkPin), new IncomingDataLabel(label.type(), label.value()));
                            addClause(clause(-flowData, incomingDataTerm));
                            clause.push(flowData);
                        }
                    }
                    addClause(clause);
                }
            }
        }
    }

    private void extractUniqueLabels() {
        labels = new HashSet<>();
        for (Constraint constraint : constraints) {
            for (Literal literal : constraint.literals()) {
                labels.add(literal.label());
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
        if (!flowToLit.containsKey(edge)) {
            flowToLit.put(edge, solver.nextFreeVarId(true));
        }
        return flowToLit.getValue(edge);
    }

    private int flowData(Flow edge, IncomingDataLabel incomingDataLabel) {
        var flowDataLabel = new FlowDataLabel(edge, incomingDataLabel);
        if (!flowDataToLit.containsKey(flowDataLabel)) {
            flowDataToLit.put(flowDataLabel, solver.nextFreeVarId(true));
        }
        return flowDataToLit.getValue(flowDataLabel);
    }

    private int term(String domain, AbstractLabel label) {
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

        return joiner.toString() + " 0";
    }

    private void writeLiteralMapping(String outputFile) {
        Map<Integer, String> literalMap = new HashMap<>();

        for (int literal = 1; literal <= maxLiteral; literal++) {
            if (termToLiteral.containsValue(literal)) {
                literalMap.put(literal, termToLiteral.getKey(literal)
                        .toString());
            } else if (flowToLit.containsValue(literal)) {
                literalMap.put(literal, flowToLit.getKey(literal)
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