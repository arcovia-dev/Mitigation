package dev.arcovia.mitigation.ilp.tests;

import java.util.List;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;

import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.ilp.OptimizationManager;

public class BasicTest {
    private final String MinDFD = "models/mindfd.json";
    AnalysisConstraint constraint = new ConstraintDSL().ofData()
            .withLabel("Sensitivity", "Personal")
            .withoutLabel("Encryption", "Encrypted")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Location", "nonEU")
            .create();
    
    @Test
    public void minTest() {
        var optimization = new OptimizationManager(MinDFD, List.of(constraint));
        
        var result = optimization.repair();
    }
    
}
