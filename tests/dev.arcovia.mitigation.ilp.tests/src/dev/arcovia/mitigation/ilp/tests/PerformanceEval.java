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
import dev.arcovia.mitigation.sat.Scaler;
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
        List<Long>  resultsSolving = new ArrayList<>();
        List<Long>  resultsExecution = new ArrayList<>();
        List<Long>  resultsIsolatedExec = new ArrayList<>();
        
        for (var scaling : scalings) {
            timeMeasurement timer = new timeMeasurement();
            
            var scaler = new Scaler(MinDFD);
            var scaledDFD = scaler.scaleTFGLength(scaling);
            
            var optimization = new OptimizationManager(scaledDFD, List.of(constraint));
            var dfd = optimization.repair(timer);
            
            resultsSolving.add(timer.getSolvingTime());
            resultsExecution.add(timer.getExecutionTime());
            resultsIsolatedExec.add(timer.getIsolatedExecution());
            
            assertTrue(optimization.isViolationFree(dfd));
            
        }
        System.out.println(resultsSolving);
        System.out.println(resultsExecution);
        System.out.println(resultsIsolatedExec);
    }
    
    @Test
    public void scaleTFGAmount() {
        List<Integer>  scalings = List.of(0,0,1,2,4,8,16,32,64,128,256,512,1024,2024, 10000);
        // results [860, 2, 2, 2, 1, 3, 2, 2, 3, 4, 5, 7, 5, 13, 63, 3692]
        List<Long>  resultsSolving = new ArrayList<>();
        List<Long>  resultsExecution = new ArrayList<>();
        List<Long>  resultsIsolatedExec = new ArrayList<>();
        
        for (var scaling : scalings) {
            timeMeasurement timer = new timeMeasurement();
            
            var scaler = new Scaler(MinDFD);
            var scaledDFD = scaler.scaleTFGAmount(scaling);
            
            var optimization = new OptimizationManager(scaledDFD, List.of(constraint));
            var dfd = optimization.repair(timer);
            
            resultsSolving.add(timer.getSolvingTime());
            resultsExecution.add(timer.getExecutionTime());
            resultsIsolatedExec.add(timer.getIsolatedExecution());
            
            assertTrue(optimization.isViolationFree(dfd));        
            
        }
        System.out.println(resultsSolving);
        System.out.println(resultsExecution);
        System.out.println(resultsIsolatedExec);
    }
    
    @Test
    public void scaleConstraintLabel() {
        
        
        int numberDummyLabels = 400;
        List<Integer>  scalings = List.of(1,10, 50, 100);
        
        List<ScalingResults> times = new ArrayList<>();
        
        for (int amountConstraint : scalings) {
            timeMeasurement timer = new timeMeasurement();
            var scaler = new Scaler(MinDFD);
            var scaledDFD = scaler.scaleLabels(numberDummyLabels);
            
            
            var constraints = scaler.scaleConstraint(amountConstraint, 1, 1
                    , 1, 1, numberDummyLabels);        
            var optimization = new OptimizationManager(scaledDFD, constraints);
            var dfd = optimization.repair(timer);
           
            
            assertTrue(optimization.isViolationFree(dfd));                     
            
            times.add(new ScalingResults(amountConstraint, 1, 1, 1, 1,timer.getExecutionTime(), timer.getSolvingTime(), timer.getIsolatedExecution()));
        }
        
        for (int numberWithLabel : scalings) {
            timeMeasurement timer = new timeMeasurement();
            var scaler = new Scaler(MinDFD);
            var scaledDFD = scaler.scaleLabels(numberDummyLabels);
            
            
            var constraints = scaler.scaleConstraint(1, numberWithLabel, 1
                    , 1, 1, numberDummyLabels);        
            var optimization = new OptimizationManager(scaledDFD, constraints);
            var dfd = optimization.repair(timer);
           
            
            assertTrue(optimization.isViolationFree(dfd));                     
            
            times.add(new ScalingResults(1, numberWithLabel, 1
                    , 1, 1,
                    timer.getExecutionTime(), timer.getSolvingTime(), timer.getIsolatedExecution()));   
        }
        
        for (int numberWithoutLabel : scalings) {
            timeMeasurement timer = new timeMeasurement();
            var scaler = new Scaler(MinDFD);
            var scaledDFD = scaler.scaleLabels(numberDummyLabels);
            
            
            var constraints = scaler.scaleConstraint(1, 1, numberWithoutLabel
                    , 1, 1, numberDummyLabels);        
            var optimization = new OptimizationManager(scaledDFD, constraints);
            var dfd = optimization.repair(timer);
           
            
            assertTrue(optimization.isViolationFree(dfd));                     
            
            times.add(new ScalingResults(1, 1, numberWithoutLabel
                    , 1, 1,
                    timer.getExecutionTime(), timer.getSolvingTime(), timer.getIsolatedExecution()));
        }
        
        for (int numberWithCharacteristic : scalings) {
            timeMeasurement timer = new timeMeasurement();
            var scaler = new Scaler(MinDFD);
            var scaledDFD = scaler.scaleLabels(numberDummyLabels);
            
            
            var constraints = scaler.scaleConstraint(1, 1, 1
                    , numberWithCharacteristic, 1, numberDummyLabels);        
            var optimization = new OptimizationManager(scaledDFD, constraints);
            var dfd = optimization.repair(timer);
           
            
            assertTrue(optimization.isViolationFree(dfd));                     
            
            times.add(new ScalingResults(1, 1, 1
                    , numberWithCharacteristic, 1,
                    timer.getExecutionTime(), timer.getSolvingTime(), timer.getIsolatedExecution()));
        }
        
        for (int numberWithoutCharacteristic : scalings) {
            timeMeasurement timer = new timeMeasurement();
            var scaler = new Scaler(MinDFD);
            var scaledDFD = scaler.scaleLabels(numberDummyLabels);
            
            
            var constraints = scaler.scaleConstraint(1, 1, 1
                    , 1, numberWithoutCharacteristic, numberDummyLabels);        
            var optimization = new OptimizationManager(scaledDFD, constraints);
            var dfd = optimization.repair(timer);
           
            
            assertTrue(optimization.isViolationFree(dfd));                     
            
            times.add(new ScalingResults(1, 1, 1
                    , 1, numberWithoutCharacteristic,
                    timer.getExecutionTime(), timer.getSolvingTime(), timer.getIsolatedExecution()));
        }
        
        
        for (int scaling : scalings) {
            timeMeasurement timer = new timeMeasurement();
            var scaler = new Scaler(MinDFD);
            var scaledDFD = scaler.scaleLabels(numberDummyLabels);
            
            
            var constraints = scaler.scaleConstraint(scaling, scaling/2, scaling/2
                    , scaling/2, scaling/2, numberDummyLabels);        
            var optimization = new OptimizationManager(scaledDFD, constraints);
            var dfd = optimization.repair(timer);
           
            
            assertTrue(optimization.isViolationFree(dfd));                     
            
            times.add(new ScalingResults(scaling, scaling/2, scaling/2
                    , scaling/2, scaling/2,
                    timer.getExecutionTime(), timer.getSolvingTime(), timer.getIsolatedExecution()));
        }
                
        
        System.out.println(times);
        
    }
    
    
        private record ScalingResults(int amountConstraint, int numberWithLabel, int numberWithoutLabel
                , int numberWithCharacteristic, int numberWithoutCharacteristic, long executionTime, long solvingTime, long IsolatedExecution) {
    }

}
