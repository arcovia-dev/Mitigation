package dev.arcovia.mitigation.sat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;
import java.util.ArrayList;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.web2dfd.Web2DFDConverter;
import org.dataflowanalysis.converter.web2dfd.WebEditorConverterModel;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryFactory;
import org.dataflowanalysis.dfd.dataflowdiagram.dataflowdiagramFactory;
import org.dataflowanalysis.dfd.dataflowdiagram.impl.ExternalImpl;
import org.dataflowanalysis.dfd.dataflowdiagram.impl.StoreImpl;
import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.SetAssignment;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

public class Scaler {
    DataFlowDiagramAndDictionary dfd;
    
    public Scaler() {
        this.dfd = null;
    }
    
    public Scaler(DataFlowDiagramAndDictionary dfd) {
        this.dfd = dfd;
    }

    public Scaler(String dfdLocation) {
        this.dfd = new Web2DFDConverter().convert(new WebEditorConverterModel(dfdLocation));
    }

    /***
     * Scaling the entire DFD, their nodes and flows
     * @param scalingNodes
     * @param scalingFlows
     * @return
     */
    public DataFlowDiagramAndDictionary scaleDFD(int scalingNodes, int scalingFlows) {

        var dd = dfd.dataDictionary();
        var dataFlowDiagram = dfd.dataFlowDiagram();

        duplicateNodes(dataFlowDiagram, dd, scalingNodes);

        duplicateFlows(dataFlowDiagram, dd, scalingFlows);

        return dfd;
    }

    /***
     * Number of Lables is scaled by adding scalingAmount Labels to the dfd (annotated to every node &flow)
     * @param scalingAmount
     * @return
     */
    public DataFlowDiagramAndDictionary scaleLabels(int scalingAmount) {
        var ddFactory = datadictionaryFactory.eINSTANCE;

        var labelType = ddFactory.createLabelType();
        labelType.setEntityName("dummyCategory");

        for (int i = 0; i < scalingAmount; i++) {
            var label = ddFactory.createLabel();
            label.setEntityName("dummy_" + i);
            labelType.getLabel()
                    .add(label);

        }

        dfd.dataDictionary()
                .getLabelTypes()
                .add(labelType);

        for (var node : dfd.dataFlowDiagram()
                .getNodes()) {
            node.getProperties()
                    .addAll(labelType.getLabel());
        }
        for (var behavior : dfd.dataDictionary()
                .getBehavior()) {
            for (var assignment : behavior.getAssignment()) {
                if (assignment instanceof SetAssignment cast) {
                    cast.getOutputLabels()
                            .addAll(labelType.getLabel());
                } else if (assignment instanceof Assignment cast) {
                    cast.getOutputLabels()
                            .addAll(labelType.getLabel());
                }
            }
        }

        return dfd;
    }

    /***
     * Number of LableTypes is scaled by adding scalingAmount Labels to the dfd (annotated to every node &flow)
     * @param scalingAmount
     * @return
     */
    public DataFlowDiagramAndDictionary scaleLabelTypes(int scalingAmount) {
        var ddFactory = datadictionaryFactory.eINSTANCE;

        List<LabelType> labelTypes = new ArrayList<>();

        for (int i = 0; i < scalingAmount; i++) {
            var label = ddFactory.createLabel();
            label.setEntityName("dummyLabelofType_" + i);
            var labelType = ddFactory.createLabelType();
            labelType.setEntityName("dummyType_" + i);
            labelType.getLabel()
                    .add(label);
            dfd.dataDictionary()
                    .getLabelTypes()
                    .add(labelType);
            labelTypes.add(labelType);
        }

        for (var node : dfd.dataFlowDiagram()
                .getNodes()) {
            for (var labelType : labelTypes) {
                node.getProperties()
                        .addAll(labelType.getLabel());
            }
        }

        for (var behavior : dfd.dataDictionary()
                .getBehavior()) {
            for (var assignment : behavior.getAssignment()) {
                if (assignment instanceof SetAssignment cast) {
                    for (var labelType : labelTypes) {
                        cast.getOutputLabels()
                                .addAll(labelType.getLabel());
                    }
                } else if (assignment instanceof Assignment cast) {
                    for (var labelType : labelTypes) {
                        cast.getOutputLabels()
                                .addAll(labelType.getLabel());
                    }
                }
            }
        }

        return dfd;
    }

