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

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReadabilityTest {

    private final Logger logger = Logger.getLogger(ReadabilityTest.class);

    private static final int testCount = 10000;
    private static final int bound = 1000;

    @Test
    public void randomTest() throws IOException {

        // set heap size of JVM to 16GB before running, then set true
        boolean heapMemorySetTo16Gb = false;
        assertTrue(true);
        if (!heapMemorySetTo16Gb) {
            return;
        }

        ReadabilityTestResult[] testResults =  new ReadabilityTestResult[testCount];

        for (int i = 0; i < testCount; i++) {

//            Random random = new Random();
//            int prob = random.nextInt(bound);
//            int inputLiterals = (int) ((1-Math.pow((double)(prob)/bound ,2))*bound) +1;

            Random random = new Random();
            int inputLiterals = random.nextInt(bound) +1;

            AnalysisConstraint constraint = getRandomConstraint(inputLiterals);
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

    private static AnalysisConstraint getRandomConstraint(int upperBound) {

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
}
