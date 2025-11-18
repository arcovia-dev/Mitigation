package dev.arcovia.mitigation.sat;

import java.util.HashMap;
import java.util.Map;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryFactory;
import org.dataflowanalysis.dfd.dataflowdiagram.dataflowdiagramFactory;
import org.dataflowanalysis.dfd.datadictionary.Pin;

public class DFDScaler {
    public static DataFlowDiagramAndDictionary scaleDFD(DataFlowDiagramAndDictionary dfd, int scaling) {

        var dfdFactory = dataflowdiagramFactory.eINSTANCE;
        var ddFactory = datadictionaryFactory.eINSTANCE;
        
        Map<Pin, Pin> scalingMap = new HashMap<>();

        //need to duplicate behavior as well
        for (var beh : dfd.dataDictionary().getBehavior()) {
            
        }
        
        
        for (var flow : dfd.dataFlowDiagram().getFlows()) {
            
            for (int i= 0; i<scaling; i++) {
                var copyFlow = dfdFactory.createFlow();
                copyFlow.setDestinationNode(flow.getDestinationNode());
                copyFlow.setDestinationPin(flow.getDestinationPin());
                copyFlow.setSourceNode(flow.getSourceNode());
                
                //new InPin --> how to add it??
                var inPin = ddFactory.createPin();
                
                copyFlow.setSourcePin(inPin);
                
               
                
                
            }
        }
        
        return dfd;
    }
    
    public static DataFlowDiagramAndDictionary scaleDFD(DataFlowDiagramAndDictionary dfd) {
        return scaleDFD(dfd, 5);
    }
}
