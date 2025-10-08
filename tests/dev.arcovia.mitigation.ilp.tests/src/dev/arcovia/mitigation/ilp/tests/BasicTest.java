package dev.arcovia.mitigation.ilp.tests;

import java.io.IOException;
import java.util.List;

import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;

import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.ilp.MitigationType;
import dev.arcovia.mitigation.ilp.MitigationStrategy;
import dev.arcovia.mitigation.ilp.OptimizationManager;
import dev.arcovia.mitigation.sat.IncomingDataLabel;
import dev.arcovia.mitigation.sat.Label;

public class BasicTest {
    private final String MinDFD = "models/mindfd.json";
    Constraint constraint = new Constraint( new ConstraintDSL().ofData()
            .withLabel("Sensitivity", "Personal")
            .withoutLabel("Encryption", "Encrypted")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Location", "nonEU")
            .create(), List.of(
                    new MitigationStrategy(new IncomingDataLabel(new Label("Encryption", "Encrypted")), 3, MitigationType.Data))
            );
    
    @Test
    public void minTest() throws IOException {
        var optimization = new OptimizationManager(MinDFD, List.of(constraint));
        
        var result = optimization.repair();
    }
    
}
