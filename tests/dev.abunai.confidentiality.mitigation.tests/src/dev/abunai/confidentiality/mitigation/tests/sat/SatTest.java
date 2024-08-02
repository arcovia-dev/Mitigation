package dev.abunai.confidentiality.mitigation.tests.sat;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import com.google.common.collect.ImmutableMap;

import dev.abunai.confidentiality.mitigation.sat.AbstractChar;
import dev.abunai.confidentiality.mitigation.sat.Constraint;
import dev.abunai.confidentiality.mitigation.sat.Edge;
import dev.abunai.confidentiality.mitigation.sat.InDataChar;
import dev.abunai.confidentiality.mitigation.sat.NodeChar;
import dev.abunai.confidentiality.mitigation.sat.Sat;

import org.dataflowanalysis.converter.DataFlowDiagramConverter;

public class SatTest {
    
    public final String MIN_SAT = "models/minsat.json";
    
    @Test
    public void manuelTest() throws ContradictionException, TimeoutException {
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
    
    @Test
    public void automaticTest() {
        var converter = new DataFlowDiagramConverter();
        var dfd = converter.webToDfd(MIN_SAT);
        converter.storeDFD(dfd, MIN_SAT);
        
        for(var node :dfd.dataFlowDiagram().getNodes()) {
            System.out.println(node.getEntityName());
            for (var property : node.getProperties()) {
                System.out.println(property.getEntityName());
            }
        }
        
        for(var flow : dfd.dataFlowDiagram().getFlows()) {
            System.out.println(flow.getSourceNode().getEntityName()+"-"+flow.getDestinationNode().getEntityName());
        }
    }
}
