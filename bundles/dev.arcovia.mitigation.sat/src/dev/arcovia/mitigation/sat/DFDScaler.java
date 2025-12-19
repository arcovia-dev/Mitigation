package dev.arcovia.mitigation.sat;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

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
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.SetAssignment;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

public class DFDScaler {
    public static DataFlowDiagramAndDictionary scaleDFD(DataFlowDiagramAndDictionary dfd, int scaling) {

        var dd = dfd.dataDictionary();
        var dataFlowDiagram = dfd.dataFlowDiagram();
        
        duplicateNodes(dataFlowDiagram, dd, scaling);
        
        duplicateFlows(dataFlowDiagram, dd, scaling);

        return dfd;
    }

    public static DataFlowDiagramAndDictionary scaleDFD(DataFlowDiagramAndDictionary dfd) {
        return scaleDFD(dfd, 5);
    }

    public static DataFlowDiagramAndDictionary scaleDFD(String dfdLocation) {
        var dfd = new Web2DFDConverter().convert(new WebEditorConverterModel(dfdLocation));
        return scaleDFD(dfd, 5);
    }

    public static DataFlowDiagramAndDictionary scaleDFD(String dfdLocation, int scaling) {
        var dfd = new Web2DFDConverter().convert(new WebEditorConverterModel(dfdLocation));
        return scaleDFD(dfd, scaling);
    }
    private static void duplicateNodes(DataFlowDiagram dataFlowDiagram, DataDictionary dd, int scaling) {
        var dfdFactory = dataflowdiagramFactory.eINSTANCE;
        var ddFactory = datadictionaryFactory.eINSTANCE;
        
        Map<Node, List<Node>> scalingMap = new HashMap<>();
        Map<Pin, List<Pin>> scalingMapPins = new HashMap<>();
        
        List<Node> newNodes = new ArrayList<>();
   
         
        for (var node : dataFlowDiagram.getNodes()) {
            scalingMap.put(node, new ArrayList<Node>());
            for (int i = 0; i < scaling; i++) {
                Node newNode;

                if (node instanceof ExternalImpl) {
                    newNode = dfdFactory.createExternal();
                }
                else if (node instanceof StoreImpl) {
                    newNode = dfdFactory.createStore();
                }
                else {
                    newNode = dfdFactory.createProcess();
                }
                newNode.getProperties().addAll(node.getProperties());
                
                var nodeBehavior = node.getBehavior();
                
                var newBehavior = ddFactory.createBehavior();
                
                for (var p : nodeBehavior.getInPin()) {
                    if (scalingMapPins.get(p) == null) {
                        scalingMapPins.put(p, new ArrayList<Pin>());
                    }
                    var inPin = ddFactory.createPin();
                    newBehavior.getInPin().add(inPin);
                    scalingMapPins.get(p).add(inPin);
                }
                for (var p : nodeBehavior.getOutPin()) {
                    if (scalingMapPins.get(p) == null) {
                        scalingMapPins.put(p, new ArrayList<Pin>());
                    }
                    var outPin = ddFactory.createPin();
                    newBehavior.getOutPin().add(outPin);
                    scalingMapPins.get(p).add(outPin);
                }
                
                
                for (var assignment : nodeBehavior.getAssignment()) {
                    if (assignment instanceof ForwardingAssignment cast) {
                        var newAssignment = ddFactory.createForwardingAssignment();
                        
                        for (var pin: cast.getInputPins()) {
                            newAssignment.getInputPins().add(scalingMapPins.get(pin).get(i));
                        }
                        newAssignment.setOutputPin(scalingMapPins.get(cast.getOutputPin()).get(i));
                                                
                        newBehavior.getAssignment().add(newAssignment);
                    }
                    else if (assignment instanceof Assignment cast) {
                        var newAssignment = ddFactory.createAssignment();
                        
                        newAssignment.setOutputPin(scalingMapPins.get(cast.getOutputPin()).get(i));
                        
                        newAssignment.getOutputLabels()
                        .addAll(cast.getOutputLabels());

                        var ddTrue = ddFactory.createTRUE();
        
                        newAssignment.setTerm(ddTrue);
                        
                        
                        newBehavior.getAssignment().add(newAssignment);
                    }
                    else if (assignment instanceof SetAssignment cast) {
                        var newAssignment = ddFactory.createAssignment();
                        
                        newAssignment.setOutputPin(scalingMapPins.get(cast.getOutputPin()).get(i));
                        
                        newAssignment.getOutputLabels()
                        .addAll(cast.getOutputLabels());

                        var ddTrue = ddFactory.createTRUE();
        
                        newAssignment.setTerm(ddTrue);
                        
                        newBehavior.getAssignment().add(newAssignment);
                    }
                }
                dd.getBehavior().add(newBehavior);
                newNode.setBehavior(newBehavior);
                newNode.setEntityName(node.getEntityName()+ "_"+ i);
                newNodes.add(newNode);
                scalingMap.get(node).add(newNode);
            }
        }
        dataFlowDiagram.getNodes().addAll(newNodes);
        
        List<Flow> newFlows = new ArrayList<>();
        for (var flow : dataFlowDiagram.getFlows()) {
            var sourePin = flow.getSourcePin();
            var sourceNode = flow.getSourceNode();
            var destinationPin = flow.getDestinationPin();
            var destinationNode = flow.getDestinationNode();
            
            for(int i = 0; i < scaling; i++) {
                var copyFlow = dfdFactory.createFlow();
                copyFlow.setDestinationNode(scalingMap.get(destinationNode).get(i));
                copyFlow.setDestinationPin(scalingMapPins.get(destinationPin).get(i));
                copyFlow.setSourceNode(scalingMap.get(sourceNode).get(i));
                copyFlow.setSourcePin(scalingMapPins.get(sourePin).get(i));
                copyFlow.setEntityName(flow.getEntityName()+"_"+i);

                newFlows.add(copyFlow);
            }
        }
        dataFlowDiagram.getFlows()
        .addAll(newFlows);
    }
    

    private static void duplicateFlows(DataFlowDiagram dataFlowDiagram, DataDictionary dd, int scaling) {
        var dfdFactory = dataflowdiagramFactory.eINSTANCE;
        var ddFactory = datadictionaryFactory.eINSTANCE;
        Map<Pin, List<Pin>> scalingMap = new HashMap<>();

        List<Flow> newFlows = new ArrayList<>();

        for (var flow : dataFlowDiagram.getFlows()) {
            scalingMap.put(flow.getSourcePin(), new ArrayList<Pin>());

            for (int i = 0; i < scaling; i++) {
                var copyFlow = dfdFactory.createFlow();
                copyFlow.setDestinationNode(flow.getDestinationNode());
                copyFlow.setDestinationPin(flow.getDestinationPin());
                copyFlow.setSourceNode(flow.getSourceNode());
                copyFlow.setEntityName(flow.getEntityName()+"-"+i);
                
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
