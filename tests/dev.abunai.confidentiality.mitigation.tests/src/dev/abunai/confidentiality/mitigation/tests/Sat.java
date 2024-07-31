package dev.abunai.confidentiality.mitigation.tests;

import java.util.List;
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
        BiMap<NodeXChar, Integer> literalMap = new BiMap<>();
        
        var personal = new Characteristic("Data", "Sensitivity", "Personal");
        var nonEu = new Characteristic("Node", "Location", "NonEu");
        var encrypted = new Characteristic("Data", "Encryption", "Encrypted");
        
        // (personal AND nonEU) => encrypted
        var constraints = List.of(new Constraint(false, personal), new Constraint(false,nonEu),
                new Constraint(true, encrypted));
                
        Map<String, List<Characteristic>> nodes = ImmutableMap.<String, List<Characteristic>>builder()
                .put("User", List.of(personal))
                .put("Process", List.of(personal))
                .put("DB", List.of(personal,nonEu))
                .build();

        ISolver solver = SolverFactory.newDefault();
        for (var node : nodes.keySet()) {
            var clause = new VecInt();
            for(var constraint : constraints) {
                var literal = solver.nextFreeVarId(true);
                literalMap.put(new NodeXChar(node, constraint.characteristic()), literal);
                clause.push((constraint.positive() ? 1 : -1) * literal);
            }
            solver.addClause(clause);
            for(var characteristic : nodes.get(node)) {
                solver.addClause(clause(literalMap.getValue(new NodeXChar(node, characteristic))));
            }           
        }
        
        IProblem problem = solver;
        while (problem.isSatisfiable()) {
            int[] model = problem.model();
            var negated = new VecInt();
            var names = IntStream.of(model)
                    .filter(lit -> lit > 0)
                    .mapToObj(lit -> literalMap.getKey(lit))
                    .filter(nxc -> !nodes.get(nxc.node()).contains(nxc.characteristic()))
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
