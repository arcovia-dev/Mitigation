package dev.abunai.confidentiality.mitigation.tests;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
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
        ISolver solver = SolverFactory.newDefault();

        var personal = new InDataChar("Sensitivity", "Personal");
        var nonEu = new NodeChar("Location", "NonEu");
        var encrypted = new InDataChar("Encryption", "Encrypted");
        
        // (personal AND nonEU) => encrypted
        var constraints = List.of(new Constraint(false, personal), new Constraint(false, nonEu), new Constraint(true, encrypted));

        Map<String, List<AbstractChar>> nodes = ImmutableMap.<String, List<AbstractChar>>builder()
                .put("User", List.of(personal))
                .put("Process", List.of(personal))
                .put("DB", List.of(personal, nonEu))
                .build();

        for (var node : nodes.keySet()) {
            var clause = new VecInt();
            for (var constraint : constraints) {
                var literal = solver.nextFreeVarId(true);
                deltaToLit.put(new Delta(node, constraint.characteristic()), literal);
                clause.push((constraint.positive() ? 1 : -1) * literal);
            }
            solver.addClause(clause);
            for (var characteristic : nodes.get(node)) {
                solver.addClause(clause(deltaToLit.getValue(new Delta(node, characteristic))));
            }
        }

        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("User", "Process"));
        edges.add(new Edge("Process", "DB"));

        for (var from : nodes.keySet()) {
            for (var to : nodes.keySet()) {
                var literal = solver.nextFreeVarId(true);
                var edge = new Edge(from, to);
                edgeToLit.put(edge, literal);
                //Prohibit new edges
                var sign = edges.contains(edge) ? 1 : -1;
                solver.addClause(clause(sign * literal));
            }
        }

        IProblem problem = solver;
        while (problem.isSatisfiable()) {
            int[] model = problem.model();
            var negated = new VecInt();
            var names = IntStream.of(model)
                    .filter(lit -> lit > 0)
                    .filter(lit -> deltaToLit.containsValue(lit))
                    .mapToObj(lit -> deltaToLit.getKey(lit))
                    .filter(delta -> !nodes.get(delta.node())
                            .contains(delta.characteristic()))
                    .toList();
            System.out.println(names);
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
