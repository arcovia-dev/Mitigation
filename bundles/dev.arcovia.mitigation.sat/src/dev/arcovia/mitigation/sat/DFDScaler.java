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
import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.SetAssignment;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;

public class DFDScaler {
    public static DataFlowDiagramAndDictionary scaleDFD(DataFlowDiagramAndDictionary dfd, int scaling) {

        var dfdFactory = dataflowdiagramFactory.eINSTANCE;
        var ddFactory = datadictionaryFactory.eINSTANCE;
        
        var dd = dfd.dataDictionary();
        var dataFlowDiagram = dfd.dataFlowDiagram();
        
        Map<Pin, List<Pin>> scalingMap = new HashMap<>();
        
        List<Flow> newFlows = new ArrayList<>();
        
        for (var flow : dataFlowDiagram.getFlows()) {
            scalingMap.put(flow.getSourcePin(), new ArrayList<Pin>());
            
            
            for (int i= 0; i<scaling; i++) {
                var copyFlow = dfdFactory.createFlow();
                copyFlow.setDestinationNode(flow.getDestinationNode());
                copyFlow.setDestinationPin(flow.getDestinationPin());
                copyFlow.setSourceNode(flow.getSourceNode());
                
                //new InPin --> how to add it??
                var outPin = ddFactory.createPin();
                
                copyFlow.setSourcePin(outPin);
                
                
                newFlows.add(copyFlow);
                
                scalingMap.get(flow.getSourcePin()).add(outPin);
            }
        }
        dataFlowDiagram.getFlows().addAll(newFlows);
        
      //need to duplicate behavior as well
        for (var behavior : dd.getBehavior()) {
            List<AbstractAssignment> newAssignments = new ArrayList<>();
            for (var assignment : behavior.getAssignment()) {
                if (assignment instanceof Assignment cast ) {
                    for (var newOutPin : scalingMap.get(cast.getOutputPin())) {
                        var assignmentNew = ddFactory.createAssignment();

                        assignmentNew.setOutputPin(newOutPin);

                        behavior.getOutPin().add(newOutPin);

                        assignmentNew.getOutputLabels().addAll(cast.getOutputLabels());

                        var ddTrue = ddFactory.createTRUE();

                        assignmentNew.setTerm(ddTrue);

                        newAssignments.add(assignmentNew);
                    }
                }
                else if (assignment instanceof SetAssignment cast ) {
                    for (var newOutPin : scalingMap.get(cast.getOutputPin())) {
                        var assignmentNew = ddFactory.createAssignment();

                        assignmentNew.setOutputPin(newOutPin);

                        behavior.getOutPin().add(newOutPin);

                        assignmentNew.getOutputLabels().addAll(cast.getOutputLabels());

                        var ddTrue = ddFactory.createTRUE();

                        assignmentNew.setTerm(ddTrue);

                        newAssignments.add(assignmentNew);
                    }
                }
                
                else if (assignment instanceof ForwardingAssignment cast) {
                    for (var newOutPin : scalingMap.get(cast.getOutputPin())) {
                        var assignmentNew = ddFactory.createForwardingAssignment();

                        assignmentNew.setOutputPin(newOutPin);

                        assignmentNew.getInputPins().addAll(cast.getInputPins());

                        behavior.getOutPin().add(newOutPin);

                        newAssignments.add(assignmentNew); 
                    }   
                }                 
            }
            behavior.getAssignment().addAll(newAssignments); 
        }
        
        
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
}
