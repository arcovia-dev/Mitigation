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
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StructureTest {

    private final Logger logger = Logger.getLogger(StructureTest.class);

    private static final int testCount = 10000;
    private static final int bound = 100;

    // 20.000 input literals need roughly 16 GB of heap size memory
    private static final long expectedMemoryInGigabyte = 16;

    /*@BeforeEach
    void beforeEach() {
        assertEquals(expectedMemoryInGigabyte * 1024 * 1024 * 1024, Runtime.getRuntime()
                .maxMemory(), "Incorrect JVM heap size");
    }*/
    
    @Test
    public void fullTest() throws IOException {
        int n = 100;

        List<StructureResult> testResults = new ArrayList<>();
        
        for (int c1 = 0; c1 <= n; c1++) {
            for (int c2 = c1; c2 <= n; c2++) {
                for (int c3 = c2; c3 <= n; c3++) {
                    AnalysisConstraint constraint = getFullConstraint(c1, c2, c3, n);               
                    
                    var timeStart = System.currentTimeMillis();
                    var translation = new CNFTranslation(constraint);
                    translation.constructCNF();
                    var timeEnd = System.currentTimeMillis();
                    var time = timeEnd - timeStart;

                    int dataPos = c1-0;
                    int dataNeg = c2-c1;
                    int nodePos = c3-c2;
                    int nodeNeg = n-c3;
                    
                    var outputClauses = translation.outputClauses();
                    var outputLiterals = translation.outputLiterals();
                    var literalsPerClause = outputLiterals / outputClauses;
                    
                    //Quadratic
                    assertEquals(Math.max(1, dataPos) * Math.max(1, nodePos), outputClauses);
                    //Cubic
                    assertEquals(Math.max(1, dataPos) * Math.max(1, nodePos) * (dataNeg + nodeNeg + (dataPos > 0 ? 1 : 0) + (nodePos > 0 ? 1 : 0)), outputLiterals);
                    //Linear
                    assertEquals((dataNeg + nodeNeg + (dataPos > 0 ? 1 : 0) + (nodePos > 0 ? 1 : 0)), literalsPerClause);
                    
                    testResults.add(new StructureResult(dataPos, dataNeg, nodePos, nodeNeg, n, outputClauses, outputLiterals, translation.outputLongestClause(),
                            literalsPerClause, Math.toIntExact(time)));
                    System.out.println(dataPos + " " + dataNeg + " " + nodePos + " " + nodeNeg);
                }
            }
        }
        DataLoader.outputStructureResults(testResults, "structure.json");
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
    }

    // This test is only to generate data for evaluating performance, it should be disabled in normal use
    @Disabled
    @Test
    public void randomTest() throws IOException {

        ReadabilityTestResult[] testResults = new ReadabilityTestResult[testCount];

        for (int i = 0; i < testCount; i++) {

            Random random = new Random();
            int inputLiterals = random.nextInt(bound) + 1;

            AnalysisConstraint constraint = getRandomNonWorstCaseConstraint(inputLiterals);
            var translation = new CNFTranslation(constraint);
            translation.constructCNF();

            var outputClauses = translation.outputClauses();
            var outputLiterals = translation.outputLiterals();
            var literalsPerClause = (float) (outputLiterals) / outputClauses;

            testResults[i] = new ReadabilityTestResult(inputLiterals, outputClauses, outputLiterals, translation.outputLongestClause(),
                    literalsPerClause);
        }

        DataLoader.outputTestResults(testResults, "readability.json");
    }

    private static AnalysisConstraint getRandomNonWorstCaseConstraint(int upperBound) {

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
    @Disabled
    @Test
    public void randomWorstCaseTest() throws IOException {

        ReadabilityTestResult[] testResults = new ReadabilityTestResult[testCount];

        for (int i = 0; i < testCount; i++) {

            Random random = new Random();
            int inputLiterals = random.nextInt(bound) + 1;

            AnalysisConstraint constraint = getRandomWorstCaseConstraint(inputLiterals);
            var translation = new CNFTranslation(constraint);
            translation.constructCNF();

            var outputClauses = translation.outputClauses();
            var outputLiterals = translation.outputLiterals();
            var literalsPerClause = (float) (outputLiterals) / outputClauses;

            testResults[i] = new ReadabilityTestResult(inputLiterals, outputClauses, outputLiterals, translation.outputLongestClause(),
                    literalsPerClause);
        }

        DataLoader.outputTestResults(testResults, "readability.json");
    }

    private static AnalysisConstraint getRandomWorstCaseConstraint(int upperBound) {

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

        for (int i = 0; i < splitLeft - splitData; i++) {
            dataNeg.add(Integer.toString(i));
        }

        for (int i = 0; i < splitNodes; i++) {
            nodesPos.add(Integer.toString(i));
        }

        for (int i = 0; i < splitRight - splitNodes; i++) {
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