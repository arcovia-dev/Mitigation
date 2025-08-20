package dev.arcovia.mitigation.sat.cnf.tests;

import dev.arcovia.mitigation.sat.cnf.CNFTranslation;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import dev.arcovia.mitigation.sat.cnf.tests.utility.DataLoader;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PerformanceTest {

    private final Logger logger = Logger.getLogger(PerformanceTest.class);
    private static DataFlowDiagramAndDictionary dfd;

    @BeforeAll
    public static void setup() throws StandaloneInitializationException {
        String model = "ewolff";
        int variant = 5;

        String name = model + "_" + variant;

        dfd = DataLoader.loadDFD(model, name);
    }

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
        var translation = new CNFTranslation(constraint, dfd);
        translation.constructCNF();
        var end = System.currentTimeMillis();
        var time = end - start;
        logger.info("Test finished in: " + time + " ms");
        logger.info(translation.getCNFStatistics());

        assertTrue(true);
    }
}
