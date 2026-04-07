package dev.arcovia.mitigation.smt.tests.constraints;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Behavior;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.SetAssignment;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryFactory;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.dataflowanalysis.dfd.dataflowdiagram.dataflowdiagramFactory;
import static dev.arcovia.mitigation.smt.tests.constraints.AbstractSelectorConstraintTest.*;

public class DFDProvider {
    private static final dataflowdiagramFactory dfdFactory = dataflowdiagramFactory.eINSTANCE;
    private static final datadictionaryFactory ddFactory = datadictionaryFactory.eINSTANCE;

    protected static DataFlowDiagramAndDictionary buildTestCase() {
        var dd = ddFactory.createDataDictionary();
        var dfd = dfdFactory.createDataFlowDiagram();

        LabelType dummyType = ddFactory.createLabelType();
        dummyType.setEntityName(LABELTYPE);
        dummyType.setId("type1");

        Label dummyLabel1 = ddFactory.createLabel();
        dummyLabel1.setEntityName(LABEL1);
        dummyLabel1.setId("label1");

        Label dummyLabel2 = ddFactory.createLabel();
        dummyLabel2.setEntityName(LABEL2);
        dummyLabel2.setId("label2");

        dummyType.getLabel()
                .add(dummyLabel1);
        dummyType.getLabel()
                .add(dummyLabel2);
        dd.getLabelTypes()
                .add(dummyType);

        Node source = dfdFactory.createExternal();
        source.setEntityName(SOURCE);
        source.setId("node1");
        source.getProperties()
                .add(dummyLabel1);
        dfd.getNodes()
                .add(source);

        Pin sourceOut = ddFactory.createPin();
        sourceOut.setEntityName("sourceOut");
        sourceOut.setId(PIN);

        SetAssignment set = ddFactory.createSetAssignment();
        set.setEntityName("set");
        set.setId("set1");
        set.setOutputPin(sourceOut);
        set.getOutputLabels()
                .add(dummyLabel1);
        set.getOutputLabels()
                .add(dummyLabel2);

        Behavior sourceBehavior = ddFactory.createBehavior();
        sourceBehavior.setEntityName("sourceBehavior");
        sourceBehavior.setId("behavior1");
        sourceBehavior.getAssignment()
                .add(set);
        sourceBehavior.getOutPin()
                .add(sourceOut);

        dd.getBehavior()
                .add(sourceBehavior);
        source.setBehavior(sourceBehavior);

        Node sink = dfdFactory.createStore();
        sink.setEntityName(SINK);
        sink.setId("node2");
        sink.getProperties()
                .add(dummyLabel2);
        dfd.getNodes()
                .add(sink);

        Pin sinkIn = ddFactory.createPin();
        sinkIn.setEntityName("sinkIn");
        sinkIn.setId("pin2");

        Behavior sinkBehavior = ddFactory.createBehavior();
        sinkBehavior.setEntityName("sinkBehavior");
        sinkBehavior.setId("behavior2");
        sinkBehavior.getInPin()
                .add(sinkIn);

        dd.getBehavior()
                .add(sinkBehavior);
        sink.setBehavior(sinkBehavior);

        Flow flow = dfdFactory.createFlow();
        flow.setEntityName(FLOW);
        flow.setId("flow1");
        flow.setSourcePin(sourceOut);
        flow.setSourceNode(source);
        flow.setDestinationPin(sinkIn);
        flow.setDestinationNode(sink);

        dfd.getFlows()
                .add(flow);

        return new DataFlowDiagramAndDictionary(dfd, dd);
    }

}
