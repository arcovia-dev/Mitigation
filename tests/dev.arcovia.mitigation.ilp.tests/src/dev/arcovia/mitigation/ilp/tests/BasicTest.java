package dev.arcovia.mitigation.ilp.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DFD2WebConverter;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.ilp.OptimizationManager;

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

}
