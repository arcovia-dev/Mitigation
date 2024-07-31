package dev.abunai.confidentiality.mitigation.tests;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

public class Sat {
    BiMap<List<String>, Integer> literalMap;

    @Test
    public void test() throws ContradictionException, TimeoutException {
        literalMap = new BiMap<>();
        // if data is personal and node in non eu then data must be encrypted
        var constraints = List.of(new Constraint(false, "Data", "Sensitivity", "Personal"), new Constraint(false, "Node", "Location", "NonEu"),
                new Constraint(true, "Data", "Encryption", "Encrypted"));
        
        var nodes = List.of("User", "Process", "DB");

        ISolver solver = SolverFactory.newDefault();
        for (var node : nodes) {
            var clause = new VecInt();
            for(var constraint : constraints) {
                var literal = solver.nextFreeVarId(true);
                literalMap.put(List.of(node, constraint.what(), constraint.type(), constraint.value()), literal);
                clause.push((constraint.positive() ? 1 : -1) * literal);
            }
            solver.addClause(clause);
            //solver.addClause(clause(literalMap.getValue(List.of(node, "Node", "Location", "NonEu"))));
            //solver.addClause(clause(literalMap.getValue(List.of(node, "Data", "Sensitivity", "Personal"))));
        }

        

        IProblem problem = solver;
        while (problem.isSatisfiable()) {
            int[] model = problem.model();
            var negated = new VecInt();
            var names = IntStream.of(model)
                    .filter(lit -> lit > 0)
                    .mapToObj(lit -> literalMap.getKey(lit))
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
