package dev.arcovia.mitigation.smt.tests.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dfd.dsl.DFDVertexType;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arcovia.mitigation.smt.util.Util;

class ContainsSelectorTest extends UtilTestBase {

    @ParameterizedTest
    @MethodSource("vertexNameCases")
    void testContainsVertexNameSelector(AnalysisConstraint constraint, boolean expected) {
        assertEquals(expected, Util.containsVertexNameSelector(List.of(constraint)));
    }

    @ParameterizedTest
    @MethodSource("vertexTypeCases")
    void testContainsVertexTypeSelector(AnalysisConstraint constraint, boolean expected) {
        assertEquals(expected, Util.containsVertexTypeSelector(List.of(constraint)));
    }

    static Stream<Arguments> vertexNameCases() {
        return Stream.of(
                // no vertex destination selectors
                Arguments.of(new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .create(), false),
                // destination vertex selector contains VertexNameSelector via
                // toVertex().withName(...)
                Arguments.of(new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .withVertexName("v1")
                        .create(), true));
    }

    static Stream<Arguments> vertexTypeCases() {
        return Stream.of(
                // no vertex type selectors
                Arguments.of(new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .create(), false),
                // source selector has VertexTypeSelector via fromNode().withType(...)
                Arguments.of(new ConstraintDSL().fromNode()
                        .withType(DFDVertexType.PROCESS)
                        .neverFlows()
                        .toVertex()
                        .create(), true));
    }
}
