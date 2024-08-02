package dev.abunai.confidentiality.mitigation.tests;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import com.google.common.collect.ImmutableMap;

public class SatTest {
    
    @Test
    public void test() throws ContradictionException, TimeoutException {
        var personal = new InDataChar("Sensitivity", "Personal");
        var nonEu = new NodeChar("Location", "NonEu");
        var encrypted = new InDataChar("Encryption", "Encrypted");

        // (personal AND nonEU) => encrypted
        var constraints = List.of(new Constraint(false, personal), new Constraint(false, nonEu), new Constraint(true, encrypted));

        var nodes = ImmutableMap.<String, List<AbstractChar>>builder()
                .put("User", List.of(personal.toOut()))
                .put("Process", List.of(personal.toOut()))
                .put("DB", List.of(nonEu))
                .build();

        var edges = List.of(new Edge("User", "Process"), new Edge("Process", "DB"));

        var solutions = new Sat().solve(nodes,edges,constraints);
        
        Collections.sort(solutions, (list1, list2) -> Integer.compare(list1.size(), list2.size()));
        var min = solutions.get(0).size();
        var minSol= solutions.stream().filter(delta -> delta.size()==min).toList();
        System.out.println(minSol);
    }
}
