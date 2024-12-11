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
    private BiMap<Edge, Integer> edgeToLit;
    private BiMap<EdgeDataCharacteristic, Integer> edgeDataToLit;
    private ISolver solver;
    private Set<Label> labels;
    private List<Node> nodes;
    private List<Edge> edges;
    private List<Constraint> constraints;
    private List<VecInt> dimacsClauses;
    private int maxLiteral;

    public List<List<Term>> solve(List<Node> nodes, List<Edge> edges, List<Constraint> constraints)
            throws ContradictionException, TimeoutException, IOException {
        this.nodes = nodes;
        this.edges = edges;
        this.constraints = constraints;

        termToLiteral = new BiMap<>();
        edgeToLit = new BiMap<>();
        edgeDataToLit = new BiMap<>();
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
                            clause.push(sign * term(node.name(), new NodeChar(type, value)));
                        } else if (literal.category()
                                .equals(LabelCategory.Data)) {
                            clause.push(sign * term(inPin.id(), new IncomingDataCharacteristics(type, value)));
                        }
                    }
                    addClause(clause);
                }

            }
        }

        // Require node and outgoing data chars
        for (Node node : nodes) {
            for (Label characteristic : node.nodeChars()) {
                addClause(clause(term(node.name(), new NodeChar(characteristic.type(), characteristic.value()))));
            }
            for (OutPin outPin : node.outPins()
                    .keySet()) {
                for (Label outgoingCharacteristic : node.outPins()
                        .get(outPin)) {
                    addClause(clause(term(outPin.id(), new OutgoingDataCharacteristic(outgoingCharacteristic.type(), outgoingCharacteristic.value()))));
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
                        var sign = edges.contains(new Edge(sourcePin, sinkPin)) ? 1 : -1;
                        addClause(clause(sign * edge(sourcePin, sinkPin)));
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
                            var edgeDataLit = edgeData(new Edge(sourcePin, sinkPin), new IncomingDataCharacteristics(label.type(), label.value()));
                            var outLit = term(sourcePin.id(), new OutgoingDataCharacteristic(label.type(), label.value()));
                            // (From.OutIn AND Edge(From,To)) <=> To.EdgeInPin
                            addClause(clause(-outLit, -edge(sourcePin, sinkPin), edgeDataLit));
                            addClause(clause(-edgeDataLit, outLit));
                            addClause(clause(-edgeDataLit, edge(sourcePin, sinkPin)));
                        }
                    }
                }
            }
        }

        // Node has incoming data at inpin iff it receives it at least once
        for (Label label : labels) {
            for (Node sinkNode : nodes) {
                for (InPin sinkPin : sinkNode.inPins()
                        .keySet()) {
                    int incomingDataTerm = term(sinkPin.id(), new IncomingDataCharacteristics(label.type(), label.value()));
                    var clause = new VecInt();
                    clause.push(-incomingDataTerm);
                    for (Node sourceNode : nodes) {
                        for (OutPin sourcePin : sourceNode.outPins()
                                .keySet()) {
                            var edgeDataLit = edgeData(new Edge(sourcePin, sinkPin), new IncomingDataCharacteristics(label.type(), label.value()));
                            addClause(clause(-edgeDataLit, incomingDataTerm));
                            clause.push(edgeDataLit);
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

    private int edge(OutPin from, InPin to) {
        var edge = new Edge(from, to);
        if (!edgeToLit.containsKey(edge)) {
            edgeToLit.put(edge, solver.nextFreeVarId(true));
        }
        return edgeToLit.getValue(edge);
    }

    private int edgeData(Edge edge, IncomingDataCharacteristics inDataChar) {
        var edgeData = new EdgeDataCharacteristic(edge, inDataChar);
        if (!edgeDataToLit.containsKey(edgeData)) {
            edgeDataToLit.put(edgeData, solver.nextFreeVarId(true));
        }
        return edgeDataToLit.getValue(edgeData);
    }

    private int term(String domain, AbstractCharacteristic characteristic) {
        var term = new Term(domain, characteristic);
        if (!termToLiteral.containsKey(term)) {
            termToLiteral.put(term, solver.nextFreeVarId(true));
        }
        return termToLiteral.getValue(term);
    }

    private void writeDimacsFile(String filePath, List<VecInt> clauses) throws IOException {

        maxLiteral = 0;
        for (var literals : clauses) {
            for (var lit : literals.toArray()) {
                int var = Math.abs(lit);
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
            } else if (edgeToLit.containsValue(literal)) {
                literalMap.put(literal, edgeToLit.getKey(literal)
                        .toString());
            } else if (edgeDataToLit.containsValue(literal)) {
                literalMap.put(literal, edgeDataToLit.getKey(literal)
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