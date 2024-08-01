package dev.abunai.confidentiality.mitigation.tests;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import com.google.common.collect.ImmutableMap;

public class Sat {

    @Test
    public void test() throws ContradictionException, TimeoutException {
        BiMap<Delta, Integer> deltaToLit = new BiMap<>();
        BiMap<Edge, Integer> edgeToLit = new BiMap<>();
        BiMap<EdgeDataChar, Integer> edgeDataToLit = new BiMap<>();
        ISolver solver = SolverFactory.newDefault();

        var personal = new InDataChar("Sensitivity", "Personal");
        var nonEu = new NodeChar("Location", "NonEu");
        var encrypted = new InDataChar("Encryption", "Encrypted");

        // (personal AND nonEU) => encrypted
        var constraints = List.of(new Constraint(false, personal), new Constraint(false, nonEu), new Constraint(true, encrypted));

        Map<String, List<AbstractChar>> nodes = ImmutableMap.<String, List<AbstractChar>>builder()
                .put("User", List.of(personal.toOut()))
                .put("Process", List.of(personal.toOut()))
                .put("DB", List.of(nonEu))
                .build();

        var edges = List.of(new Edge("User", "Process"), new Edge("Process", "DB"));

        // Extract unique labels from constraints
        Set<Label> labels = new HashSet<>();
        for (var constraint : constraints) {
            labels.add(new Label(constraint.characteristic()
                    .type(),
                    constraint.characteristic()
                            .value()));
        }

        // Init delta to literal map
        for (var label : labels) {
            var type = label.type();
            var value = label.value();
            for (var node : nodes.keySet()) {
                deltaToLit.put(new Delta(node, new NodeChar(type, value)), solver.nextFreeVarId(true));
                deltaToLit.put(new Delta(node, new InDataChar(type, value)), solver.nextFreeVarId(true));
                deltaToLit.put(new Delta(node, new OutDataChar(type, value)), solver.nextFreeVarId(true));
            }
        }

        // Transform constraints to clauses
        for (var node : nodes.keySet()) {
            var clause = new VecInt();
            for (var constraint : constraints) {
                clause.push((constraint.positive() ? 1 : -1) * deltaToLit.getValue(new Delta(node, constraint.characteristic())));
            }
            solver.addClause(clause);
        }

        // Make clauses that require node and outgoing data chars
        for (var node : nodes.keySet()) {
            for (var characteristic : nodes.get(node)) {
                if (characteristic instanceof InDataChar cast) {
                    solver.addClause(clause(deltaToLit.getValue(new Delta(node, cast.toOut()))));
                } else {
                    solver.addClause(clause(deltaToLit.getValue(new Delta(node, characteristic))));
                }
            }
        }

        // Init edges map and prohibit creation of new edges
        for (var from : nodes.keySet()) {
            for (var to : nodes.keySet()) {
                if (!from.equals(to)) {
                    var literal = solver.nextFreeVarId(true);
                    var edge = new Edge(from, to);
                    edgeToLit.put(edge, literal);
                    // Prohibit new edges
                    var sign = edges.contains(edge) ? 1 : -1;
                    solver.addClause(clause(sign * literal));
                }
            }
        }

        // Make clauses for label propagation
        for (var from : nodes.keySet()) {
            for (var to : nodes.keySet()) {
                if (!from.equals(to)) {
                    var edgeLit = edgeToLit.getValue(new Edge(from, to));
                    for (var label : labels) {
                        var inFromLit = solver.nextFreeVarId(true);
                        edgeDataToLit.put(new EdgeDataChar(new Edge(from, to), new InDataChar(label.type(), label.value())), inFromLit);
                        var outLit = deltaToLit.getValue(new Delta(from, new OutDataChar(label.type(), label.value())));
                        // (From.Outgoing AND Edge(From,To)) <=> To.IngoingFrom
                        // (¬A∨¬B∨C)∧(¬C∨A)∧(¬C∨B)
                        solver.addClause(clause(-outLit, -edgeLit, inFromLit));
                        solver.addClause(clause(-inFromLit, outLit));
                        solver.addClause(clause(-inFromLit, edgeLit));
                    }
                }
            }
        }

        // Node has incoming data iff it receives it at least once
        for (var label : labels) {
            for (var to : nodes.keySet()) {
                var inLit = deltaToLit.getValue(new Delta(to, new InDataChar(label.type(), label.value())));
                var clause = new VecInt();
                clause.push(-inLit);
                for (var from : nodes.keySet()) {
                    if (!from.equals(to)) {
                        var inFromLit = edgeDataToLit.getValue(new EdgeDataChar(new Edge(from, to), new InDataChar(label.type(), label.value())));
                        solver.addClause(clause(-inFromLit, inLit));
                        clause.push(inFromLit);
                    }
                }
                solver.addClause(clause);
            }
        }

        IProblem problem = solver;
        Set<List<Delta>> solutions = new HashSet<>();
        while (problem.isSatisfiable()) {
            int[] model = problem.model();
            var negated = new VecInt();
            var deltas = IntStream.of(model)
                    .filter(lit -> lit > 0)
                    .filter(lit -> deltaToLit.containsValue(lit))
                    .mapToObj(lit -> deltaToLit.getKey(lit))
                    .filter(delta -> !nodes.get(delta.node())
                            .contains(delta.characteristic()))
                    .filter(delta -> !delta.characteristic()
                            .what()
                            .equals("InData"))
                    .toList();
            if (!solutions.contains(deltas)) {
                System.out.println(deltas);
                solutions.add(deltas);
            }
            for (var literal : model) {
                negated.push(-literal);
            }
            solver.addClause(negated);
        }
    }

    private VecInt clause(int... literals) {
        return new VecInt(literals);
    }
}
