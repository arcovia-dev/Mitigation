package dev.arcovia.mitigation.sat;

import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;

import java.util.*;

public class ModelCostCalculator {
    DataFlowDiagramAndDictionary dfd;
    List<Constraint> constraints;
    Map<Label, Integer> costs;
    List<Node> nodes = new ArrayList<>();

    public ModelCostCalculator(DataFlowDiagramAndDictionary dfd, List<Constraint> constraints, Map<Label, Integer> costs) {
        this.dfd = dfd;
        this.constraints = constraints;
        this.costs = costs;
    }
    public int calculateCost() {
        var flatendNodes = getFlatendNodes();
        var constrainLabel = getConstraintLabel();
        int cost = 0;
        for (var term : flatendNodes) {
            var label = term.compositeLabel().label();
            if (constrainLabel.contains(label) && costs.containsKey(label)) {
                cost += costs.get(label);
            }
        }
        return cost;
    }

    private List<Label> getConstraintLabel() {
        var label = new HashSet<Label>();
        for (var constraint : constraints) {
            for (var literal : constraint.literals()) {
                label.add(literal.compositeLabel()
                        .label());
            }
        }
        return List.copyOf(label);
    }

    private List<Term> getFlatendNodes() {
        List<Term> flatendNodes = new ArrayList<>();

        getNodes();

        for (var node : nodes) {
            for (var outPin : node.outPins()
                    .keySet()) {
                for (var label : node.outPins()
                        .get(outPin)) {
                    flatendNodes.add(new Term(outPin.id(), new OutgoingDataLabel(label)));
                }
            }
            for (var property : node.nodeChars()) {
                flatendNodes.add(new Term(node.id(), new NodeLabel(new Label(property.type(), property.value()))));
            }
        }
        return flatendNodes;
    }
    Comparator<OutPin> outPinComparator = (s1, s2) -> {
        boolean isNumeric1 = s1.id()
                .matches("\\d+");
        boolean isNumeric2 = s2.id()
                .matches("\\d+");

        if (isNumeric1 && isNumeric2) {
            return Integer.compare(Integer.parseInt(s1.id()), Integer.parseInt(s2.id())); // Sort numerically
        } else if (!isNumeric1 && !isNumeric2) {
            return s1.id()
                    .compareTo(s2.id()); // Sort lexicographically
        } else {
            return isNumeric1 ? -1 : 1; // Numbers first, then UUIDs
        }
    };

    private void getNodes (){
        var resourceProvider = new DFDModelResourceProvider(dfd.dataDictionary(), dfd.dataFlowDiagram());
        var analysis = new DFDDataFlowAnalysisBuilder().standalone()
                .useCustomResourceProvider(resourceProvider)
                .build();

        analysis.initializeAnalysis();
        var flowGraph = analysis.findFlowGraphs();
        flowGraph.evaluate();
        for (var tfg : flowGraph.getTransposeFlowGraphs()) {
            for (var vertex : tfg.getVertices()) {
                DFDVertex node = (DFDVertex) vertex;
                final String id = node.getReferencedElement()
                        .getId();
                Map<InPin, List<Label>> inPinLabelMap = new HashMap<>();

                TreeMap<OutPin, List<Label>> outPinLabelMap = new TreeMap<>(outPinComparator);
                for (var outPin : node.getAllOutgoingDataCharacteristics()) {
                    List<Label> pinLabel = new ArrayList<>();
                    for (var property : outPin.getAllCharacteristics()) {
                        var type = property.getTypeName();
                        var value = property.getValueName();
                        pinLabel.add(new Label(type, value));
                    }
                    outPinLabelMap.put(new OutPin(outPin.getVariableName()), pinLabel);
                }
                // if not in list add otherwise add missing pins

                if (nodes.stream()
                        .noneMatch(n -> n.id()
                                .equals(id))) {
                    List<Label> nodeLabels = new ArrayList<>();
                    for (var property : node.getAllVertexCharacteristics()) {
                        var type = property.getTypeName();
                        var value = property.getValueName();
                        nodeLabels.add(new Label(type, value));
                    }
                    nodes.add(new Node(id, inPinLabelMap, outPinLabelMap, nodeLabels));

                } else {
                    var satNode = nodes.stream()
                            .filter(n -> n.id()
                                    .equals(id))
                            .findFirst()
                            .get();
                    for (var outPin : outPinLabelMap.keySet()) {
                        if (satNode.outPins()
                                .containsKey(outPin)) {
                            Set<Label> outPinLabel = new HashSet<>(outPinLabelMap.get(outPin));

                            outPinLabel.addAll(new ArrayList<>(satNode.outPins()
                                    .get(outPin)));

                            satNode.outPins()
                                    .put(outPin, new ArrayList<>(outPinLabel));
                        } else
                            satNode.outPins()
                                    .put(outPin, outPinLabelMap.get(outPin));
                    }
                }
            }
        }
    }
}
