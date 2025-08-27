package dev.arcovia.mitigation.sat.dsl.tests;

import dev.arcovia.mitigation.sat.*;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import dev.arcovia.mitigation.sat.dsl.tests.utility.DataLoader;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.core.AbstractTransposeFlowGraph;
import org.dataflowanalysis.examplemodels.results.ExpectedCharacteristic;
import org.dataflowanalysis.examplemodels.results.ExpectedViolation;
import org.dataflowanalysis.examplemodels.results.dfd.DFDExampleModelResult;
import org.dataflowanalysis.examplemodels.results.dfd.scenarios.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class VerificationTest {

    private final Logger logger = Logger.getLogger(VerificationTest.class);

    private static Stream<Arguments> provideDFDExampleModelViolations() {
        return Stream.of(
                Arguments.of(new OnlineShopResult()), Arguments.of(new SimpleOnlineShopResult()),
                Arguments.of(new CWANoViolation()), Arguments.of(new VWCariad()), Arguments.of(new CWAPersonalDataViolation()),
                Arguments.of(new CWARPIViolation()));
    }

    @ParameterizedTest
    @MethodSource("provideDFDExampleModelViolations")
    public void validate(DFDExampleModelResult exampleModelResult) throws StandaloneInitializationException {

        var name = exampleModelResult.getModelName();
        var dfd = DataLoader.loadDFDfromPath(exampleModelResult.getDataFlowDiagram(), exampleModelResult.getDataDictionary());
        var analysisConstraints = exampleModelResult.getDSLConstraints();
        var constraints = analysisConstraints.stream().map(it -> new CNFTranslation(it, dfd))
                .map(CNFTranslation::constructCNF)
                .flatMap(Collection::stream).toList();


        logger.info("Model Name: " + name);
        logger.info("Constraints: " + constraints.toString());

        Mechanic mechanic = new Mechanic(dfd, name, constraints);
        List<AbstractTransposeFlowGraph> violatingTFGs = mechanic.getViolatingTFGs(dfd, constraints);

        shouldReturnCorrectViolations(violatingTFGs, exampleModelResult);
    }

    private void shouldReturnCorrectViolations(List<AbstractTransposeFlowGraph> violatingTFGs, DFDExampleModelResult exampleModelResult) {
        Assumptions.assumeTrue(!exampleModelResult.getDSLConstraints()
                .isEmpty(), "Example Model does not define any constraints!");

        if (exampleModelResult.getExpectedViolations().isEmpty() && !violatingTFGs.isEmpty()) {
            logger.error("Offending violations:" + violatingTFGs);
            fail("Analysis found violating vertices, but none were expected");
        }

        for (ExpectedViolation expectedViolation : exampleModelResult.getExpectedViolations()) {
            var violatingVertices = violatingTFGs.stream()
                    .map(AbstractTransposeFlowGraph::getVertices)
                    .flatMap(Collection::stream)
                    .filter(it -> expectedViolation.getIdentifier().matches(it))
                    .toList();

            if (violatingVertices.isEmpty()) {
                logger.error(String.format("Could not find vertex with id: %s", expectedViolation.getIdentifier()));
                logger.error(String.format("Number of violating TFGs: %s", violatingTFGs.size()));
                fail(String.format("Could not find vertex with id: %s", expectedViolation.getIdentifier()));
            }

            int numberOfViolatingVertices = 0;

            for (var violatingVertex : violatingVertices) {
                logger.info("Evaluating vertex: " + expectedViolation.getIdentifier());

                List<ExpectedCharacteristic> missingNodeCharacteristics = expectedViolation.hasNodeCharacteristic(violatingVertex
                        .getAllVertexCharacteristics());
                if (!missingNodeCharacteristics.isEmpty()) {
                    logger.warn(String.format("Skipped: Vertex %s is missing the following node characteristics: %s", violatingVertex,
                            missingNodeCharacteristics));
                    continue;
                }

                var incorrectNodeCharacteristics = expectedViolation.hasIncorrectNodeCharacteristics(violatingVertex
                        .getAllVertexCharacteristics());
                if (!incorrectNodeCharacteristics.isEmpty()) {
                    logger.warn(String.format("Skipped: Vertex %s has the following incorrect node characteristics: %s", violatingVertex,
                            incorrectNodeCharacteristics));
                    continue;
                }

                Map<String, List<ExpectedCharacteristic>> missingDataCharacteristics = expectedViolation.hasDataCharacteristics(violatingVertex
                        .getAllDataCharacteristics());
                if (!missingDataCharacteristics.isEmpty()) {
                    logger.warn(String.format("Skipped: Vertex %s is missing the following data characteristics: %s", violatingVertex,
                            missingDataCharacteristics));
                    continue;
                }

                var incorrectDataCharacteristics = expectedViolation.hasMissingDataCharacteristics(violatingVertex
                        .getAllDataCharacteristics());
                if (!incorrectDataCharacteristics.isEmpty()) {
                    logger.warn(String.format("Skipped: Vertex %s has the following incorrect data characteristics: %s", violatingVertex,
                            incorrectDataCharacteristics));
                    continue;
                }

                logger.info("Found violating Vertex.");
                numberOfViolatingVertices++;
            }

            logger.info("Number of violating Vertex: " + numberOfViolatingVertices);
            assertNotEquals(0, numberOfViolatingVertices, "Found no matching vertices for violation.");
        }
    }
}
