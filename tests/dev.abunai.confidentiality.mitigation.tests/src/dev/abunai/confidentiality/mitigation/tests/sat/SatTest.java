package dev.abunai.confidentiality.mitigation.tests.sat;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

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
import dev.abunai.confidentiality.mitigation.sat.OutDataChar;
import dev.abunai.confidentiality.mitigation.sat.Sat;
import dev.abunai.confidentiality.mitigation.sat.Label;
import dev.abunai.confidentiality.mitigation.sat.Node;
import dev.abunai.confidentiality.mitigation.sat.OutPin;
import dev.abunai.confidentiality.mitigation.sat.InPin;

import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.dataflowanalysis.dfd.datadictionary.LabelType;

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
    public void automaticTest() {
        var converter = new DataFlowDiagramConverter();
        var dfd = converter.webToDfd(MIN_SAT);
        converter.storeDFD(dfd, MIN_SAT);

        Map<String, List<AbstractChar>> nodes = new HashMap<>();
        for (var node : dfd.dataFlowDiagram()
                .getNodes()) {
            List<AbstractChar> chars = new ArrayList<>();
            for (var property : node.getProperties()) {
                var type = ((LabelType) property.eContainer()).getEntityName();
                var value = property.getEntityName();
                chars.add(new NodeChar(type, value));
            }
            nodes.put(node.getEntityName(), chars);
        }
        System.out.println(nodes);

        for (var flow : dfd.dataFlowDiagram()
                .getFlows()) {
            System.out.println(flow.getSourceNode()
                    .getEntityName() + "-"
                    + flow.getDestinationNode()
                            .getEntityName());
        }
    }
}
