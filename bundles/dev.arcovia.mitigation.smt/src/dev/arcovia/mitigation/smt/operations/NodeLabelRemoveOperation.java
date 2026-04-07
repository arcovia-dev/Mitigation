package dev.arcovia.mitigation.smt.operations;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

/**
 * This operation removes a label from a node
 */
public class NodeLabelRemoveOperation extends AbstractNodeLabelOperation {

    public NodeLabelRemoveOperation(Node node, Label label) {
        super(node, label);
    }

    /**
     * super.undoOperation(dfd) removes the label. Therefore we inverted the calls compared to NodeLabelAddOperation.
     */
    @Override
    public DataFlowDiagramAndDictionary doOperation(DataFlowDiagramAndDictionary dfd) {
        return super.undoOperation(dfd);
    }

    /**
     * super.doOperation(dfd) adds the label
     */
    @Override
    public DataFlowDiagramAndDictionary undoOperation(DataFlowDiagramAndDictionary dfd) {
        return super.doOperation(dfd);
    }

    @Override
    public String toString() {
        return "Remove " + label.getEntityName() + " from Node " + node.getId() + " " + node.getEntityName();
    }
}
