package dev.arcovia.mitigation.sat.dsl.tests;

import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import dev.arcovia.mitigation.sat.dsl.tests.utility.DataLoader;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class PerformanceTest {

    // This test is only to generate data for evaluating performance, it should be disabled in normal use

    private final Logger logger = Logger.getLogger(PerformanceTest.class);

    // set heap size of JVM to 16GB before running, then set true
    private static final boolean heapMemorySetTo16Gb = false;

    @Disabled
    @Test
    public void performanceTest() {

        if (!heapMemorySetTo16Gb) {
            throw new IllegalStateException("Set heap size of JVM to 16GB before running, then set true.");
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
    }

    private static final int max = 7000; //7000
    private static final int start = 6000;
    private static final int step = 100;

    private static final int count = 1 + (max-start) / step;
    private static final int[] outputLiterals = new int[count];
    private static final int[] outputTime = new int[count];


    private static IntStream inputLiterals() {
        int[] inputs = new int[count];
        for (int i = 0; i < count; i++) {
            inputs[i] = i;
        }
        return Arrays.stream(inputs);
    }

    @Disabled
    @ParameterizedTest()
    @MethodSource("inputLiterals")
    public void performanceTest2(int input) {

        if (!heapMemorySetTo16Gb) {
            throw new IllegalStateException("Set heap size of JVM to 16GB before running, then set true.");
        }

        var literals = input*step+start;
        List<String> longList = new ArrayList<>();
        for (int i = 0; i < literals; i++) {
            longList.add(Integer.toString(i));
        }

        AnalysisConstraint constraint = new ConstraintDSL().ofData()
                .withLabel("DataLabel", longList)
                .neverFlows()
                .toVertex()
                .withCharacteristic("NodeLabel", longList)
                .create();

        var timeStart = System.currentTimeMillis();
        var translation = new CNFTranslation(constraint);
        translation.constructCNF();
        var timeEnd = System.currentTimeMillis();
        var time = timeEnd - timeStart;
        logger.info("\n " + literals + " Literals | " +  time + " ms");
        outputLiterals[input] = literals;
        outputTime[input] = Math.toIntExact(time);
    }

    @Disabled
    @AfterAll
    public static void afterAll() throws IOException {
        if (!heapMemorySetTo16Gb) {
            return;
        }
        DataLoader.outputJsonArray(outputLiterals, "literals.json");
        DataLoader.outputJsonArray(outputTime, "time.json");
    }
}
