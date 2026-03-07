package dev.arcovia.mitigation.smt.tests.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.analysis.dsl.selectors.CharacteristicsSelectorData;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arcovia.mitigation.smt.utils.ParsingUtils;

class GetLabelsForCharacteristicsTest extends UtilTestBase {

    @ParameterizedTest
    @MethodSource("cases")
    void testGetLabelsForCharacteristics(AnalysisConstraint constraint, Set<String> expectedLabelNames) {
        // Build DataDictionary containing exactly the labels we expect (and optionally
        // extras)
        DataDictionary dd = ddFactory.createDataDictionary();

        LabelType typeA = ddFactory.createLabelType();
        typeA.setEntityName("A");

        Map<String, Label> labelsByName = new HashMap<>();
        for (String name : expectedLabelNames) {
            Label l = ddFactory.createLabel();
            l.setEntityName(name);
            typeA.getLabel()
                    .add(l);
            labelsByName.put(name, l);
        }

        // add an extra label not referenced (edge: ensure not returned)
        Label extra = ddFactory.createLabel();
        extra.setEntityName("Z");
        typeA.getLabel()
                .add(extra);

        dd.getLabelTypes()
                .add(typeA);

        var chars = ParsingUtils.getAnalysisCharacteristics(List.of(constraint));
        List<CharacteristicsSelectorData> data = new ArrayList<>(chars);
        Set<Label> result = ParsingUtils.getLabelsForCharacteristics(dd, data);

        Set<Label> expected = expectedLabelNames.stream()
                .map(labelsByName::get)
                .collect(Collectors.toSet());

        assertEquals(expected, result);
    }

    static Stream<Arguments> cases() {
        return Stream.of(
                // edge: no characteristics => empty result
                Arguments.of(new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .create(), Set.of()),

                // single characteristic (data add)
                Arguments.of(new ConstraintDSL().ofData()
                        .withLabel("A", "B")
                        .neverFlows()
                        .toVertex()
                        .create(), Set.of("B")),

                // single characteristic (node remove)
                Arguments.of(new ConstraintDSL().fromNode()
                        .withoutCharacteristic("A", "C")
                        .neverFlows()
                        .toVertex()
                        .create(), Set.of("C")),

                // duplicates across data/node + add/remove should de-dupe in Set
                Arguments.of(new ConstraintDSL().ofData()
                        .withLabel("A", "B")
                        .withoutLabel("A", "B")
                        .neverFlows()
                        .toVertex()
                        .withCharacteristic("A", "B")
                        .withoutCharacteristic("A", "B")
                        .create(), Set.of("B")),

                // mixed multiple
                Arguments.of(new ConstraintDSL().ofData()
                        .withLabel("A", "B")
                        .withoutLabel("A", "C")
                        .neverFlows()
                        .toVertex()
                        .withCharacteristic("A", "D")
                        .withoutCharacteristic("A", "E")
                        .create(), Set.of("B", "C", "D", "E")));
    }
}
