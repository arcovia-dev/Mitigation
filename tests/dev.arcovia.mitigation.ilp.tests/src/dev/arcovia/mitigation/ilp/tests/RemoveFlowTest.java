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
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.sat.OutgoingDataLabel;
import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.ilp.MitigationStrategy;
import dev.arcovia.mitigation.ilp.MitigationType;

public class RemoveFlowTest {




    Path current = Paths.get(System.getProperty("user.dir"));
    
    private final String MinDFD = current.getParent()
            .resolve("dev.arcovia.mitigation.sat.tests")
            .resolve("models")
            .resolve("minsat.json")
            .toString();
    
    
    
    
    AnalysisConstraint dsl = new ConstraintDSL().ofData()
            .withLabel("Sensitivity", "Personal")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Location", "nonEU")
            .create();
    
    Constraint constraint = new Constraint(dsl, List.of(new MitigationStrategy(List.of(new OutgoingDataLabel(new Label("Sensitivity","Personal"))), 1, MitigationType.DeleteFlow)));

    @Test
    public void test() {
        var optimization = new OptimizationManager(MinDFD, List.of(constraint), false);

        var result = optimization.repair();
                
        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(result)
                .save("models/", "DeletedFlow-repaired.json");
        
        assertTrue(optimization.isViolationFree(result));
    }

}
