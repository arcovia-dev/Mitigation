package dev.abunai.confidentiality.mitigation.sat;

import java.util.List;
import java.io.IOException;
import java.io.BufferedWriter;
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

public class Sat {

    private BiMap<Delta, Integer> deltaToLit;
    private BiMap<Edge, Integer> edgeToLit;
    private BiMap<EdgeDataChar, Integer> edgeDataToLit;
    private ISolver solver;
    private Set<Label> labels;
    private List<Node> nodes;
    private List<Edge> edges;
    private List<List<Constraint>> constraints;
    private List<VecInt> dimacsClauses;

    public List<List<Delta>> solve(List<Node> nodes, List<Edge> edges, List<List<Constraint>> constraints)
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

        return solveClauses();
    }

    private List<List<Delta>> solveClauses() throws TimeoutException, ContradictionException {
        IProblem problem = solver;

        List<List<Delta>> solutions = new ArrayList<>();

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
            for (var inPin : node.inPins()) {
                var clause = new VecInt();
                for (var constraint : constraints) {
                    for (var variable : constraint) {
                        var type = variable.label()
                                .type();
                        var value = variable.label()
                                .value();
                        var sign = variable.positive() ? 1 : -1;
                        if (variable.what()
                                .equals("Node")) {
                            clause.push(sign * delta(node.name(), new NodeChar(type, value)));
                        } else if (variable.what()
                                .equals("Data")) {
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
                    for (var toPin : toNode.inPins()) {
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
                    for (var toPin : toNode.inPins()) {
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
                for (var toPin : toNode.inPins()) {
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
            for (var variable : constraint) {
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
        var delta = new Delta(where, characteristic);
        if (!deltaToLit.containsKey(delta)) {
            deltaToLit.put(delta, solver.nextFreeVarId(true));
        }
        return deltaToLit.getValue(delta);
    }

    public void writeDimacsFile(String filePath, List<VecInt> clauses) throws IOException {

        int maxVar = 0;
        for (var literals : clauses) {
            for (var lit : literals.toArray()) {
                int var = Math.abs(lit);
                if (var > maxVar) {
                    maxVar = var;
                }
            }
        }

        int numClauses = clauses.size();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("p cnf " + maxVar + " " + numClauses);
            writer.newLine();

            for (var literals : clauses) {
                writer.write(formatClauseLine(literals));
                writer.newLine();
            }
        }
    }

    public static String formatClauseLine(VecInt literals) {
        StringJoiner joiner = new StringJoiner(" ");

        for (int i = 0; i < literals.size(); i++) {
            int literal = literals.get(i);
            if (literal != 0) {
                joiner.add(Integer.toString(literal));
            }
        }

        return joiner.toString() + " 0";
    }
}