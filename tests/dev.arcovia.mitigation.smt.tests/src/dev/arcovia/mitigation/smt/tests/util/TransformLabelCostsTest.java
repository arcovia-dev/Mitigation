package dev.arcovia.mitigation.smt.tests.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arcovia.mitigation.smt.util.Util;

class TransformLabelCostsTest extends UtilTestBase {

    @ParameterizedTest
    @MethodSource("cases")
    void testTransformLabelCosts(Map<String, Integer> input, Map<String, Integer> expectedByName) {
        DataDictionary dd = ddFactory.createDataDictionary();

        Map<String, Label> labelsByQualifiedName = new HashMap<>();

        // Create label types and labels referenced in test cases
        expectedByName.keySet()
                .forEach(qn -> {
                    String[] parts = qn.split("\\.");
                    String typeName = parts[0];
                    String labelName = parts[1];

                    LabelType type = dd.getLabelTypes()
                            .stream()
                            .filter(t -> typeName.equals(t.getEntityName()))
                            .findFirst()
                            .orElseGet(() -> {
                                LabelType t = ddFactory.createLabelType();
                                t.setEntityName(typeName);
                                dd.getLabelTypes()
                                        .add(t);
                                return t;
                            });

                    Label label = ddFactory.createLabel();
                    label.setEntityName(labelName);
                    type.getLabel()
                            .add(label);

                    labelsByQualifiedName.put(qn, label);
                });

        Map<Label, Integer> result = Util.transformLabelCosts(dd, input);

        Map<Label, Integer> expected = new HashMap<>();
        expectedByName.forEach((qn, cost) -> expected.put(labelsByQualifiedName.get(qn), cost));

        assertEquals(expected, result);
    }

    static Stream<Arguments> cases() {
        return Stream.of(
                // single entry
                Arguments.of(Map.of("A.B", 5), Map.of("A.B", 5)),

                // multiple labels in one type
                Arguments.of(Map.of("A.B", 3, "A.C", 7), Map.of("A.B", 3, "A.C", 7)),

                // multiple label types
                Arguments.of(Map.of("A.B", 1, "X.Y", 9), Map.of("A.B", 1, "X.Y", 9)),

                // empty input
                Arguments.of(Map.of(), Map.of()));
    }
}
