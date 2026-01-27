package dev.arcovia.mitigation.ilp.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.ilp.OptimizationManager;
import dev.arcovia.mitigation.sat.DFDScaler;
import dev.arcovia.mitigation.ilp.timeMeasurement;

public class PerformanceEval {
    Path current = Paths.get(System.getProperty("user.dir"));

    /*private final String MinDFD = current.getParent()
            .resolve("dev.arcovia.mitigation.sat.tests")
            .resolve("models")
            .resolve("minsat.json")
            .toString();*/
    
    private final String MinDFD = "models/sourceSink.json";
    
    AnalysisConstraint constraint = new ConstraintDSL().ofData()
            .withLabel("Sensitivity", "Personal")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Location", "nonEU")
            .create();
    
    
    @Test
    public void scaleTFGLength() {
        List<Integer>  scalings = List.of(0,0,1,2,4,8,16,32,64,128,256,512);
        List<Long>  results = new ArrayList<>();
        
        for (var scaling : scalings) {
            timeMeasurement timer = new timeMeasurement();
            
            var scaler = new DFDScaler(MinDFD);
            var scaledDFD = scaler.scaleTFGLength(scaling);
            
            var optimization = new OptimizationManager(scaledDFD, List.of(constraint));
            var dfd = optimization.repair(timer);
            
            results.add(timer.getSolvingTime());
            
            assertTrue(optimization.isViolationFree(dfd));
            
        }
        System.out.println(results);
    }
    
    @Test
    public void scaleTFGAmount() {
        List<Integer>  scalings = List.of(0,0,1,2,4,8,16,32,64,128,256,512,1024,2024, 10000, 100000);
        List<Long>  results = new ArrayList<>();
        
        for (var scaling : scalings) {
            timeMeasurement timer = new timeMeasurement();
            
            var scaler = new DFDScaler(MinDFD);
            var scaledDFD = scaler.scaleTFGAmount(scaling);
            
            var optimization = new OptimizationManager(scaledDFD, List.of(constraint));
            var dfd = optimization.repair(timer);
            
            results.add(timer.getSolvingTime());
            
            assertTrue(optimization.isViolationFree(dfd));        
            
        }
        System.out.println(results);
    }

}
