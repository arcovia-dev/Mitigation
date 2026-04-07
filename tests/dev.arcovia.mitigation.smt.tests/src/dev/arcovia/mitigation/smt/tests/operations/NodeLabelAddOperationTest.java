package dev.arcovia.mitigation.smt.tests.operations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.operations.NodeLabelAddOperation;

class NodeLabelAddOperationTest extends OperationTestBase {

    @Test
    void doOperationAddsLabelToMatchingNode() {
        var dfd = emptyDfd();

        Node node = dfdFactory.createProcess();
        node.setEntityName("P");
        node.setId("n1");
        dfd.dataFlowDiagram()
                .getNodes()
                .add(node);

        var label = ddFactory.createLabel();
        label.setEntityName("L");

        new NodeLabelAddOperation(node, label).doOperation(dfd);

        assertTrue(node.getProperties()
                .contains(label));
    }

    @Test
    void undoOperationRemovesLabelFromMatchingNode() {
        var dfd = emptyDfd();

        Node node = dfdFactory.createProcess();
        node.setEntityName("P");
        node.setId("n1");
        dfd.dataFlowDiagram()
                .getNodes()
                .add(node);

        var label = ddFactory.createLabel();
        label.setEntityName("L");
        node.getProperties()
                .add(label);

        new NodeLabelAddOperation(node, label).undoOperation(dfd);

        assertFalse(node.getProperties()
                .contains(label));
    }
}
