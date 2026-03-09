package dev.arcovia.mitigation.smt.tests.constraints;

import java.util.List;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;

public class VertexCharacteristicsListTest extends AbstractSelectorConstraintTest {

    @Override
    protected Stream<SelectorTestCase> cases() {
        return Stream.concat(vertexCharacteristicsListDestinationCases(), vertexCharacteristicsListSourceCasesIfNonRecursive());
    }

    static Stream<SelectorTestCase> vertexCharacteristicsListDestinationCases() {
        return Stream.of(new SelectorTestCase("Source Label", new ConstraintDSL().fromNode()
                .neverFlows()
                .toVertex()
                .withCharacteristic(LABELTYPE, List.of(LABEL1))
                .create(), List.of("(not " + nodeLabel(SOURCE, LABEL1) + ")"), List.of(TRUE)),
                new SelectorTestCase("Sink needs to remove label", new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .withCharacteristic(LABELTYPE, List.of(LABEL2))
                        .create(), List.of(TRUE), List.of("(not " + nodeLabel(SINK, LABEL2) + ")")),
                new SelectorTestCase("Sink needs to remove label 2", new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .withCharacteristic(LABELTYPE, List.of(LABEL3, LABEL2))
                        .create(), List.of(TRUE), List.of("(not " + nodeLabel(SINK, LABEL2) + ")")),
                new SelectorTestCase("Both need to remove their label", new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .withCharacteristic(LABELTYPE, List.of(LABEL1, LABEL2))
                        .create(), List.of("(not " + nodeLabel(SOURCE, LABEL1) + ")"), List.of("(not " + nodeLabel(SINK, LABEL2) + ")")),
                new SelectorTestCase("Empty list always satisfied for normal selector", new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .withCharacteristic(LABELTYPE, List.of())
                        .create(), List.of(TRUE), List.of(TRUE)),
                new SelectorTestCase("Both need to add the label", new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .withoutCharacteristic(LABELTYPE, List.of(LABEL3))
                        .create(), List.of(nodeLabel(SOURCE, LABEL3)), List.of(nodeLabel(SINK, LABEL3))),
                new SelectorTestCase("Empty list never satisfied for inverted", new ConstraintDSL().fromNode()
                        .neverFlows()
                        .toVertex()
                        .withoutCharacteristic(LABELTYPE, List.of())
                        .create(), List.of(FALSE), List.of(FALSE)));
    }

    /**
     * Contains Test cases if Vertex Characteristics List should not act recursive as a source selector
     * @return test cases
     */
    private static Stream<SelectorTestCase> vertexCharacteristicsListSourceCasesIfNonRecursive() {
        return Stream.of(new SelectorTestCase("Source Label", new ConstraintDSL().fromNode()
                .withCharacteristic(LABELTYPE, List.of(LABEL1))
                .neverFlows()
                .toVertex()
                .create(), List.of("(not " + nodeLabel(SOURCE, LABEL1) + ")"), List.of(TRUE)),
                new SelectorTestCase("Sink needs to remove label", new ConstraintDSL().fromNode()
                        .withCharacteristic(LABELTYPE, List.of(LABEL2))
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(TRUE), List.of("(not " + nodeLabel(SINK, LABEL2) + ")")),
                new SelectorTestCase("Sink needs to remove label 2", new ConstraintDSL().fromNode()
                        .withCharacteristic(LABELTYPE, List.of(LABEL3, LABEL2))
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(TRUE), List.of("(not " + nodeLabel(SINK, LABEL2) + ")")),
                new SelectorTestCase("Both need to remove their label", new ConstraintDSL().fromNode()
                        .withCharacteristic(LABELTYPE, List.of(LABEL1, LABEL2))
                        .neverFlows()
                        .toVertex()
                        .create(), List.of("(not " + nodeLabel(SOURCE, LABEL1) + ")"), List.of("(not " + nodeLabel(SINK, LABEL2) + ")")),
                new SelectorTestCase("Empty list always satisfied for normal selector", new ConstraintDSL().fromNode()
                        .withCharacteristic(LABELTYPE, List.of())
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(TRUE), List.of(TRUE)),
                new SelectorTestCase("Both need to add the label", new ConstraintDSL().fromNode()
                        .withoutCharacteristic(LABELTYPE, List.of(LABEL3))
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(nodeLabel(SOURCE, LABEL3)), List.of(nodeLabel(SINK, LABEL3))),
                new SelectorTestCase("Empty list never satisfied for inverted", new ConstraintDSL().fromNode()
                        .withoutCharacteristic(LABELTYPE, List.of())
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(FALSE), List.of(FALSE)));
    }

    /**
     * Contains Test cases Vertex Characteristics List should act recursive as a source selector. Not used.
     * @return test cases
     */
    static Stream<SelectorTestCase> vertexCharacteristicsListSourceCasesIfRecursive() {
        return Stream.of(new SelectorTestCase("Source needs to remove label", new ConstraintDSL().fromNode()
                .withCharacteristic(LABELTYPE, List.of(LABEL1))
                .neverFlows()
                .toVertex()
                .create(), List.of("(not " + nodeLabel(SOURCE, LABEL1) + ")"), List.of("(not " + nodeLabel(SOURCE, LABEL1) + ")")),
                new SelectorTestCase("Sink needs to remove label", new ConstraintDSL().fromNode()
                        .withCharacteristic(LABELTYPE, List.of(LABEL2))
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(TRUE), List.of("(not " + nodeLabel(SINK, LABEL2) + ")")),
                new SelectorTestCase("Sink needs to remove label 2", new ConstraintDSL().fromNode()
                        .withCharacteristic(LABELTYPE, List.of(LABEL3, LABEL2))
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(TRUE), List.of("(not " + nodeLabel(SINK, LABEL2) + ")")),
                new SelectorTestCase("Both need to remove their label", new ConstraintDSL().fromNode()
                        .withCharacteristic(LABELTYPE, List.of(LABEL1, LABEL2))
                        .neverFlows()
                        .toVertex()
                        .create(), List.of("(not " + nodeLabel(SOURCE, LABEL1) + ")"),
                        List.of("(not (or " + nodeLabel(SINK, LABEL2) + " " + nodeLabel(SOURCE, LABEL1) + "))",
                                "(not (or " + nodeLabel(SOURCE, LABEL1) + " " + nodeLabel(SINK, LABEL2) + "))")),
                new SelectorTestCase("Empty list always satisfied for normal selector", new ConstraintDSL().fromNode()
                        .withCharacteristic(LABELTYPE, List.of())
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(TRUE), List.of(TRUE)),
                new SelectorTestCase("Both need to add the label", new ConstraintDSL().fromNode()
                        .withoutCharacteristic(LABELTYPE, List.of(LABEL3))
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(nodeLabel(SOURCE, LABEL3)),
                        List.of("(not (or (not " + nodeLabel(SINK, LABEL3) + ") (not " + nodeLabel(SOURCE, LABEL3) + ")))",
                                "(not (or (not " + nodeLabel(SOURCE, LABEL3) + ") (not " + nodeLabel(SINK, LABEL3) + ")))")),
                new SelectorTestCase("Empty list never satisfied for inverted", new ConstraintDSL().fromNode()
                        .withoutCharacteristic(LABELTYPE, List.of())
                        .neverFlows()
                        .toVertex()
                        .create(), List.of(FALSE), List.of(FALSE)));
    }

}
