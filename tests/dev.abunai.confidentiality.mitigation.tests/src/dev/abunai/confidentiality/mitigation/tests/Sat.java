package dev.abunai.confidentiality.mitigation.tests;

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
        if (problem.isSatisfiable()) {
            System.out.println("Satisfiable: Yes");
            int[] model = problem.model();
            for (int i = 0; i < model.length; i++) {
                System.out.println("x" + (i + 1) + " = " + (model[i] > 0));
            }
        } else {
            System.out.println("Satisfiable: No");
        }
        System.out.println(solver.nextFreeVarId(false));
         
    }

    private VecInt clause(int... literals ) {
        return new VecInt(literals);
    }
}

