package dev.arcovia.mitigation.ilp.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.analysis.dsl.result.DSLResult;
import org.dataflowanalysis.converter.dfd2web.DFD2WebConverter;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.ilp.MitigationStrategy;
import dev.arcovia.mitigation.ilp.MitigationType;
import dev.arcovia.mitigation.ilp.Node;
import dev.arcovia.mitigation.ilp.OptimizationManager;
import dev.arcovia.mitigation.sat.CompositeLabel;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.LabelCategory;
import dev.arcovia.mitigation.sat.NodeLabel;

public class BasicTest {
    Path current = Paths.get(System.getProperty("user.dir"));

    private final String MinDFD = current.getParent()
            .resolve("dev.arcovia.mitigation.sat.tests")
            .resolve("models")
            .resolve("minsat.json")
            .toString();

    AnalysisConstraint constraint = new ConstraintDSL().ofData()
            .withLabel("Sensitivity", "Personal")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Location", "nonEU")
            .create();

    @Test
    public void minTest() {
        var optimization = new OptimizationManager(MinDFD, List.of(constraint));

        var result = optimization.repair();

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(result)
                .save("models/", "mindfd-repaired.json");

        assertTrue(optimization.isViolationFree(result, List.of(constraint)));
    }
    
    @Test
    public void customConstraintTest() {
        var customConstraint = new Constraint(List.of(new MitigationStrategy(new NodeLabel(new Label("Location", "nonEU")), 1, MitigationType.DeleteNodeLabel)));
        
        customConstraint.addEvalFunction(flowGraph -> {
            Set<Node> violatingNodes = new HashSet<>();
            List<DSLResult> results = constraint.findViolations(flowGraph);
            for (var result : results) {
                var tfg = result.getTransposeFlowGraph();
                for (var vertex : result.getMatchedVertices())
                    violatingNodes.add(new Node((DFDVertex) vertex, tfg, customConstraint));
            }
            return violatingNodes;
        });
        
        var optimization = new OptimizationManager(MinDFD, List.of(customConstraint), false);

        var result = optimization.repair();

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(result)
                .save("models/", "mindfd-repaired.json");

        assertTrue(optimization.isViolationFree(result, List.of(constraint)));
    }

}
