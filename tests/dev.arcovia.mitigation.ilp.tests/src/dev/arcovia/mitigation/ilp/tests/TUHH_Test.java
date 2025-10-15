package dev.arcovia.mitigation.ilp.tests;

import java.util.List;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DFD2WebConverter;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.Activator;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.ilp.OptimizationManager;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.ArrayList;

public class TUHH_Test {
    final List<AnalysisConstraint> analysisConstraints = List.of(new ConstraintDSL().ofData()
            .withLabel("Stereotype", "entrypoint")
            .withoutLabel("Stereotype", "gateway")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .create(),
            
            new ConstraintDSL().ofData()
                    .withoutLabel("Stereotype", "authenticated_request")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "internal")
                    .create(),
            new ConstraintDSL().ofData()
                    .withLabel("Stereotype", "entrypoint")
                    .withoutLabel("Stereotype", "transform_identity_representation")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "internal")
                    .create(),
            new ConstraintDSL().ofData()
                    .withLabel("Stereotype", "entrypoint")
                    .withoutLabel("Stereotype", "token_validation")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "internal")
                    .create(),
            new ConstraintDSL().ofData()
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "authorization_server")
                    .withoutCharacteristic("Stereotype", "login_attempts_regulation")
                    .create(),
            new ConstraintDSL().ofData()
                    .withLabel("Stereotype", "entrypoint")
                    .withoutLabel("Stereotype", "encrypted_connection")
                    .neverFlows()
                    .toVertex()
                    .create(),
            new ConstraintDSL().ofData()
                    .withLabel("Stereotype", "internal")
                    .withoutLabel("Stereotype", "encrypted_connection")
                    .neverFlows()
                    .toVertex()
                    .create(),
            new ConstraintDSL().ofData()
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "internal")
                    .withoutCharacteristic("Stereotype", "local_logging")
                    .create(),
            new ConstraintDSL().ofData()
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "local_logging")
                    .withoutCharacteristic("Stereotype", "log_sanitization")
                    .create()

    );
    
    @Test
    public void main() throws StandaloneInitializationException {
        var tuhhModels = TuhhModels.getTuhhModels();
        
        List<Constraint> constraints = new ArrayList<>();
        for (var constraint : analysisConstraints) {
            constraints.add(new Constraint(constraint));
        }
        
        for (var model : tuhhModels.keySet()) {
            for (int variant : tuhhModels.get(model)) {
                String name = model + "_" + variant;

                System.out.println(name);
                
                DataFlowDiagramAndDictionary dfd = loadDFD(model, name);
                
                var optimization = new OptimizationManager(dfd, constraints);
                
                var result = optimization.repair();
                
                var dfdConverter = new DFD2WebConverter();
                dfdConverter.convert(result).save("models/" ,"temp-repaired.json");
                
                assertTrue(optimization.isViolationFree(result, constraints));
            }
        }
    }
    
    @Disabled
    @Test
    public void runSpecific() throws StandaloneInitializationException {
        String model = "jferrater";
        int variant = 18;
        String name = model + "_" + variant;
        
        List<Constraint> constraints = new ArrayList<>();
        for (var constraint : analysisConstraints) {
            constraints.add(new Constraint(constraint));
        }
        
        DataFlowDiagramAndDictionary dfd = loadDFD(model, name);
        
        var optimization = new OptimizationManager(dfd, constraints);
        
        var result = optimization.repair();
        
        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(result).save("models/" ,"temp-repaired.json");
        
        assertTrue(optimization.isViolationFree(result, constraints));
    }
    
    private DataFlowDiagramAndDictionary loadDFD(String model, String name) throws StandaloneInitializationException {
        final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
        final String location = Paths.get("scenarios", "dfd", "TUHH-Models")
                .toString();
        var dfd = new DataFlowDiagramAndDictionary(PROJECT_NAME, Paths.get(location, model, (name + ".dataflowdiagram"))
                .toString(),
                Paths.get(location, model, (name + ".datadictionary"))
                        .toString(),
                Activator.class);
        return dfd;
    }
}
