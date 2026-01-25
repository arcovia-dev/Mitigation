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
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.SetAssignment;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

public class DFDScaler {
    DataFlowDiagramAndDictionary dfd;
    int scaling = 5;
    public DFDScaler(DataFlowDiagramAndDictionary dfd) {
        this.dfd = dfd;
    }
    
    public DFDScaler(String dfdLocation) {
        this.dfd = new Web2DFDConverter().convert(new WebEditorConverterModel(dfdLocation));
    }

    
    
    public DataFlowDiagramAndDictionary scaleDFD(int scalingNodes, int scalingFlows) {

        var dd = dfd.dataDictionary();
        var dataFlowDiagram = dfd.dataFlowDiagram();
        
        duplicateNodes(dataFlowDiagram, dd, scalingNodes);
        
        duplicateFlows(dataFlowDiagram, dd, scalingFlows);

        return dfd;
    }

    public DataFlowDiagramAndDictionary scaleDFD() {
        return scaleDFD(scaling,scaling);
    }   
    
    public DataFlowDiagramAndDictionary scaleLabels(int scaling) {        
        var ddFactory = datadictionaryFactory.eINSTANCE;
        
        var labelType = ddFactory.createLabelType();
        labelType.setEntityName("dummyCategory");        
        
        for (int i = 0; i<scaling; i++) {
            var label = ddFactory.createLabel();
            label.setEntityName("dummy_" + i);
            labelType.getLabel().add(label);
            
        }
        
        dfd.dataDictionary().getLabelTypes()
        .add(labelType);
        
        for (var node : dfd.dataFlowDiagram().getNodes()) {
            node.getProperties().addAll(labelType.getLabel());
        }
        for (var behavior : dfd.dataDictionary().getBehavior()) {
            for (var assignment : behavior.getAssignment()) {
                if (assignment instanceof SetAssignment cast) {
                    cast.getOutputLabels().addAll(labelType.getLabel());
                }
                else if (assignment instanceof Assignment cast) {
                    cast.getOutputLabels().addAll(labelType.getLabel());
                }
            }
        }
        
        return dfd;
    }
    
    public DataFlowDiagramAndDictionary scaleLabelTypes(int scaling) {        
        var ddFactory = datadictionaryFactory.eINSTANCE;
        
        var label = ddFactory.createLabel();
        label.setEntityName("dummyLabel");        
        
        List<LabelType> labelTypes = new ArrayList<>();
        
        for (int i = 0; i<scaling; i++) {
            var labelType = ddFactory.createLabelType();
            labelType.setEntityName("dummyType_" + i);
            labelType.getLabel().add(label);
            dfd.dataDictionary().getLabelTypes()
            .add(labelType);
            labelTypes.add(labelType);
        }

        for (var node : dfd.dataFlowDiagram().getNodes()) {
            for (var labelType: labelTypes) {
                node.getProperties().addAll(labelType.getLabel());
            }
        }
        
        for (var behavior : dfd.dataDictionary().getBehavior()) {
            for (var assignment : behavior.getAssignment()) {
                if (assignment instanceof SetAssignment cast) {
                    for (var labelType: labelTypes) {
                        cast.getOutputLabels().addAll(labelType.getLabel());
                    }
                }
                else if (assignment instanceof Assignment cast) {
                    for (var labelType: labelTypes) {
                        cast.getOutputLabels().addAll(labelType.getLabel());
                    }
                }
            }
        }
        
        
        return dfd;
    }
    
