package dev.arcovia.mitigation.smt.tests.constraints;

import java.util.List;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;

public class VertexCharacteristicsTest extends AbstractSelectorConstraintTest {

    @Override
    protected Stream<SelectorTestCase> cases() {
        return Stream.concat(vertexCharacteristicsDestinationCases(), vertexCharacteristicsSourceCases());
    }

    static Stream<SelectorTestCase> vertexCharacteristicsDestinationCases() {

        new ConstraintDSL().fromNode()
                .withCharacteristic("DummyType", List.of("A", "B"))
                .neverFlows()
                .toVertex()
                .create();
        new ConstraintDSL().fromNode()
                .neverFlows()
                .toVertex()
                .withCharacteristic("DummyType", List.of("A", "B"))
                .create();

        return Stream.of(new SelectorTestCase("Source Label", new ConstraintDSL().fromNode()
                .neverFlows()
                .toVertex()
                .withCharacteristic(LABELTYPE, LABEL1)
                .create(), List.of("(not " + nodeLabel(SOURCE, LABEL1) + ")"), List.of(TRUE)),
                new SelectorTestCase("Sink label", new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .withCharacteristic(LABELTYPE, LABEL2)
                        .create(), List.of(TRUE), List.of("(not " + nodeLabel(SINK, LABEL2) + ")")),
                new SelectorTestCase("Other label", new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .withCharacteristic(LABELTYPE, LABEL3)
                        .create(), List.of(TRUE), List.of(TRUE)),
                new SelectorTestCase("Not source Label", new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .withoutCharacteristic(LABELTYPE, LABEL1)
                        .create(), List.of(TRUE), List.of(nodeLabel(SINK, LABEL1))),
                new SelectorTestCase("Not sink Label", new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .withoutCharacteristic(LABELTYPE, LABEL2)
                        .create(), List.of(nodeLabel(SOURCE, LABEL2)), List.of(TRUE)),
                new SelectorTestCase("Not other Label", new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .withoutCharacteristic(LABELTYPE, LABEL3)
                        .create(), List.of(nodeLabel(SOURCE, LABEL3)), List.of(nodeLabel(SINK, LABEL3))

                ));
    }

    static Stream<SelectorTestCase> vertexCharacteristicsSourceCases() {
        return Stream.of(new SelectorTestCase("Source Label", new ConstraintDSL().fromNode()
                .withCharacteristic(LABELTYPE, LABEL1)
                .neverFlows()
                .toVertex()
                .create(), List.of("(not " + nodeLabel(SOURCE, LABEL1) + ")"), List.of("(not " + nodeLabel(SOURCE, LABEL1) + ")")),
                new SelectorTestCase("Sink Label", new ConstraintDSL().fromNode()
                        .withCharacteristic(LABELTYPE, LABEL2)
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(TRUE), List.of("(not " + nodeLabel(SINK, LABEL2) + ")")),
                new SelectorTestCase("Other Label", new ConstraintDSL().fromNode()
                        .withCharacteristic(LABELTYPE, LABEL3)
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(TRUE), List.of(TRUE)),
                new SelectorTestCase("Not source Label", new ConstraintDSL().fromNode()
                        .withoutCharacteristic(LABELTYPE, LABEL1)
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(TRUE), List.of(nodeLabel(SINK, LABEL1))),
                new SelectorTestCase("Not sink Label", new ConstraintDSL().fromNode()
                        .withoutCharacteristic(LABELTYPE, LABEL2)
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(nodeLabel(SOURCE, LABEL2)), List.of(nodeLabel(SOURCE, LABEL2))),
                new SelectorTestCase("Not other Label", new ConstraintDSL().fromNode()
                        .withoutCharacteristic(LABELTYPE, LABEL3)
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(nodeLabel(SOURCE, LABEL3)),
                        List.of("(not (or (not " + nodeLabel(SINK, LABEL3) + ") (not " + nodeLabel(SOURCE, LABEL3) + ")))",
                                "(not (or (not " + nodeLabel(SOURCE, LABEL3) + ") (not " + nodeLabel(SINK, LABEL3) + ")))"))

        );
    }
}
