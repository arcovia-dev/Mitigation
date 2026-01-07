package dev.arcovia.mitigation.ilp.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DFD2WebConverter;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.ilp.OptimizationManager;

import dev.arcovia.mitigation.ilp.Constraint;

public class ExampleModelTest {
    private final String Model = "models/examplemodel.json";
    
    private final Constraint localLogging = new Constraint(new ConstraintDSL().ofData()
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .withoutCharacteristic("Stereotype", "local_logging")
            .create());
    
    private final Constraint logSanitization = new Constraint(new ConstraintDSL().ofData()
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "local_logging")
            .withoutCharacteristic("Stereotype", "log_sanitization")
            .create());
    
    private final Constraint loggingServer = null;
    
    private final Constraint authServer = null;
    
    private final Constraint authenticated_request = new Constraint(new ConstraintDSL().ofData()
            .withoutLabel("Stereotype", "authenticated_request")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .create());
    
    private final Constraint personalLogging = new Constraint(new ConstraintDSL().ofData()
            .withLabel("Sensitivity", "Personal")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Logging", "Server")
            .create());
    
    private final Constraint personalStorageNonEu = new Constraint(new ConstraintDSL().ofData()
            .withLabel("Sensitivity", "Personal")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Location", "nonEU")
            .create());
    
    private final Constraint adminStorage = new Constraint(new ConstraintDSL().ofData()
            .withLabel("User", "admin")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "storage")
            .create());
    
    private final Constraint adminEncryption = new Constraint(new ConstraintDSL().ofData()
            .withLabel("User", "admin")
            .withoutLabel("Encryption", "encrypted")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .create());
    
    private final List<Constraint> Constraints = List.of(localLogging, logSanitization, loggingServer, authServer, authenticated_request,
            personalLogging, personalStorageNonEu, adminStorage, adminEncryption);
    
    
    @Test
    public void StandardTest(){
        var optimization = new OptimizationManager(Model, Constraints, false);

        var result = optimization.repair();
        
        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(result)
                .save("models/", "examplemodel-Standardrepair.json");
        
        assertTrue(optimization.isViolationFree(result));
        
        
    }

}
