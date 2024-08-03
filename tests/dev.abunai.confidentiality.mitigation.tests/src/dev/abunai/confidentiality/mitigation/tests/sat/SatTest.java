package dev.abunai.confidentiality.mitigation.tests.sat;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import dev.abunai.confidentiality.mitigation.sat.Constraint;
import dev.abunai.confidentiality.mitigation.sat.Edge;
import dev.abunai.confidentiality.mitigation.sat.Delta;
import dev.abunai.confidentiality.mitigation.sat.Sat;
import dev.abunai.confidentiality.mitigation.sat.Label;
import dev.abunai.confidentiality.mitigation.sat.Node;
import dev.abunai.confidentiality.mitigation.sat.NodeChar;
import dev.abunai.confidentiality.mitigation.sat.OutDataChar;
import dev.abunai.confidentiality.mitigation.sat.OutPin;
import dev.abunai.confidentiality.mitigation.sat.InPin;

import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.dfd.datadictionary.Assignment;

public class SatTest {

    public final String MIN_SAT = "models/minsat.json";

    @Test
    public void automaticTest() throws ContradictionException, TimeoutException {
        var converter = new DataFlowDiagramConverter();
        var dfd = converter.webToDfd(MIN_SAT);

        // (personal AND nonEU) => encrypted
        var constraints = List.of(new Constraint(false, "Data", new Label("Sensitivity", "Personal")),
                new Constraint(false, "Node", new Label("Location", "nonEU")), new Constraint(true, "Data", new Label("Encryption", "Encrypted")));

        var repairedDfd = repair(dfd, constraints);
        converter.storeWeb(converter.dfdToWeb(repairedDfd), "repaired.json");
    }

    private DataFlowDiagramAndDictionary repair(DataFlowDiagramAndDictionary dfd, List<Constraint> constraints)
            throws ContradictionException, TimeoutException {
        List<Node> nodes = new ArrayList<>();
        Map<String, String> outPinToAss = new HashMap<>();
        for (var node : dfd.dataFlowDiagram()
                .getNodes()) {

            List<InPin> inPins = new ArrayList<>();
            for (var inPin : node.getBehaviour()
                    .getInPin()) {
                inPins.add(new InPin(inPin.getId()));
            }

            Map<OutPin, List<Label>> outPins = new HashMap<>();
            for (var assignment : node.getBehaviour()
                    .getAssignment()) {
                var outPin = assignment.getOutputPin();
                outPinToAss.put(outPin.getId(), assignment.getId());
                List<Label> outLabels = new ArrayList<>();
                if (assignment instanceof Assignment cast) {
                    for (var label : cast.getOutputLabels()) {
                        var type = ((LabelType) label.eContainer()).getEntityName();
                        var value = label.getEntityName();
                        outLabels.add(new Label(type, value));
                    }
                }
                outPins.put(new OutPin(assignment.getId()), outLabels);
            }

            List<Label> nodeChars = new ArrayList<>();
            for (var property : node.getProperties()) {
                var type = ((LabelType) property.eContainer()).getEntityName();
                var value = property.getEntityName();
                nodeChars.add(new Label(type, value));
            }

            nodes.add(new Node(node.getEntityName(), inPins, outPins, nodeChars));
        }

        List<Edge> edges = new ArrayList<>();
        for (var flow : dfd.dataFlowDiagram()
                .getFlows()) {
            edges.add(new Edge(new OutPin(outPinToAss.get(flow.getSourcePin()
                    .getId())), new InPin(flow.getDestinationPin()
                            .getId())));
        }

        var solutions = new Sat().solve(nodes, edges, constraints);

        Collections.sort(solutions, (list1, list2) -> Integer.compare(list1.size(), list2.size()));
        var minSol = solutions.get(0);

        List<Delta> flatNodes = new ArrayList<>();
        for (var node : nodes) {
            for (var outPin : node.outPins()
                    .keySet()) {
                for (var label : node.outPins()
                        .get(outPin)) {
                    flatNodes.add(new Delta(outPin.id(), new OutDataChar(label.type(), label.value())));
                }
            }
            for (var property : node.nodeChars()) {
                flatNodes.add(new Delta(node.name(), new NodeChar(property.type(), property.value())));
            }
        }

        List<Delta> actions = new ArrayList<>();
        for (var delta : minSol) {
            if (delta.characteristic()
                    .what()
                    .equals("InData"))
                continue;
            if (flatNodes.contains(delta))
                continue;
            actions.add(delta);
        }

        System.out.println(actions);

        var dd = dfd.dataDictionary();
        for (var action : actions) {
            if (action.characteristic()
                    .what()
                    .equals("OutData")) {
                for (var behavior : dd.getBehaviour()) {
                    for (var assignment : behavior.getAssignment()) {
                        if (assignment.getId()
                                .equals(action.where())) {
                            var type = action.characteristic()
                                    .type();
                            var value = action.characteristic()
                                    .value();
                            var label = dd.getLabelTypes()
                                    .stream()
                                    .filter(labelType -> labelType.getEntityName()
                                            .equals(type))
                                    .flatMap(labelType -> labelType.getLabel()
                                            .stream())
                                    .filter(labelValue -> labelValue.getEntityName()
                                            .equals(value))
                                    .findAny()
                                    .get();
                            if (assignment instanceof Assignment cast) {
                                cast.getOutputLabels()
                                        .add(label);
                            }
                        }
                    }
                }
            }
        }

        return dfd;
    }
}
