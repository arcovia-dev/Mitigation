package dev.arcovia.mitigation.smt.tests.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Behavior;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.UnsetAssignment;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.operations.UnsetAssignmentOperation;

class UnsetAssignmentOperationTest extends OperationTestBase {

    @Test
    void doOperationAddsUnsetAssignmentToMatchingBehavior() {
        var dfd = emptyDfd();

        Pin pin = ddFactory.createPin();
        pin.setId("p1");

        Behavior behavior = ddFactory.createBehavior();
        behavior.getOutPin()
                .add(pin);
        dfd.dataDictionary()
                .getBehavior()
                .add(behavior);

        var label = ddFactory.createLabel();
        label.setEntityName("L");

        new UnsetAssignmentOperation(pin, label).doOperation(dfd);

        List<AbstractAssignment> assigns = behavior.getAssignment();
        assertTrue(assigns.stream()
                .anyMatch(a -> a instanceof UnsetAssignment));

        UnsetAssignment ua = (UnsetAssignment) assigns.stream()
                .filter(a -> a instanceof UnsetAssignment)
                .findFirst()
                .get();

        assertEquals(pin, ua.getOutputPin());
        assertEquals(List.of(label), ua.getOutputLabels());
        assertNotNull(ua.getId());
    }

    @Test
    void undoOperationRemovesMatchingUnsetAssignment() {
        var dfd = emptyDfd();

        Pin pin = ddFactory.createPin();
        pin.setId("p1");

        Behavior behavior = ddFactory.createBehavior();
        behavior.getOutPin()
                .add(pin);
        dfd.dataDictionary()
                .getBehavior()
                .add(behavior);

        var label = ddFactory.createLabel();
        label.setEntityName("L");

        new UnsetAssignmentOperation(pin, label).doOperation(dfd);
        assertFalse(behavior.getAssignment()
                .isEmpty());

        new UnsetAssignmentOperation(pin, label).undoOperation(dfd);

        assertTrue(behavior.getAssignment()
                .isEmpty());
    }

    @Test
    void doOperationNoopIfNoBehaviorMatchesPin() {
        var dfd = emptyDfd();

        Pin pin = ddFactory.createPin();
        pin.setId("p1");

        var label = ddFactory.createLabel();
        label.setEntityName("L");

        new UnsetAssignmentOperation(pin, label).doOperation(dfd);

        assertTrue(dfd.dataDictionary()
                .getBehavior()
                .isEmpty());
    }
}
