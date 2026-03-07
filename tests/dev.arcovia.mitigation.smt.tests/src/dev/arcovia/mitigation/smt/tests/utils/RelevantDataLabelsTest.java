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
import org.dataflowanalysis.analysis.dsl.constraint.DSLDataSourceSelector;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arcovia.mitigation.smt.utils.ParsingUtils;

class RelevantDataLabelsTest extends ParsingUtilsTestBase {

    @ParameterizedTest
    @MethodSource("constraintParameters")
    void testGetRelevantDataLabelsAdd(List<String> withoutSingle, List<List<String>> withoutMulti, List<String> withSingle,
            List<List<String>> withMulti, Set<String> expectedAddNames, Set<String> expectedRemoveNames) {

        var ctx = buildConstraintAndDictionary(withoutSingle, withoutMulti, withSingle, withMulti);

        Set<Label> result = ParsingUtils.getRelevantDataLabelsAdd(ctx.dd, List.of(ctx.constraint));
        assertEquals(resolve(ctx.labelsByName, expectedAddNames), result);
    }

    @ParameterizedTest
    @MethodSource("constraintParameters")
    void testGetRelevantDataLabelsRemove(List<String> withoutSingle, List<List<String>> withoutMulti, List<String> withSingle,
            List<List<String>> withMulti, Set<String> expectedAddNames, Set<String> expectedRemoveNames) {

        var ctx = buildConstraintAndDictionary(withoutSingle, withoutMulti, withSingle, withMulti);
        Set<Label> result = ParsingUtils.getRelevantDataLabelsRemove(ctx.dd, List.of(ctx.constraint));
        assertEquals(resolve(ctx.labelsByName, expectedRemoveNames), result);
    }

    private record Ctx(DataDictionary dd, AnalysisConstraint constraint, Map<String, Label> labelsByName) {
    }

    private Ctx buildConstraintAndDictionary(List<String> withoutSingle, List<List<String>> withoutMulti, List<String> withSingle,
            List<List<String>> withMulti) {

        DataDictionary dd = ddFactory.createDataDictionary();

        LabelType type = ddFactory.createLabelType();
        type.setEntityName("A");

        Map<String, Label> labelsByName = new HashMap<>();

        Set<String> allNames = new HashSet<>();
        allNames.addAll(withoutSingle);
        withoutMulti.forEach(allNames::addAll);
        allNames.addAll(withSingle);
        withMulti.forEach(allNames::addAll);

        for (String name : allNames) {
            Label label = ddFactory.createLabel();
            label.setEntityName(name);
            type.getLabel()
                    .add(label);
            labelsByName.put(name, label);
        }

        dd.getLabelTypes()
                .add(type);

        DSLDataSourceSelector builder = new ConstraintDSL().ofData();

        for (String name : withoutSingle)
            builder = builder.withoutLabel("A", name);

        for (List<String> names : withoutMulti)
            builder = builder.withoutLabel("A", names);

        for (String name : withSingle)
            builder = builder.withLabel("A", name);

        for (List<String> names : withMulti)
            builder = builder.withLabel("A", names);

        AnalysisConstraint constraint = builder.neverFlows()
                .toVertex()
                .create();

        return new Ctx(dd, constraint, labelsByName);
    }

    private Set<Label> resolve(Map<String, Label> labelsByName, Set<String> names) {
        Set<Label> out = new HashSet<>();
        for (String n : names)
            out.add(labelsByName.get(n));
        return out;
    }

    static Stream<Arguments> constraintParameters() {
        return Stream.of(
                // only without (single + multi) => Add: B,C ; Remove: empty
                Arguments.of(List.of("B"), List.of(List.of("B", "C")), List.of(), List.of(), Set.of("B", "C"), Set.of()),
                // only with (single + multi) => Add: empty ; Remove: B,C
                Arguments.of(List.of(), List.of(), List.of("B"), List.of(List.of("B", "C")), Set.of(), Set.of("B", "C")),
                // mixed => Add from without, Remove from with
                Arguments.of(List.of("B"), List.of(List.of("C", "D")), List.of("E"), List.of(List.of("F", "G")), Set.of("B", "C", "D"),
                        Set.of("E", "F", "G")),
                // duplicates across singles/multis => still sets
                Arguments.of(List.of("B"), List.of(List.of("B", "C")), List.of("D"), List.of(List.of("D", "E")), Set.of("B", "C"), Set.of("D", "E")),
                // none => both empty
                Arguments.of(List.of(), List.of(), List.of(), List.of(), Set.of(), Set.of()));
    }
}
