package dev.arcovia.mitigation.ilp.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DFD2WebConverter;
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
            .create());
    
    @Test
    public void minTest(){
        var optimization = new OptimizationManager(MinDFD, List.of(constraint));
        
        var result = optimization.repair();
        
        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(result).save("models/" ,"mindfd-repaired.json");
        
        assertTrue(optimization.isViolationFree(result, List.of(constraint)));
    }
    
}