    /***
     * Scaling the length of a tfg, by chaining scalingAmount sinks to an existing sink
     * @param scalingAmount
     * @return
     */
    public DataFlowDiagramAndDictionary scaleTFGLength(int scalingAmount) {
        var dfdFactory = dataflowdiagramFactory.eINSTANCE;
        var ddFactory = datadictionaryFactory.eINSTANCE;

        var nodes = dfd.dataFlowDiagram()
                .getNodes();

        List<Node> sinks = new ArrayList<>();

        for (var node : nodes) {
            if (node.getBehavior()
                    .getAssignment()
                    .isEmpty()) {
                sinks.add(node);
            }
        }
        if (sinks.isEmpty()) {
            return dfd;
        }

        for (var sink : sinks) {
            for (int i = 0; i < scalingAmount; i++) {
                var node = dfdFactory.createStore();
                node.setEntityName("dummyNode_" + i);

                node.getProperties()
                        .addAll(sink.getProperties());

                var behavior = ddFactory.createBehavior();
                var inPin = ddFactory.createPin();
                behavior.getInPin()
                        .add(inPin);
                node.setBehavior(behavior);

                dfd.dataDictionary()
                        .getBehavior()
                        .add(behavior);

                var outPin = ddFactory.createPin();
                sink.getBehavior()
                        .getOutPin()
                        .add(outPin);

                var forwarding = ddFactory.createForwardingAssignment();
                forwarding.setOutputPin(outPin);
                forwarding.getInputPins()
                        .addAll(sink.getBehavior()
                                .getInPin());

                sink.getBehavior()
                        .getAssignment()
                        .add(forwarding);
                dfd.dataFlowDiagram()
                        .getNodes()
                        .add(node);

                var flow = dfdFactory.createFlow();
                flow.setEntityName("dummyFlow_" + i);
                flow.setDestinationNode(node);
                flow.setDestinationPin(inPin);
                flow.setSourceNode(sink);
                flow.setSourcePin(outPin);

                dfd.dataFlowDiagram()
                        .getFlows()
                        .add(flow);
                sink = node;
            }
        }

        return dfd;
    }

    /***
     * Scaling the amount of TFGs of a dfd by duplicating a random flow scalingAmount times
     * @param scalingAmount
     * @return
     */
    public DataFlowDiagramAndDictionary scaleTFGAmount(int scalingAmount) {
        var dfdFactory = dataflowdiagramFactory.eINSTANCE;
        var ddFactory = datadictionaryFactory.eINSTANCE;

        var nodes = dfd.dataFlowDiagram()
                .getNodes();

        List<Node> sinks = new ArrayList<>();

        for (var node : nodes) {
            if (node.getBehavior()
                    .getAssignment()
                    .isEmpty()) {
                sinks.add(node);
            }
        }
        if (sinks.isEmpty()) {
            return dfd;
        }

        List<Flow> flows = new ArrayList<>();

        for (var sink : sinks) {
            var inPins = sink.getBehavior()
                    .getInPin();
            flows.addAll(dfd.dataFlowDiagram()
                    .getFlows()
                    .stream()
                    .filter(flow -> inPins.contains(flow.getDestinationPin()))
                    .toList());

        }

        for (var flow : flows) {
            var source = flow.getSourceNode();
            var sourcePin = flow.getSourcePin();
            var destination = flow.getDestinationNode();
            var destinationPin = flow.getDestinationPin();

            var sourceBehavior = source.getBehavior();

            List<AbstractAssignment> assignments = sourceBehavior.getAssignment()
                    .stream()
                    .filter(assign -> assign.getOutputPin()
                            .equals(sourcePin))
                    .toList();

            for (int i = 0; i < scalingAmount; i++) {
                flow = dfdFactory.createFlow();
                flow.setSourceNode(source);
                flow.setDestinationNode(destination);
                flow.setDestinationPin(destinationPin);
                flow.setEntityName("dummyFlow_" + i);

                var outPin = ddFactory.createPin();
                flow.setSourcePin(outPin);
                sourceBehavior.getOutPin()
                        .add(outPin);

                for (var assignment : assignments) {
                    if (assignment instanceof Assignment cast) {
                        var newAssignment = ddFactory.createAssignment();
                        newAssignment.setOutputPin(outPin);
                        newAssignment.getInputPins()
                                .add(destinationPin);
                        newAssignment.getOutputLabels()
                                .addAll(cast.getOutputLabels());
                        newAssignment.setTerm(ddFactory.createTRUE());
                        sourceBehavior.getAssignment()
                                .add(newAssignment);
                    } else if (assignment instanceof SetAssignment cast) {
                        var newAssignment = ddFactory.createSetAssignment();
                        newAssignment.setOutputPin(outPin);
                        newAssignment.getOutputLabels()
                                .addAll(cast.getOutputLabels());
                        sourceBehavior.getAssignment()
                                .add(newAssignment);
                    } else if (assignment instanceof ForwardingAssignment cast) {
                        var newAssignment = ddFactory.createForwardingAssignment();

                        newAssignment.setOutputPin(outPin);
                        newAssignment.getInputPins()
                                .addAll(cast.getInputPins());
                        sourceBehavior.getAssignment()
                                .add(newAssignment);
                    }
                }

                dfd.dataFlowDiagram()
                        .getFlows()
                        .add(flow);
            }
        }

        return dfd;
    }
    
