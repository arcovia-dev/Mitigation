package dev.arcovia.mitigation.smt.operations;

import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

/**
 * This operation adds a label to a node
 */
public class NodeLabelAddOperation extends AbstractNodeLabelOperation {

    public NodeLabelAddOperation(Node node, Label label) {
        super(node, label);
    }

    @Override
    public String toString() {
        return "Add " + label.getEntityName() + " to Node " + node.getId() + " " + node.getEntityName();
    }

}
