package dev.arcovia.mitigation.smt.tests.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.operations.LabelOperation;

class LabelOperationTest extends OperationTestBase {

    @Test
    void doOperationAddsLabelWhenTypeExists() {
        var dfd = emptyDfd();
        LabelType typeA = ddFactory.createLabelType();
        typeA.setEntityName("A");
        dfd.dataDictionary()
                .getLabelTypes()
                .add(typeA);

        new LabelOperation("A", "B", "42").doOperation(dfd);

        assertTrue(typeA.getLabel()
                .stream()
                .anyMatch(l -> "B".equals(l.getEntityName())));
        assertEquals("42", typeA.getLabel()
                .stream()
                .filter(l -> "B".equals(l.getEntityName()))
                .findFirst()
                .get()
                .getId());
    }

    @Test
    void doOperationNoopWhenTypeMissing() {
        var dfd = emptyDfd();

        new LabelOperation("A", "B", "42").doOperation(dfd);

        assertTrue(dfd.dataDictionary()
                .getLabelTypes()
                .isEmpty());
    }

    @Test
    void undoOperationRemovesLabelByName() {
        var dfd = emptyDfd();
        LabelType typeA = ddFactory.createLabelType();
        typeA.setEntityName("A");
        dfd.dataDictionary()
                .getLabelTypes()
                .add(typeA);

        new LabelOperation("A", "B", "42").doOperation(dfd);
        new LabelOperation("A", "C", "43").doOperation(dfd);

        new LabelOperation("A", "B", "42").undoOperation(dfd);

        assertFalse(typeA.getLabel()
                .stream()
                .anyMatch(l -> "B".equals(l.getEntityName())));
        assertTrue(typeA.getLabel()
                .stream()
                .anyMatch(l -> "C".equals(l.getEntityName())));
    }
}
