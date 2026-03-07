package dev.arcovia.mitigation.smt.tests.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arcovia.mitigation.smt.utils.ParsingUtils;

class AnalysisCharacteristicsTest extends UtilTestBase {

    @ParameterizedTest
    @MethodSource("characteristicCases")
    void testGetAnalysisCharacteristics(AnalysisConstraint constraint, int expectedCount, Set<String> expectedEntries) {

        var result = ParsingUtils.getAnalysisCharacteristics(List.of(constraint));

        assertEquals(expectedCount, result.size());

        // compare printed form
        Set<String> actualEntries = result.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertEquals(expectedEntries, actualEntries);
    }

    static Stream<Arguments> characteristicCases() {
        return Stream.of(
                // []
                Arguments.of(new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .create(), 0, Set.of()),

                // [A.B]
                Arguments.of(new ConstraintDSL().ofData()
                        .withLabel("A", "B")
                        .neverFlows()
                        .toVertex()
                        .create(), 1, Set.of("A.B")),

                // [A.B]
                Arguments.of(new ConstraintDSL().ofData()
                        .withoutLabel("A", "B")
                        .neverFlows()
                        .toVertex()
                        .create(), 1, Set.of("A.B")),

                // [A.B]
                Arguments.of(new ConstraintDSL().fromNode()
                        .withCharacteristic("A", "B")
                        .neverFlows()
                        .toVertex()
                        .create(), 1, Set.of("A.B")),

                // [A.B]
                Arguments.of(new ConstraintDSL().fromNode()
                        .withoutCharacteristic("A", "B")
                        .neverFlows()
                        .toVertex()
                        .create(), 1, Set.of("A.B")),

                // [A.B, A.C, A.D, A.E]
                Arguments.of(new ConstraintDSL().ofData()
                        .withLabel("A", "B")
                        .withoutLabel("A", "C")
                        .neverFlows()
                        .toVertex()
                        .withCharacteristic("A", "D")
                        .withoutCharacteristic("A", "E")
                        .create(), 4, Set.of("A.B", "A.C", "A.D", "A.E")));
    }
}