    public DataFlowDiagramAndDictionary scaleLTFGLength(int scaling) {
        var dfdFactory = dataflowdiagramFactory.eINSTANCE;
        var ddFactory = datadictionaryFactory.eINSTANCE;
        
        var nodes = dfd.dataFlowDiagram().getNodes();
        
        Node sink = null;
        
        for (var node : nodes) {
            if (node.getBehavior().getAssignment().isEmpty()) {
                sink = node;
                break;
            }
        }
        if (sink == null) return dfd;
        
        for (int i = 0; i<scaling; i++) {
            var node = dfdFactory.createStore();
            node.setEntityName("dummyNode_" + i);
            
            node.getProperties().addAll(sink.getProperties());
            
            var behavior = ddFactory.createBehavior();
            var inPin = ddFactory.createPin();
            behavior.getInPin().add(inPin);
            node.setBehavior(behavior);
            
            dfd.dataDictionary().getBehavior().add(behavior);
            
            var outPin = ddFactory.createPin();
            sink.getBehavior().getOutPin().add(outPin);
            
            var forwarding = ddFactory.createForwardingAssignment();
            forwarding.setOutputPin(outPin);
            forwarding.getInputPins().addAll(sink.getBehavior().getInPin());
            
            sink.getBehavior().getAssignment().add(forwarding);
            dfd.dataFlowDiagram().getNodes().add(node);
            
            var flow = dfdFactory.createFlow();
            flow.setEntityName("dummyFlow_" + i);
            flow.setDestinationNode(node);
            flow.setDestinationPin(inPin);
            flow.setSourceNode(sink);
            flow.setSourcePin(outPin);
            
            dfd.dataFlowDiagram().getFlows().add(flow);
            sink = node;
        }
        
        return dfd;
    }
    
    public DataFlowDiagramAndDictionary scaleLTFGAmount(int scaling) {
        var dfdFactory = dataflowdiagramFactory.eINSTANCE;
        var ddFactory = datadictionaryFactory.eINSTANCE;
        
        var flow = dfd.dataFlowDiagram().getFlows().get(0);
        var source = flow.getSourceNode();
        var sourcePin = flow.getSourcePin();
        var destination = flow.getDestinationNode();
        var destinationPin = flow.getDestinationPin();
        
        var sourceBehavior = source.getBehavior();
        
        List<AbstractAssignment> assignments = sourceBehavior.getAssignment().stream().filter(assign -> assign.getOutputPin().equals(sourcePin)).toList();
        
        for (int i = 0; i < scaling; i++) {
            flow = dfdFactory.createFlow();
            flow.setSourceNode(source);
            flow.setDestinationNode(destination);
            flow.setDestinationPin(destinationPin);
            flow.setEntityName("dummyFlow_" + i);
            
            var outPin = ddFactory.createPin();
            flow.setSourcePin(outPin);
            sourceBehavior.getOutPin().add(outPin);
            
            for (var assignment : assignments) {
                if (assignment instanceof Assignment cast) {
                    var newAssignment = ddFactory.createAssignment();
                    newAssignment.setOutputPin(outPin);
                    newAssignment.getInputPins().add(destinationPin);
                    newAssignment.getOutputLabels().addAll(cast.getOutputLabels());
                    newAssignment.setTerm(ddFactory.createTRUE());
                    sourceBehavior.getAssignment().add(newAssignment);
                }
                else if (assignment instanceof SetAssignment cast) {
                    var newAssignment = ddFactory.createSetAssignment();
                    newAssignment.setOutputPin(outPin);
                    newAssignment.getOutputLabels().addAll(cast.getOutputLabels());
                    sourceBehavior.getAssignment().add(newAssignment);
                }
                else if (assignment instanceof ForwardingAssignment cast){
                    var newAssignment = ddFactory.createForwardingAssignment();
                    
                    newAssignment.setOutputPin(outPin);
                    newAssignment.getInputPins().addAll(cast.getInputPins());
                    sourceBehavior.getAssignment().add(newAssignment);
                }
            }
            
            dfd.dataFlowDiagram().getFlows().add(flow);
        }
        
        
        
        return dfd;
    }
    
    
    private void duplicateNodes(DataFlowDiagram dataFlowDiagram, DataDictionary dd, int scaling) {
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
    

    private void duplicateFlows(DataFlowDiagram dataFlowDiagram, DataDictionary dd, int scaling) {
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
