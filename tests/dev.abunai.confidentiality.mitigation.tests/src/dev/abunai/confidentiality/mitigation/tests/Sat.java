package dev.abunai.confidentiality.mitigation.tests;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

public class Sat {
    
    @Test
    public void test() throws ContradictionException, TimeoutException {
        ISolver solver = SolverFactory.newDefault();

        solver.addClause(clause(1,2));
        solver.addClause(clause(-1,3));
        solver.addClause(clause(-2,-3));

        IProblem problem = solver;
        while (problem.isSatisfiable()) {
            int[] model = problem.model();
            var negated = new VecInt();
            System.out.println(Arrays.toString(model));
            for (var literal:model) {
                negated.push(-literal);
            }
            solver.addClause(negated);
        }
        System.out.println(solver.nextFreeVarId(false));
    }

    private VecInt clause(int... literals ) {
        return new VecInt(literals);
    }
}

