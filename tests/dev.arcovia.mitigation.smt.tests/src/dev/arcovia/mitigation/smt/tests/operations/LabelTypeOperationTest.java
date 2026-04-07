package dev.arcovia.mitigation.smt.tests.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.operations.LabelTypeOperation;

class LabelTypeOperationTest extends OperationTestBase {

    @Test
    void doOperationAddsLabelType() {
        var dfd = emptyDfd();

        new LabelTypeOperation("A", "1").doOperation(dfd);

        assertTrue(dfd.dataDictionary()
                .getLabelTypes()
                .stream()
                .anyMatch(t -> "A".equals(t.getEntityName())));
        assertEquals("1", dfd.dataDictionary()
                .getLabelTypes()
                .stream()
                .filter(t -> "A".equals(t.getEntityName()))
                .findFirst()
                .get()
                .getId());
    }

    @Test
    void undoOperationRemovesLabelTypeByName() {
        var dfd = emptyDfd();
        new LabelTypeOperation("A", "1").doOperation(dfd);
        new LabelTypeOperation("B", "2").doOperation(dfd);

        new LabelTypeOperation("A", "1").undoOperation(dfd);

        assertFalse(dfd.dataDictionary()
                .getLabelTypes()
                .stream()
                .anyMatch(t -> "A".equals(t.getEntityName())));
        assertTrue(dfd.dataDictionary()
                .getLabelTypes()
                .stream()
                .anyMatch(t -> "B".equals(t.getEntityName())));
    }
}
