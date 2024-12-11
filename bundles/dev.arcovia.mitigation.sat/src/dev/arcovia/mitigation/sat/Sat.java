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

    private BiMap<Term, Integer> deltaToLit;
    private BiMap<Edge, Integer> edgeToLit;
    private BiMap<EdgeDataChar, Integer> edgeDataToLit;
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

        deltaToLit = new BiMap<>();
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
            var deltas = IntStream.of(model)
                    .filter(lit -> lit > 0)
                    .filter(lit -> deltaToLit.containsValue(lit))
                    .mapToObj(lit -> deltaToLit.getKey(lit))
                    .toList();

            // Store unique solutions
            if (!solutions.contains(deltas)) {
                solutions.add(deltas);
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
        for (var node : nodes) {

            for (var inPin : node.inPins()
                    .keySet()) {
                for (Constraint constraint : constraints) {
                    var clause = new VecInt();
                    for (var variable : constraint.literals()) {
                        var type = variable.label()
                                .type();
                        var value = variable.label()
                                .value();
                        var sign = variable.positive() ? 1 : -1;
                        if (variable.category()
                                .equals(LabelCategory.Node)) {
                            clause.push(sign * delta(node.name(), new NodeChar(type, value)));
                        } else if (variable.category()
                                .equals(LabelCategory.Data)) {
                            clause.push(sign * delta(inPin.id(), new InDataChar(type, value)));
                        }
                    }
                    addClause(clause);
                }

            }
        }

        // Require node and outgoing data chars
        for (var node : nodes) {
            for (var property : node.nodeChars()) {
                addClause(clause(delta(node.name(), new NodeChar(property.type(), property.value()))));
            }
            for (var outPin : node.outPins()
                    .keySet()) {
                for (var outData : node.outPins()
                        .get(outPin)) {
                    addClause(clause(delta(outPin.id(), new OutDataChar(outData.type(), outData.value()))));
                }
            }
        }

        // Prohibit creation of new edges
        for (var fromNode : nodes) {
            for (var fromPin : fromNode.outPins()
                    .keySet()) {
                for (var toNode : nodes) {
                    for (var toPin : toNode.inPins()
                            .keySet()) {
                        var sign = edges.contains(new Edge(fromPin, toPin)) ? 1 : -1;
                        addClause(clause(sign * edge(fromPin, toPin)));
                    }
                }
            }
        }

        // Make clauses for label propagation
        for (var fromNode : nodes) {
            for (var fromPin : fromNode.outPins()
                    .keySet()) {
                for (var toNode : nodes) {
                    for (var toPin : toNode.inPins()
                            .keySet()) {
                        for (var label : labels) {
                            var edgeDataLit = edgeData(new Edge(fromPin, toPin), new InDataChar(label.type(), label.value()));
                            var outLit = delta(fromPin.id(), new OutDataChar(label.type(), label.value()));
                            // (From.OutIn AND Edge(From,To)) <=> To.EdgeInPin
                            addClause(clause(-outLit, -edge(fromPin, toPin), edgeDataLit));
                            addClause(clause(-edgeDataLit, outLit));
                            addClause(clause(-edgeDataLit, edge(fromPin, toPin)));
                        }
                    }
                }
            }
        }

        // Node has incoming data at inpin iff it receives it at least once
        for (var label : labels) {
            for (var toNode : nodes) {
                for (var toPin : toNode.inPins()
                        .keySet()) {
                    var inLit = delta(toPin.id(), new InDataChar(label.type(), label.value()));
                    var clause = new VecInt();
                    clause.push(-inLit);
                    for (var fromNode : nodes) {
                        for (var fromPin : fromNode.outPins()
                                .keySet()) {
                            var edgeDataLit = edgeData(new Edge(fromPin, toPin), new InDataChar(label.type(), label.value()));
                            addClause(clause(-edgeDataLit, inLit));
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
        for (var constraint : constraints) {
            for (var variable : constraint.literals()) {
                labels.add(variable.label());
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

    private int edgeData(Edge edge, InDataChar inDataChar) {
        var edgeData = new EdgeDataChar(edge, inDataChar);
        if (!edgeDataToLit.containsKey(edgeData)) {
            edgeDataToLit.put(edgeData, solver.nextFreeVarId(true));
        }
        return edgeDataToLit.getValue(edgeData);
    }

    private int delta(String where, AbstractChar characteristic) {
        var delta = new Term(where, characteristic);
        if (!deltaToLit.containsKey(delta)) {
            deltaToLit.put(delta, solver.nextFreeVarId(true));
        }
        return deltaToLit.getValue(delta);
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
            if (deltaToLit.containsValue(literal)) {
                literalMap.put(literal, deltaToLit.getKey(literal)
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