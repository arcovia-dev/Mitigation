package dev.arcovia.mitigation.smt.tests.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arcovia.mitigation.smt.utils.ParsingUtils;

class RelevantNodeLabelsTest extends UtilTestBase {

    @ParameterizedTest
    @MethodSource("nodeConstraintParameters")
    void testGetRelevantNodeLabelsAdd(List<String> withoutSingleFrom, List<List<String>> withoutMultiFrom, List<String> withSingleFrom,
            List<List<String>> withMultiFrom, List<String> withoutSingleTo, List<List<String>> withoutMultiTo, List<String> withSingleTo,
            List<List<String>> withMultiTo, Set<String> expectedAddNames, Set<String> expectedRemoveNames) {

        var ctx = buildConstraintAndDictionary(withoutSingleFrom, withoutMultiFrom, withSingleFrom, withMultiFrom, withoutSingleTo, withoutMultiTo,
                withSingleTo, withMultiTo);

        Set<Label> result = ParsingUtils.getRelevantNodeLabelsAdd(ctx.dd, List.of(ctx.constraint));
        assertEquals(resolve(ctx.labelsByName, expectedAddNames), result);
    }

    @ParameterizedTest
    @MethodSource("nodeConstraintParameters")
    void testGetRelevantNodeLabelsRemove(List<String> withoutSingleFrom, List<List<String>> withoutMultiFrom, List<String> withSingleFrom,
            List<List<String>> withMultiFrom, List<String> withoutSingleTo, List<List<String>> withoutMultiTo, List<String> withSingleTo,
            List<List<String>> withMultiTo, Set<String> expectedAddNames, Set<String> expectedRemoveNames) {

        var ctx = buildConstraintAndDictionary(withoutSingleFrom, withoutMultiFrom, withSingleFrom, withMultiFrom, withoutSingleTo, withoutMultiTo,
                withSingleTo, withMultiTo);

        Set<Label> result = ParsingUtils.getRelevantNodeLabelsRemove(ctx.dd, List.of(ctx.constraint));
        assertEquals(resolve(ctx.labelsByName, expectedRemoveNames), result);
    }

    private record Ctx(DataDictionary dd, AnalysisConstraint constraint, Map<String, Label> labelsByName) {
    }

    private Ctx buildConstraintAndDictionary(List<String> withoutSingleFrom, List<List<String>> withoutMultiFrom, List<String> withSingleFrom,
            List<List<String>> withMultiFrom, List<String> withoutSingleTo, List<List<String>> withoutMultiTo, List<String> withSingleTo,
            List<List<String>> withMultiTo) {

        DataDictionary dd = ddFactory.createDataDictionary();

        LabelType type = ddFactory.createLabelType();
        type.setEntityName("A");

        Map<String, Label> labelsByName = new HashMap<>();

        Set<String> allNames = new HashSet<>();
        allNames.addAll(withoutSingleFrom);
        withoutMultiFrom.forEach(allNames::addAll);
        allNames.addAll(withSingleFrom);
        withMultiFrom.forEach(allNames::addAll);

        allNames.addAll(withoutSingleTo);
        withoutMultiTo.forEach(allNames::addAll);
        allNames.addAll(withSingleTo);
        withMultiTo.forEach(allNames::addAll);

        for (String name : allNames) {
            Label label = ddFactory.createLabel();
            label.setEntityName(name);
            type.getLabel()
                    .add(label);
            labelsByName.put(name, label);
        }

        dd.getLabelTypes()
                .add(type);

        // Build constraint:
        // - characteristics on source: .fromVertex().withCharacteristic /
        // withoutCharacteristic
        // - characteristics on destination: .toVertex().withCharacteristic /
        // withoutCharacteristic
        var fromBuilder = new ConstraintDSL().fromNode();

        for (String name : withoutSingleFrom)
            fromBuilder = fromBuilder.withoutCharacteristic("A", name);
        for (List<String> names : withoutMultiFrom)
            fromBuilder = fromBuilder.withoutCharacteristic("A", names);

        for (String name : withSingleFrom)
            fromBuilder = fromBuilder.withCharacteristic("A", name);
        for (List<String> names : withMultiFrom)
            fromBuilder = fromBuilder.withCharacteristic("A", names);

        var toBuilder = fromBuilder.neverFlows()
                .toVertex();

        for (String name : withoutSingleTo)
            toBuilder = toBuilder.withoutCharacteristic("A", name);
        for (List<String> names : withoutMultiTo)
            toBuilder = toBuilder.withoutCharacteristic("A", names);

        for (String name : withSingleTo)
            toBuilder = toBuilder.withCharacteristic("A", name);
        for (List<String> names : withMultiTo)
            toBuilder = toBuilder.withCharacteristic("A", names);

        AnalysisConstraint constraint = toBuilder.create();

        return new Ctx(dd, constraint, labelsByName);
    }

    private Set<Label> resolve(Map<String, Label> labelsByName, Set<String> names) {
        Set<Label> out = new HashSet<>();
        for (String n : names)
            out.add(labelsByName.get(n));
        return out;
    }

    static Stream<Arguments> nodeConstraintParameters() {
        return Stream.of(
                // 1) only WITHOUT on fromVertex -> Add: {B,C}, Remove: {}
                Arguments.of(List.of("B"), List.of(List.of("B", "C")), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                        Set.of("B", "C"), Set.of()),
                // 2) only WITH on fromVertex -> Add: {}, Remove: {B,C}
                Arguments.of(List.of(), List.of(), List.of("B"), List.of(List.of("B", "C")), List.of(), List.of(), List.of(), List.of(), Set.of(),
                        Set.of("B", "C")),
                // 3) only WITHOUT on toVertex -> Add: {D,E}, Remove: {}
                Arguments.of(List.of(), List.of(), List.of(), List.of(), List.of("D"), List.of(List.of("D", "E")), List.of(), List.of(),
                        Set.of("D", "E"), Set.of()),
                // 4) only WITH on toVertex -> Add: {}, Remove: {F,G}
                Arguments.of(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of("F"), List.of(List.of("F", "G")), Set.of(),
                        Set.of("F", "G")),
                // 5) mixed across both sides
                Arguments.of(List.of("B"), List.of(List.of("C")), List.of("X"), List.of(List.of("Y")), List.of("D"), List.of(List.of("E")),
                        List.of("F"), List.of(List.of("G")), Set.of("B", "C", "D", "E"), Set.of("X", "Y", "F", "G")),
                // 6) none -> both empty
                Arguments.of(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Set.of(), Set.of()));
    }
}
