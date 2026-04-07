package dev.arcovia.mitigation.smt.tests.constraints;

import java.util.List;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;

public class VertexNameTest extends AbstractSelectorConstraintTest {

    @Override
    protected Stream<SelectorTestCase> cases() {
        return vertexNameCases();
    }

    static Stream<SelectorTestCase> vertexNameCases() {
        return Stream.of(new SelectorTestCase("Sink name", new ConstraintDSL().fromNode()
                .neverFlows()
                .toVertex()
                .withVertexName(SINK)
                .create(), List.of(TRUE), List.of(FALSE)), new SelectorTestCase("Source Name",
                        new ConstraintDSL().fromNode()
                                .neverFlows()
                                .toVertex()
                                .withVertexName(SOURCE)
                                .create(),
                        List.of(FALSE), List.of(TRUE)),
                new SelectorTestCase("Other name", new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .withVertexName("otherName")
                        .create(), List.of(TRUE), List.of(TRUE)),
                new SelectorTestCase("Not sink name", new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .withoutVertexName(SINK)
                        .create(), List.of(FALSE), List.of(TRUE)),
                new SelectorTestCase("Not source name", new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .withoutVertexName(SOURCE)
                        .create(), List.of(TRUE), List.of(FALSE)),
                new SelectorTestCase("Not other name", new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .withoutVertexName("otherName")
                        .create(), List.of(FALSE), List.of(FALSE)));
    }
}
