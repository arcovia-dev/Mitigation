package dev.arcovia.mitigation.smt.tests.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dfd.dsl.DFDVertexType;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.analysis.dsl.constraint.DSLNodeSourceSelector;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arcovia.mitigation.smt.utils.ParsingUtils;

class RelevantVertexTypesTest extends UtilTestBase {

    @ParameterizedTest
    @MethodSource("constraintCases")
    void testGetRelevantVertexTypes(List<DFDVertexType> withTypes, List<DFDVertexType> withoutTypes, Set<DFDVertexType> expected) {

        DSLNodeSourceSelector builder = new ConstraintDSL().fromNode();

        for (DFDVertexType t : withTypes)
            builder = builder.withType(t);

        for (DFDVertexType t : withoutTypes)
            builder = builder.withoutType(t);

        AnalysisConstraint constraint = builder.neverFlows()
                .toVertex()
                .create();

        assertEquals(expected, ParsingUtils.getRelevantVertexTypes(List.of(constraint)));
    }

    static Stream<Arguments> constraintCases() {
        DFDVertexType[] types = DFDVertexType.values();
        List<List<DFDVertexType>> subsets = generateSubsetsStatic(types);

        List<Arguments> args = new ArrayList<>();

        // empty constraint case
        args.add(Arguments.of(List.of(), List.of(), Set.of()));

        for (List<DFDVertexType> withTypes : subsets) {
            for (List<DFDVertexType> withoutTypes : subsets) {

                Set<DFDVertexType> expected = new HashSet<>();
                expected.addAll(withTypes);
                expected.addAll(withoutTypes);

                args.add(Arguments.of(withTypes, withoutTypes, expected));
            }
        }

        return args.stream();
    }

    private static List<List<DFDVertexType>> generateSubsetsStatic(DFDVertexType[] types) {

        List<List<DFDVertexType>> subsets = new ArrayList<>();
        subsets.add(new ArrayList<>());

        for (DFDVertexType type : types) {
            List<List<DFDVertexType>> additions = new ArrayList<>();
            for (List<DFDVertexType> existing : subsets) {
                List<DFDVertexType> copy = new ArrayList<>(existing);
                copy.add(type);
                additions.add(copy);
            }
            subsets.addAll(additions);
        }

        return subsets;
    }
}
