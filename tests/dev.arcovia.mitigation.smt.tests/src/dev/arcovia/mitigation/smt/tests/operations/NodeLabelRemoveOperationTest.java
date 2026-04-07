package dev.arcovia.mitigation.smt.tests.operations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.operations.NodeLabelRemoveOperation;

class NodeLabelRemoveOperationTest extends OperationTestBase {

    @Test
    void doOperationRemovesLabelFromMatchingNode() {
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

        new NodeLabelRemoveOperation(node, label).doOperation(dfd);

        assertFalse(node.getProperties()
                .contains(label));
    }

    @Test
    void undoOperationAddsLabelBackToMatchingNode() {
        var dfd = emptyDfd();

        Node node = dfdFactory.createProcess();
        node.setEntityName("P");
        node.setId("n1");
        dfd.dataFlowDiagram()
                .getNodes()
                .add(node);

        var label = ddFactory.createLabel();
        label.setEntityName("L");

        new NodeLabelRemoveOperation(node, label).undoOperation(dfd);

        assertTrue(node.getProperties()
                .contains(label));
    }
}
