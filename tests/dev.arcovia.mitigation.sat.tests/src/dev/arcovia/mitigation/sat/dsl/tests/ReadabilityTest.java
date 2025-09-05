package dev.arcovia.mitigation.sat.dsl.tests;

import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import dev.arcovia.mitigation.sat.dsl.tests.utility.DataLoader;
import dev.arcovia.mitigation.sat.dsl.tests.utility.ReadabilityTestResult;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ReadabilityTest {

    private final Logger logger = Logger.getLogger(ReadabilityTest.class);

    private static final int testCount = 10000;
    private static final int bound = 100;
    // set heap size of JVM to 16GB before running, then set true
    private static final boolean heapMemorySetTo16Gb = true;

    @Test
    public void randomTest() throws IOException {

        if (!heapMemorySetTo16Gb) {
            return;
        }

        ReadabilityTestResult[] testResults =  new ReadabilityTestResult[testCount];

        for (int i = 0; i < testCount; i++) {

            Random random = new Random();
            int inputLiterals = random.nextInt(bound)+1;

            AnalysisConstraint constraint = getRandomNonWorstCaseConstraint(inputLiterals);
            var translation = new CNFTranslation(constraint);
            translation.constructCNF();

            var outputClauses = translation.outputClauses();
            var outputLiterals = translation.outputLiterals();
            var literalsPerClause = (float) (outputLiterals) / outputClauses;


            testResults[i] = new ReadabilityTestResult(
                    inputLiterals,
                    outputClauses,
                    outputLiterals,
                    translation.outputLongestClause(),
                    literalsPerClause
            );
        }

        DataLoader.outputTestResults(testResults, "readability.json");
    }

    private static AnalysisConstraint getRandomNonWorstCaseConstraint(int upperBound) {

        Random random = new Random();
        int splitIndex = random.nextInt(upperBound+1);

        List<String> data = new ArrayList<>();
        List<String> nodes = new ArrayList<>();

        for (int i = 0; i < splitIndex; i++) {
            data.add(Integer.toString(i));
        }

        for (int i = 0; i < upperBound-splitIndex; i++) {
            nodes.add(Integer.toString(i));
        }

        return new ConstraintDSL().ofData()
                .withLabel("DataLabel", data)
                .neverFlows()
                .toVertex()
                .withoutCharacteristic("NodeLabel", nodes)
                .create();
    }

    @Test
    public void randomWorstCaseTest() throws IOException {

        if (!heapMemorySetTo16Gb) {
            return;
        }

        ReadabilityTestResult[] testResults =  new ReadabilityTestResult[testCount];

        for (int i = 0; i < testCount; i++) {

            Random random = new Random();
            int inputLiterals = random.nextInt(bound)+1;

            AnalysisConstraint constraint = getRandomWorstCaseConstraint(inputLiterals);
            var translation = new CNFTranslation(constraint);
            translation.constructCNF();

            var outputClauses = translation.outputClauses();
            var outputLiterals = translation.outputLiterals();
            var literalsPerClause = (float) (outputLiterals) / outputClauses;


            testResults[i] = new ReadabilityTestResult(
                    inputLiterals,
                    outputClauses,
                    outputLiterals,
                    translation.outputLongestClause(),
                    literalsPerClause
            );
        }

        DataLoader.outputTestResults(testResults, "readability.json");
    }

    private static AnalysisConstraint getRandomWorstCaseConstraint(int upperBound) {

        Random random = new Random();
        int splitLeft = random.nextInt(upperBound+1);
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
        int splitLeft = random.nextInt(upperBound+1);
        int splitRight = upperBound - splitLeft;
        int splitData = random.nextInt(splitLeft+1);
        int splitNodes =  random.nextInt(splitRight+1);

        List<String> dataPos = new ArrayList<>();
        List<String> dataNeg = new ArrayList<>();
        List<String> nodesPos = new ArrayList<>();
        List<String> nodesNeg = new ArrayList<>();

        for (int i = 0; i < splitData; i++) {
            dataPos.add(Integer.toString(i));
        }

        for (int i = 0; i < splitLeft-splitData; i++) {
            dataNeg.add(Integer.toString(i));
        }

        for (int i = 0; i < splitNodes; i++) {
            nodesPos.add(Integer.toString(i));
        }

        for (int i = 0; i < splitRight-splitNodes; i++) {
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