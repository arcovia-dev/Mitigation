package dev.arcovia.mitigation.sat.dsl.tests;

import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import dev.arcovia.mitigation.sat.dsl.tests.utility.DataLoader;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PerformanceTest {

    private final Logger logger = Logger.getLogger(PerformanceTest.class);

    // 20.000 input literals need roughly 16 GB of heap size memory
    private static final long expectedMemoryInGigabyte = 16;

    @BeforeEach
    void beforeEach() {
        assertEquals(expectedMemoryInGigabyte * 1024 * 1024 * 1024, Runtime.getRuntime()
                .maxMemory(), "Incorrect JVM heap size");
    }

    // This test is only to generate data for evaluating performance, it should be disabled in normal use
    @Disabled
    @Test
    public void singleInputPerformanceTest() {

        int inputLiteralsPerSide = 10000; // double that as total literals

        List<String> longList = new ArrayList<>();
        for (int i = 0; i < inputLiteralsPerSide; i++) {
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

    private static final int maxInputLiterals = 7000;
    private static final int start = 6000;
    private static final int step = 100;

    private static final int count = 1 + (maxInputLiterals - start) / step;
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
    public void multipleInputPerformanceTest(int input) {

        var literals = input * step + start;
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
        logger.info("\n " + literals + " Literals | " + time + " ms");
        outputLiterals[input] = literals;
        outputTime[input] = Math.toIntExact(time);
    }

    @AfterEach
    public void afterEach() throws IOException {
        DataLoader.outputJsonArray(outputLiterals, "literals.json");
        DataLoader.outputJsonArray(outputTime, "time.json");
    }
}
