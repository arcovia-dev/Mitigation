package dev.arcovia.mitigation.smt.tests.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dataflowanalysis.dfd.datadictionary.AND;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelReference;
import org.dataflowanalysis.dfd.datadictionary.NOT;
import org.dataflowanalysis.dfd.datadictionary.OR;
import org.dataflowanalysis.dfd.datadictionary.Term;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arcovia.mitigation.smt.utils.ParsingUtils;

class ReduceToLabelReferenceTest extends UtilTestBase {

    @ParameterizedTest
    @MethodSource("cases")
    void testReduceToLabelReferences(Term term, Set<String> expectedLabelNames) {
        Set<LabelReference> result = ParsingUtils.reduceToLabelReferences(term);

        Set<String> actualLabelNames = result.stream()
                .map(lr -> lr.getLabel()
                        .getEntityName())
                .collect(Collectors.toSet());

        assertEquals(expectedLabelNames, actualLabelNames);
    }

    static Stream<Arguments> cases() {
        Term tTrue = ddFactory.createTRUE();

        NOT notTrue = ddFactory.createNOT();
        notTrue.setNegatedTerm(ddFactory.createTRUE());

        LabelReference r1 = ref("L1");

        NOT notR1 = ddFactory.createNOT();
        notR1.setNegatedTerm(ref("L1"));

        AND andMix = ddFactory.createAND();
        andMix.getTerms()
                .addAll(List.of(ddFactory.createTRUE(), notTrue, ref("L1"), notR1));

        OR orMix = ddFactory.createOR();
        orMix.getTerms()
                .addAll(List.of(ref("L2"), andMix));

        AND nested = ddFactory.createAND();
        nested.getTerms()
                .addAll(List.of(orMix, ref("L3")));

        return Stream.of(Arguments.of(tTrue, Set.of()), Arguments.of(notTrue, Set.of()), Arguments.of(r1, Set.of("L1")),
                Arguments.of(notR1, Set.of("L1")), Arguments.of(andMix, Set.of("L1")), Arguments.of(orMix, Set.of("L1", "L2")),
                Arguments.of(nested, Set.of("L1", "L2", "L3")));
    }

    private static LabelReference ref(String labelName) {
        Label l = ddFactory.createLabel();
        l.setEntityName(labelName);

        LabelReference r = ddFactory.createLabelReference();
        r.setLabel(l);
        return r;
    }
}
