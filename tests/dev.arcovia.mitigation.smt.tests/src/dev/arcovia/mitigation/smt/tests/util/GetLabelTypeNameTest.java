package dev.arcovia.mitigation.smt.tests.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;

import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arcovia.mitigation.smt.util.Util;

class GetLabelTypeNameTest extends UtilTestBase {

    @ParameterizedTest
    @MethodSource("cases")
    void testGetLabelTypeByName(String lookupName, boolean shouldExist) {
        DataDictionary dd = ddFactory.createDataDictionary();

        LabelType typeA = ddFactory.createLabelType();
        typeA.setEntityName("A");

        LabelType typeB = ddFactory.createLabelType();
        typeB.setEntityName("B");

        dd.getLabelTypes()
                .add(typeA);
        dd.getLabelTypes()
                .add(typeB);

        LabelType result = Util.getLabelTypeByName(dd, lookupName);

        if (shouldExist) {
            assertEquals(lookupName, result.getEntityName());
        } else {
            assertNull(result);
        }
    }

    static Stream<Arguments> cases() {
        return Stream.of(Arguments.of("A", true), // existing
                Arguments.of("B", true), // existing
                Arguments.of("C", false), // missing
                Arguments.of("", false), // empty lookup
                Arguments.of(null, false) // null lookup
        );
    }
}
