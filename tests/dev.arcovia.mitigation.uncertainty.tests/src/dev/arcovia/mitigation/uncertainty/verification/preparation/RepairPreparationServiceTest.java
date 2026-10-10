package dev.arcovia.mitigation.uncertainty.verification.preparation;

import dev.arcovia.mitigation.uncertainty.preparation.RepairPreparationService;

import dev.arcovia.mitigation.cost.ActionType;
import dev.arcovia.mitigation.ilp.*;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.NodeLabel;
import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.web2dfd.Web2DFDConverter;
import org.dataflowanalysis.converter.web2dfd.WebEditorConverterModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RepairPreparationServiceTest {
    private final Path current = Path.of(System.getProperty("user.dir"));

    private final String minDfd = current.resolve("models")
            .resolve("minsat.json")
            .toString();

    @Test
    public void preparesMitigationsThatSolveTheSingleScenarioModel() throws Exception {
        var dfd = new Web2DFDConverter().convert(new WebEditorConverterModel(minDfd));
        var preparation = new RepairPreparationService().prepare(dfd, List.of(createDeleteNodeConstraint()));
        List<Mitigation> chosenMitigations = new ILPSolver().solve(preparation.mitigations(),
                new java.util.HashSet<>(preparation.allMitigations()),
                preparation.contradictions());

        assertEquals(1, preparation.preparedConstraints().size());
        assertFalse(preparation.violatingNodes().isEmpty());
        assertFalse(preparation.mitigations().isEmpty());
        assertFalse(preparation.allMitigations().isEmpty());
        assertNotNull(chosenMitigations);
        assertFalse(chosenMitigations.isEmpty());
        assertEquals(1, chosenMitigations.size());
        assertEquals(ActionType.RemoveNode, chosenMitigations.get(0).mitigation().type());
    }

    @Test
    public void rejectsCustomEvaluationFunctionConstraintsForRobustPreparation() {
        var customConstraint = new Constraint(
                List.of(new MitigationStrategy(List.of(new NodeLabel(new Label("Location", "nonEU"))), 1,
                        MitigationType.DeleteNodeLabel))
        );
        customConstraint.addEvalFunction(createCustomEvaluationFunction());

        var dfd = new Web2DFDConverter().convert(new WebEditorConverterModel(minDfd));

        var exception = assertThrows(UnsupportedOperationException.class,
                () -> new RepairPreparationService().prepare(dfd, List.of(customConstraint)));

        assertTrue(exception.getMessage().contains("DSL-backed constraints"));
        assertTrue(exception.getMessage().contains("out of scope"));
    }

    private Constraint createDeleteNodeConstraint() {
        AnalysisConstraint dsl = new ConstraintDSL().ofData()
                .withLabel("Sensitivity", "Personal")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Stereotype", "internal")
                .create();

        return new Constraint(dsl, List.of(
                new MitigationStrategy(List.of(new NodeLabel(new Label("Stereotype", "internal"))), 1,
                        MitigationType.DeleteNode)));
    }

    private EvaluationFunction createCustomEvaluationFunction() {
        return new EvaluationFunction() {
            @Override
            public Set<Node> evaluate(DFDFlowGraphCollection flowGraph) {
                throw new AssertionError("Custom evaluation must be rejected before evaluation");
            }

            @Override
            public boolean isMatched(DFDVertex node) {
                throw new AssertionError("Custom evaluation must be rejected before matching");
            }
        };
    }
}
