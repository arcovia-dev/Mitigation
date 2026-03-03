package dev.arcovia.mitigation.smt.tests.constraints;

import java.util.List;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dfd.dsl.DFDVertexType;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;

public class VertexTypeTest extends AbstractSelectorConstraintTest {

    @Override
    protected Stream<SelectorTestCase> cases() {
        return vertexTypeSourceCases();
    }

    static Stream<SelectorTestCase> vertexTypeSourceCases() {
        return Stream.of(new SelectorTestCase("Source Type", new ConstraintDSL().fromNode()
                .withType(DFDVertexType.EXTERNAL)
                .neverFlows()
                .toVertex()
                .create(), List.of(FALSE), List.of(FALSE)), new SelectorTestCase("Sink Type",
                        new ConstraintDSL().fromNode()
                                .withType(DFDVertexType.STORE)
                                .neverFlows()
                                .toVertex()
                                .create(),
                        List.of(TRUE), List.of(FALSE)),
                new SelectorTestCase("Other type", new ConstraintDSL().fromNode()
                        .withType(DFDVertexType.PROCESS)
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(TRUE), List.of(TRUE)),
                new SelectorTestCase("Not source type", new ConstraintDSL().fromNode()
                        .withoutType(DFDVertexType.EXTERNAL)
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(TRUE), List.of(FALSE)),
                new SelectorTestCase("Not sink type", new ConstraintDSL().fromNode()
                        .withoutType(DFDVertexType.STORE)
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(FALSE), List.of(FALSE)),
                new SelectorTestCase("Not other type", new ConstraintDSL().fromNode()
                        .withoutType(DFDVertexType.PROCESS)
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(FALSE), List.of(FALSE))

        );
    }
}