    /***
     * Constraints in Usage:
     * - numberWithLabel + numberWithCharacteristic <= numberDummyLabels
     * - use constraints only on DFD scaled using the scaleLabels function
     * Scales constraints in 5 dimesnions
     * @param numberConstraints that get returned
     * @param numberWithLabel: Labels that are positve before neverflows
     * @param numberWithoutLabel: Labels that are negative before neverflows
     * @param numberWithCharacteristic: Labels that are positve after neverflows
     * @param numberWithoutCharacteristic: Labels that are negative after neverflows
     * @param numberDummyLabels: The number of dummy Labels in the scaled DFD (number you previous scaled DFDLabels by)
     * @return
     */
    public List<AnalysisConstraint> scaleConstraint(int numberConstraints, int numberWithLabel, int numberWithoutLabel, int numberWithCharacteristic, int numberWithoutCharacteristic, int numberPositiveLabels){
        List<AnalysisConstraint> constraints = new ArrayList<>();
        
        for (int i = 0; i< numberConstraints; i++) {
            Set<String> withLabel = new HashSet<>();
            Set<String> withoutLabel = new HashSet<>();
            Set<String> withCharacteristic = new HashSet<>();
            Set<String> withoutCharacteristic = new HashSet<>();
            
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            
            while(withLabel.size() < numberWithLabel) {
                withLabel.add(String.valueOf(rnd.nextInt(0, numberPositiveLabels/2 - 1)));
            }
            while(withoutLabel.size() < numberWithCharacteristic) {
                withCharacteristic.add(String.valueOf(rnd.nextInt(numberPositiveLabels/2, numberPositiveLabels)));
            }
            while(withoutLabel.size() < numberWithoutLabel) {
                withoutLabel.add(String.valueOf(rnd.nextInt(numberPositiveLabels+1, numberPositiveLabels*3)));
            }
            while(withCharacteristic.size() < numberWithoutCharacteristic) {
                withoutCharacteristic.add(String.valueOf(rnd.nextInt(numberPositiveLabels*3 + 1, numberPositiveLabels*6)));
            }
                       
            
            AnalysisConstraint constraint = new ConstraintDSL().ofData()
                    .withLabel("dummyCategory", new ArrayList<>(withLabel))
                    .withoutLabel("dummyCategory", new ArrayList<>(withoutLabel))
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("dummyCategory", new ArrayList<>(withCharacteristic))
                    .withoutCharacteristic("dummyCategory", new ArrayList<>(withoutCharacteristic))
                    .create();
            
            constraints.add(constraint);
        }
        
        
        return constraints;
    }
    
    
    /***
     * Copying the entire DFD to achieve the scalingAmount of Nodes
     * @param dataFlowDiagram
     * @param dd
     * @param scalingAmount
     */
    private void duplicateNodes(DataFlowDiagram dataFlowDiagram, DataDictionary dd, int scalingAmount) {
        var dfdFactory = dataflowdiagramFactory.eINSTANCE;
        var ddFactory = datadictionaryFactory.eINSTANCE;

        Map<Node, List<Node>> scalingMapNodes = new HashMap<>();
        Map<Pin, List<Pin>> scalingMapPins = new HashMap<>();

        List<Node> newNodes = new ArrayList<>();

        for (var node : dataFlowDiagram.getNodes()) {
            scalingMapNodes.put(node, new ArrayList<Node>());
            for (int i = 0; i < scalingAmount; i++) {
                Node newNode;

                if (node instanceof ExternalImpl) {
                    newNode = dfdFactory.createExternal();
                } else if (node instanceof StoreImpl) {
                    newNode = dfdFactory.createStore();
                } else {
                    newNode = dfdFactory.createProcess();
                }
                newNode.getProperties()
                        .addAll(node.getProperties());

                var nodeBehavior = node.getBehavior();

                var newBehavior = ddFactory.createBehavior();

                for (var pin : nodeBehavior.getInPin()) {
                    if (scalingMapPins.get(pin) == null) {
                        scalingMapPins.put(pin, new ArrayList<Pin>());
                    }
                    var inPin = ddFactory.createPin();
                    newBehavior.getInPin()
                            .add(inPin);
                    scalingMapPins.get(pin)
                            .add(inPin);
                }
                for (var pin : nodeBehavior.getOutPin()) {
                    if (scalingMapPins.get(pin) == null) {
                        scalingMapPins.put(pin, new ArrayList<Pin>());
                    }
                    var outPin = ddFactory.createPin();
                    newBehavior.getOutPin()
                            .add(outPin);
                    scalingMapPins.get(pin)
                            .add(outPin);
                }

                for (var assignment : nodeBehavior.getAssignment()) {
                    if (assignment instanceof ForwardingAssignment cast) {
                        var newAssignment = ddFactory.createForwardingAssignment();

                        for (var pin : cast.getInputPins()) {
                            newAssignment.getInputPins()
                                    .add(scalingMapPins.get(pin)
                                            .get(i));
                        }
                        newAssignment.setOutputPin(scalingMapPins.get(cast.getOutputPin())
                                .get(i));

                        newBehavior.getAssignment()
                                .add(newAssignment);
                    } else if (assignment instanceof Assignment cast) {
                        var newAssignment = ddFactory.createAssignment();

                        newAssignment.setOutputPin(scalingMapPins.get(cast.getOutputPin())
                                .get(i));

                        newAssignment.getOutputLabels()
                                .addAll(cast.getOutputLabels());

                        var ddTrue = ddFactory.createTRUE();

                        newAssignment.setTerm(ddTrue);

                        newBehavior.getAssignment()
                                .add(newAssignment);
                    } else if (assignment instanceof SetAssignment cast) {
                        var newAssignment = ddFactory.createAssignment();

                        newAssignment.setOutputPin(scalingMapPins.get(cast.getOutputPin())
                                .get(i));

                        newAssignment.getOutputLabels()
                                .addAll(cast.getOutputLabels());

                        var ddTrue = ddFactory.createTRUE();

                        newAssignment.setTerm(ddTrue);

                        newBehavior.getAssignment()
                                .add(newAssignment);
                    }
                }
                dd.getBehavior()
                        .add(newBehavior);
                newNode.setBehavior(newBehavior);
                newNode.setEntityName(node.getEntityName() + "_" + i);
                newNodes.add(newNode);
                scalingMapNodes.get(node)
                        .add(newNode);
            }
        }
        dataFlowDiagram.getNodes()
                .addAll(newNodes);

        List<Flow> newFlows = new ArrayList<>();
        for (var flow : dataFlowDiagram.getFlows()) {
            var sourePin = flow.getSourcePin();
            var sourceNode = flow.getSourceNode();
            var destinationPin = flow.getDestinationPin();
            var destinationNode = flow.getDestinationNode();

            for (int i = 0; i < scalingAmount; i++) {
                var copyFlow = dfdFactory.createFlow();
                copyFlow.setDestinationNode(scalingMapNodes.get(destinationNode)
                        .get(i));
                copyFlow.setDestinationPin(scalingMapPins.get(destinationPin)
                        .get(i));
                copyFlow.setSourceNode(scalingMapNodes.get(sourceNode)
                        .get(i));
                copyFlow.setSourcePin(scalingMapPins.get(sourePin)
                        .get(i));
                copyFlow.setEntityName(flow.getEntityName() + "_" + i);

                newFlows.add(copyFlow);
            }
        }
        dataFlowDiagram.getFlows()
                .addAll(newFlows);
    }

