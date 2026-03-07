package dev.arcovia.mitigation.smt.tests.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arcovia.mitigation.smt.utils.ParsingUtils;

class LabelContainmentTest extends ParsingUtilsTestBase {

    @ParameterizedTest
    @MethodSource("labelTypeCases")
    void testContainsLabelType(String lookupName, boolean expected) {
        DataDictionary dd = ddFactory.createDataDictionary();

        LabelType typeA = ddFactory.createLabelType();
        typeA.setEntityName("A");

        LabelType typeB = ddFactory.createLabelType();
        typeB.setEntityName("B");

        dd.getLabelTypes()
                .add(typeA);
        dd.getLabelTypes()
                .add(typeB);

        boolean result = ParsingUtils.containsLabelType(dd, lookupName);
        assertEquals(expected, result);
    }

    static Stream<Arguments> labelTypeCases() {
        return Stream.of(Arguments.of("A", true), Arguments.of("B", true), Arguments.of("C", false), Arguments.of("", false),
                Arguments.of(null, false));
    }

    @ParameterizedTest
    @MethodSource("labelCases")
    void testContainsLabel(String lookupName, boolean expected) {
        LabelType type = ddFactory.createLabelType();
        type.setEntityName("A");

        Label labelB = ddFactory.createLabel();
        labelB.setEntityName("B");

        Label labelC = ddFactory.createLabel();
        labelC.setEntityName("C");

        type.getLabel()
                .add(labelB);
        type.getLabel()
                .add(labelC);

        boolean result = ParsingUtils.containsLabel(type, lookupName);
        assertEquals(expected, result);
    }

    static Stream<Arguments> labelCases() {
        return Stream.of(Arguments.of("B", true), Arguments.of("C", true), Arguments.of("D", false), Arguments.of("", false),
                Arguments.of(null, false));
    }
}
