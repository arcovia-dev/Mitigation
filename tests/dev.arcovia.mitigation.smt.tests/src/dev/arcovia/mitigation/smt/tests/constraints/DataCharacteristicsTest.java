package dev.arcovia.mitigation.smt.tests.constraints;

import java.util.List;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;

public class DataCharacteristicsTest extends AbstractSelectorConstraintTest {

    @Override
    protected Stream<SelectorTestCase> cases() {
        return dataCharacteristicsCases();
    }

    static Stream<SelectorTestCase> dataCharacteristicsCases() {
        return Stream.of(
                // Sink constraint should only be satisfied when the label is removed.
                new SelectorTestCase("withLabel that exists -> sink satisfies only if label removed", new ConstraintDSL().ofData()
                        .withLabel(LABELTYPE, LABEL1)
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(TRUE), List.of(unset(LABEL1))),
                new SelectorTestCase("withLabel that does not exist -> sink satisfies", new ConstraintDSL().ofData()
                        .withLabel(LABELTYPE, LABEL3)
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(TRUE), List.of(TRUE)),

                new SelectorTestCase("withoutLabel that exists -> sink satisfies", new ConstraintDSL().ofData()
                        .withoutLabel(LABELTYPE, LABEL1)
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(TRUE), List.of(TRUE)),
                new SelectorTestCase("withoutLabel that does not exist -> labels needs to be added", new ConstraintDSL().ofData()
                        .withoutLabel(LABELTYPE, LABEL3)
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(TRUE), List.of(set(LABEL3))));
    }

}