    /***
     * Multiplying all flows by scalingAmount
     * @param dataFlowDiagram
     * @param dd
     * @param scalingAmount
     */
    private void duplicateFlows(DataFlowDiagram dataFlowDiagram, DataDictionary dd, int scalingAmount) {
        var dfdFactory = dataflowdiagramFactory.eINSTANCE;
        var ddFactory = datadictionaryFactory.eINSTANCE;
        Map<Pin, List<Pin>> scalingMap = new HashMap<>();

        List<Flow> newFlows = new ArrayList<>();

        for (var flow : dataFlowDiagram.getFlows()) {
            scalingMap.put(flow.getSourcePin(), new ArrayList<Pin>());

            for (int i = 0; i < scalingAmount; i++) {
                var copyFlow = dfdFactory.createFlow();
                copyFlow.setDestinationNode(flow.getDestinationNode());
                copyFlow.setDestinationPin(flow.getDestinationPin());
                copyFlow.setSourceNode(flow.getSourceNode());
                copyFlow.setEntityName(flow.getEntityName() + "-" + i);

                var outPin = ddFactory.createPin();

                copyFlow.setSourcePin(outPin);

                newFlows.add(copyFlow);

                scalingMap.get(flow.getSourcePin())
                        .add(outPin);
            }
        }
        dataFlowDiagram.getFlows()
                .addAll(newFlows);

        for (var behavior : dd.getBehavior()) {
            List<AbstractAssignment> newAssignments = new ArrayList<>();
            for (var assignment : behavior.getAssignment()) {
                if (assignment instanceof Assignment cast) {
                    for (var newOutPin : scalingMap.get(cast.getOutputPin())) {
                        var assignmentNew = ddFactory.createAssignment();

                        assignmentNew.setOutputPin(newOutPin);

                        behavior.getOutPin()
                                .add(newOutPin);

                        assignmentNew.getOutputLabels()
                                .addAll(cast.getOutputLabels());

                        var ddTrue = ddFactory.createTRUE();

                        assignmentNew.setTerm(ddTrue);

                        newAssignments.add(assignmentNew);
                    }
                } else if (assignment instanceof SetAssignment cast) {
                    for (var newOutPin : scalingMap.get(cast.getOutputPin())) {
                        var assignmentNew = ddFactory.createAssignment();

                        assignmentNew.setOutputPin(newOutPin);

                        behavior.getOutPin()
                                .add(newOutPin);

                        assignmentNew.getOutputLabels()
                                .addAll(cast.getOutputLabels());

                        var ddTrue = ddFactory.createTRUE();

                        assignmentNew.setTerm(ddTrue);

                        newAssignments.add(assignmentNew);
                    }
                }

                else if (assignment instanceof ForwardingAssignment cast) {
                    for (var newOutPin : scalingMap.get(cast.getOutputPin())) {
                        var assignmentNew = ddFactory.createForwardingAssignment();

                        assignmentNew.setOutputPin(newOutPin);

                        assignmentNew.getInputPins()
                                .addAll(cast.getInputPins());

                        behavior.getOutPin()
                                .add(newOutPin);

                        newAssignments.add(assignmentNew);
                    }
                }
            }
            behavior.getAssignment()
                    .addAll(newAssignments);
        }
    }
}
