package dev.abunai.confidentiality.mitigation.tests.sat;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import com.google.common.collect.ImmutableMap;

import dev.abunai.confidentiality.mitigation.sat.AbstractChar;
import dev.abunai.confidentiality.mitigation.sat.Constraint;
import dev.abunai.confidentiality.mitigation.sat.Edge;
import dev.abunai.confidentiality.mitigation.sat.NodeChar;
import dev.abunai.confidentiality.mitigation.sat.Sat;
import dev.abunai.confidentiality.mitigation.sat.Label;
import dev.abunai.confidentiality.mitigation.sat.Node;
import dev.abunai.confidentiality.mitigation.sat.OutPin;
import dev.abunai.confidentiality.mitigation.sat.InPin;

import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.dfd.datadictionary.Assignment;

public class SatTest {

    public final String MIN_SAT = "models/minsat.json";

    @Test
    public void manuelTest() throws ContradictionException, TimeoutException {
        var personal = new Label("Sensitivity", "Personal");
        var nonEu = new Label("Location", "NonEu");
        var encrypted = new Label("Encryption", "Encrypted");

        // (personal AND nonEU) => encrypted
        var constraints = List.of(new Constraint(false, "Data", personal), new Constraint(false, "Node", nonEu),
                new Constraint(true, "Data", encrypted));

        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node("User", List.of(), ImmutableMap.<OutPin, List<Label>>builder()
                .put(new OutPin("1"), List.of(personal))
                .build(), List.of()));
        nodes.add(new Node("Process", List.of(new InPin("2")), ImmutableMap.<OutPin, List<Label>>builder()
                .put(new OutPin("3"), List.of(personal))
                .build(), List.of()));
        nodes.add(new Node("DB", List.of(new InPin("4")), ImmutableMap.<OutPin, List<Label>>builder()
                .build(), List.of(nonEu)));
                
        var edges = List.of(new Edge(new OutPin("1"), new InPin("2")), new Edge(new OutPin("3"), new InPin("4")));

        var solutions = new Sat().solve(nodes, edges, constraints);

        Collections.sort(solutions, (list1, list2) -> Integer.compare(list1.size(), list2.size()));
        var min = solutions.get(0)
                .size();
        var minSol = solutions.stream()
                .filter(delta -> delta.size() == min)
                .toList();
        System.out.println(solutions.size());
        System.out.println(minSol);
    }

    @Test
    public void automaticTest() throws ContradictionException, TimeoutException{
        var converter = new DataFlowDiagramConverter();
        var dfd = converter.webToDfd(MIN_SAT);
        converter.storeDFD(dfd, MIN_SAT);

        List<Node> nodes = new ArrayList<>();
        for (var node : dfd.dataFlowDiagram()
                .getNodes()) {
            
            List<InPin> inPins = new ArrayList<>();
            for(var inPin : node.getBehaviour().getInPin()) {
                inPins.add(new InPin(inPin.getId()));
            }
            
            Map<OutPin,List<Label>> outPins = new HashMap<>();
            for(var assignment : node.getBehaviour().getAssignment()) {
                var outPin = assignment.getOutputPin();
                List<Label> outLabels = new ArrayList<>();
                if(assignment instanceof Assignment cast) {
                    for(var label : cast.getOutputLabels()) {
                        var type = ((LabelType) label.eContainer()).getEntityName();
                        var value = label.getEntityName();
                        outLabels.add(new Label(type,value));
                    }
                } 
                outPins.put(new OutPin(outPin.getId()), outLabels);
            }
            
            List<Label> nodeChars = new ArrayList<>();
            for (var property : node.getProperties()) {
                var type = ((LabelType) property.eContainer()).getEntityName();
                var value = property.getEntityName();
                nodeChars.add(new Label(type, value));
            }
            
            nodes.add(new Node(node.getEntityName(),inPins,outPins,nodeChars));
        }
        System.out.println(nodes);

        List<Edge> edges = new ArrayList<>();
        for (var flow : dfd.dataFlowDiagram()
                .getFlows()) {
            edges.add(new Edge(new OutPin(flow.getSourcePin().getId()),new InPin(flow.getDestinationPin().getId())));
        }
        
        System.out.println(edges);
        
        var personal = new Label("Sensitivity", "Personal");
        var nonEu = new Label("Location", "nonEU");
        var encrypted = new Label("Encryption", "Encrypted");

        // (personal AND nonEU) => encrypted
        var constraints = List.of(new Constraint(false, "Data", personal), new Constraint(false, "Node", nonEu),
                new Constraint(true, "Data", encrypted));
        
        var solutions = new Sat().solve(nodes, edges, constraints);

        Collections.sort(solutions, (list1, list2) -> Integer.compare(list1.size(), list2.size()));
        var min = solutions.get(0)
                .size();
        var minSol = solutions.stream()
                .filter(delta -> delta.size() == min)
                .toList();
        System.out.println(solutions.size());
        System.out.println(minSol);
    }
}
