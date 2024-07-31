package dev.abunai.confidentiality.mitigation.tests;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

public class Sat {
    BiMap<List<String>, Integer> nodeCharToLiteral = new BiMap<>();

    @Test
    public void test() throws ContradictionException, TimeoutException {
        ISolver solver = SolverFactory.newDefault();
        for (var node : List.of("User", "Process", "DB")) {
            var literal = solver.nextFreeVarId(true);
            nodeCharToLiteral.put(List.of(node, "Location", "NonEU"), literal);
            solver.registerLiteral(literal);
        }

        solver.addClause(clause(nodeCharToLiteral.getValue(List.of("User", "Location", "NonEU")),
                nodeCharToLiteral.getValue(List.of("DB", "Location", "NonEU"))));

        IProblem problem = solver;
        while (problem.isSatisfiable()) {
            int[] model = problem.model();
            var negated = new VecInt();
            System.out.println(Arrays.toString(model));
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
