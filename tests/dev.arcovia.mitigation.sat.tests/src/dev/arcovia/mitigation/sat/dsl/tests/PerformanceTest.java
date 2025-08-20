package dev.arcovia.mitigation.sat.dsl.tests;

import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PerformanceTest {

    private final Logger logger = Logger.getLogger(PerformanceTest.class);

    private final static int[] inputs = {1,2};

    @Test
    public void performanceTest() {

        // set heap size of JVM to 16GB before running, then set true
        boolean heapMemorySetTo16Gb = false;

        assertTrue(true);

        if (!heapMemorySetTo16Gb) {
            return;
        }

        List<String> longList = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            longList.add(Integer.toString(i));
        }

        AnalysisConstraint constraint = new ConstraintDSL().ofData()
                .withLabel("DataLabel", longList)
                .neverFlows()
                .toVertex()
                .withCharacteristic("NodeLabel", longList)
                .create();

        var start = System.currentTimeMillis();
        var translation = new CNFTranslation(constraint);
        translation.constructCNF();
        var end = System.currentTimeMillis();
        var time = end - start;
        logger.info("\nTest finished in: " + time + " ms");
        logger.info(translation.getCNFStatistics());

        assertTrue(true);
    }

    @ParameterizedTest()
    @ValueSource(ints =  {500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000, 5500, 6000, 6500, 7000, 7500, 8000, 8500, 9000, 9500, 10000})
    public void performanceTest2(int input) {

        // set heap size of JVM to 16GB before running, then set true
        boolean heapMemorySetTo16Gb = false;

        assertTrue(true);

        if (!heapMemorySetTo16Gb) {
            return;
        }

        List<String> longList = new ArrayList<>();
        for (int i = 0; i < input; i++) {
            longList.add(Integer.toString(i));
        }

        AnalysisConstraint constraint = new ConstraintDSL().ofData()
                .withLabel("DataLabel", longList)
                .neverFlows()
                .toVertex()
                .withCharacteristic("NodeLabel", longList)
                .create();

        var start = System.currentTimeMillis();
        var translation = new CNFTranslation(constraint);
        translation.constructCNF();
        var end = System.currentTimeMillis();
        var time = end - start;
        logger.info("\n=== " + input + " ===");
        logger.info("\nTest finished in: " + time + " ms");
//        logger.info(translation.getCNFStatistics());

        assertTrue(true);
    }
}
