package dev.arcovia.mitigation.smt.tests.operations;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryFactory;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.dataflowanalysis.dfd.dataflowdiagram.dataflowdiagramFactory;

public abstract class OperationTestBase {

    protected static final dataflowdiagramFactory dfdFactory = dataflowdiagramFactory.eINSTANCE;
    protected static final datadictionaryFactory ddFactory = datadictionaryFactory.eINSTANCE;

    protected DataFlowDiagramAndDictionary emptyDfd() {
        DataFlowDiagram diagram = dfdFactory.createDataFlowDiagram();
        DataDictionary dict = ddFactory.createDataDictionary();
        return new DataFlowDiagramAndDictionary(diagram, dict);
    }
}
