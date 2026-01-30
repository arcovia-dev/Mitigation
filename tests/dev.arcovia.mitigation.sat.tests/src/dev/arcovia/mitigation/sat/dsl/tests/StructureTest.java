package dev.arcovia.mitigation.sat.dsl.tests;

import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import dev.arcovia.mitigation.sat.dsl.tests.utility.DataLoader;
import dev.arcovia.mitigation.sat.dsl.tests.utility.ReadabilityTestResult;
import dev.arcovia.mitigation.sat.dsl.tests.utility.StructureResult;

import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StructureTest {

    private final Logger logger = Logger.getLogger(StructureTest.class);

    private static final int testCount = 100000;
    private static final int bound = 1000;

    // 20.000 input literals need roughly 16 GB of heap size memory
    private static final long expectedMemoryInGigabyte = 16;

    /*@BeforeEach
    void beforeEach() {
        assertEquals(expectedMemoryInGigabyte * 1024 * 1024 * 1024, Runtime.getRuntime()
                .maxMemory(), "Incorrect JVM heap size");
    }*/

    // This test is only to generate data for evaluating performance, it should be disabled in normal use
    @Test
    public void randomLinearTest() throws IOException {

        ReadabilityTestResult[] testResults = new ReadabilityTestResult[testCount];

        for (int i = 0; i < testCount; i++) {

            Random random = new Random();
            int inputLiterals = random.nextInt(bound) + 1;

            AnalysisConstraint constraint = getRandomLinearConstraint(inputLiterals);
            var translation = new CNFTranslation(constraint);
            translation.constructCNF();

            var outputClauses = translation.outputClauses();
            var outputLiterals = translation.outputLiterals();
            var literalsPerClause = (float) (outputLiterals) / outputClauses;

            testResults[i] = new ReadabilityTestResult(inputLiterals, outputClauses, outputLiterals, translation.outputLongestClause(),
                    literalsPerClause);
        }

        DataLoader.outputTestResults(testResults, "readability_linear.json");
    }
    
    @Test
    public void fullTest() throws IOException {
        int n = 10;
        int faktor = 200;

        List<StructureResult> testResults = new ArrayList<>();
        
        for (int c1 = 0; c1 <= n; c1++) {
            for (int c2 = c1; c2 <= n; c2++) {
                for (int c3 = c2; c3 <= n; c3++) {
                    AnalysisConstraint constraint = getFullConstraint(c1*faktor,c2*faktor,c3*faktor,n*faktor);               
                    
                    var timeStart = System.currentTimeMillis();
                    var translation = new CNFTranslation(constraint);
                    translation.constructCNF();
                    var timeEnd = System.currentTimeMillis();
                    var time = timeEnd - timeStart;

                    var outputClauses = translation.outputClauses();
                    var outputLiterals = translation.outputLiterals();
                    var literalsPerClause = (float) (outputLiterals) / outputClauses;

                    testResults.add(new StructureResult(c1*faktor, c2*faktor, c3*faktor, n*faktor, outputClauses, outputLiterals, translation.outputLongestClause(),
                            literalsPerClause, Math.toIntExact(time)));
                    System.out.println(c1 + " " + c2 + " " + c3 + " " + n); 
                }
            }
        }
        DataLoader.outputTestResults(testResults, "structure_full.json");
        testResults.sort(Comparator.comparingInt(StructureResult::runtime));
        Map<String,Integer> configsByTime = new LinkedHashMap<>();
        for (var result : testResults) {
            configsByTime.put(result.cut1()+"_"+(result.cut2()-result.cut1())+"_"+
        (result.cut3()-result.cut2())+"_"+(result.inputLiterals()-result.cut3()),result.runtime());
        }
        System.out.println(configsByTime);
    }
    
    private static AnalysisConstraint getFullConstraint(int c1, int c2, int c3, int n) {
        List<String> dataPos = new ArrayList<>();
        List<String> dataNeg = new ArrayList<>();
        List<String> nodesPos = new ArrayList<>();
        List<String> nodesNeg = new ArrayList<>();

        for (int i = 0; i < c1; i++) {
            dataPos.add(Integer.toString(i));
        }

        for (int i = c1; i < c2; i++) {
            dataNeg.add(Integer.toString(i));
        }

        for (int i = c2; i < c3; i++) {
            nodesPos.add(Integer.toString(i));
        }

        for (int i = c3; i < n; i++) {
            nodesNeg.add(Integer.toString(i));
        }
        
        var data = new ConstraintDSL().ofData();
        
        if(!dataPos.isEmpty()) {
            data = data.withLabel("DataLabel", dataPos);

        }
        
        if(!dataNeg.isEmpty()) {
            data = data.withoutLabel("DataLabel", dataNeg);

        }
        
        var node = data.neverFlows().toVertex();
        
        if(!nodesPos.isEmpty()) {
            node = node.withCharacteristic("NodeLabel", nodesPos);

        }
        
        if(!nodesNeg.isEmpty()) {
            node = node.withoutCharacteristic("NodeLabel", nodesNeg);

        }
        
        return node.create();
        
        /*return new ConstraintDSL().ofData()
                .withLabel("DataLabel", dataPos)
                .withoutLabel("DataLabel", dataNeg)
                .neverFlows()
                .toVertex()
                .withCharacteristic("NodeLabel", nodesPos)
                .withoutCharacteristic("NodeLabel", nodesNeg)
                .create();*/
    }
    
    @Test
    public void randomTest() throws IOException {

        ReadabilityTestResult[] testResults = new ReadabilityTestResult[testCount];

        for (int i = 0; i < testCount; i++) {

            Random random = new Random();
            int inputLiterals = random.nextInt(bound) + 1;

            AnalysisConstraint constraint = getRandomConstraint(inputLiterals);
            var translation = new CNFTranslation(constraint);
            translation.constructCNF();

            var outputClauses = translation.outputClauses();
            var outputLiterals = translation.outputLiterals();
            var literalsPerClause = (float) (outputLiterals) / outputClauses;

            testResults[i] = new ReadabilityTestResult(inputLiterals, outputClauses, outputLiterals, translation.outputLongestClause(),
                    literalsPerClause);
            
            System.out.println(i);
        }

        DataLoader.outputTestResults(testResults, "readability_random.json");
    }

    private static AnalysisConstraint getRandomLinearConstraint(int upperBound) {

        Random random = new Random();
        int splitIndex = random.nextInt(upperBound + 1);

        List<String> data = new ArrayList<>();
        List<String> nodes = new ArrayList<>();

        for (int i = 0; i < splitIndex; i++) {
            data.add(Integer.toString(i));
        }

        for (int i = 0; i < upperBound - splitIndex; i++) {
            nodes.add(Integer.toString(i));
        }

        return new ConstraintDSL().ofData()
                .withLabel("DataLabel", data)
                .neverFlows()
                .toVertex()
                .withoutCharacteristic("NodeLabel", nodes)
                .create();
    }

    // This test is only to generate data for evaluating performance, it should be disabled in normal use
    @Test
    public void randomQuadraticTest() throws IOException {

        ReadabilityTestResult[] testResults = new ReadabilityTestResult[testCount];

        for (int i = 0; i < testCount; i++) {

            Random random = new Random();
            int inputLiterals = random.nextInt(bound) + 1;

            AnalysisConstraint constraint = getRandomQuadraticConstraint(inputLiterals);
            var translation = new CNFTranslation(constraint);
            translation.constructCNF();

            var outputClauses = translation.outputClauses();
            var outputLiterals = translation.outputLiterals();
            var literalsPerClause = (float) (outputLiterals) / outputClauses;

            testResults[i] = new ReadabilityTestResult(inputLiterals, outputClauses, outputLiterals, translation.outputLongestClause(),
                    literalsPerClause);
        }

        DataLoader.outputTestResults(testResults, "readability_quadratic.json");
    }

    private static AnalysisConstraint getRandomQuadraticConstraint(int upperBound) {

        Random random = new Random();
        int splitLeft = random.nextInt(upperBound + 1);
        int splitRight = upperBound - splitLeft;

        List<String> data = new ArrayList<>();
        List<String> nodes = new ArrayList<>();

        for (int i = 0; i < splitLeft; i++) {
            data.add(Integer.toString(i));
        }

        for (int i = 0; i < splitRight; i++) {
            nodes.add(Integer.toString(i));
        }

        return new ConstraintDSL().ofData()
                .withLabel("DataLabel", data)
                .neverFlows()
                .toVertex()
                .withCharacteristic("NodeLabel", nodes)
                .create();
    }

    private static AnalysisConstraint getRandomConstraint(int upperBound) {

        Random random = new Random();
        int splitLeft = random.nextInt(upperBound + 1);
        int splitRight = upperBound - splitLeft;
        int splitData = random.nextInt(splitLeft + 1);
        int splitNodes = random.nextInt(splitRight + 1);

        List<String> dataPos = new ArrayList<>();
        List<String> dataNeg = new ArrayList<>();
        List<String> nodesPos = new ArrayList<>();
        List<String> nodesNeg = new ArrayList<>();

        for (int i = 0; i < splitData; i++) {
            dataPos.add(Integer.toString(i));
        }

        for (int i = splitData; i < splitLeft; i++) {
            dataNeg.add(Integer.toString(i));
        }

        for (int i = 0; i < splitNodes; i++) {
            nodesPos.add(Integer.toString(i));
        }

        for (int i = splitNodes; i < splitRight; i++) {
            nodesNeg.add(Integer.toString(i));
        }
        
        return new ConstraintDSL().ofData()
                .withLabel("DataLabel", dataPos)
                .withoutLabel("DataLabel", dataNeg)
                .neverFlows()
                .toVertex()
                .withCharacteristic("NodeLabel", nodesPos)
                .withoutCharacteristic("NodeLabel", nodesNeg)
                .create();
    }
}