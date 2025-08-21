package dev.arcovia.mitigation.sat.dsl.tests;

import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.analysis.dsl.selectors.Intersection;
import org.dataflowanalysis.analysis.dsl.variable.ConstraintVariable;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import dev.arcovia.mitigation.sat.dsl.tests.utility.DataLoader;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

import org.apache.log4j.Logger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DemoTest {

    private final Logger logger = Logger.getLogger(DemoTest.class);
    private static DataFlowDiagramAndDictionary dfd;

    @BeforeAll
    public static void setup() throws StandaloneInitializationException {
        String model = "ewolff";
        int variant = 5;

        String name = model + "_" + variant;

        dfd = DataLoader.loadDFD(model, name);
    }

    @Test
    public void verySimpleConstraint() {
        AnalysisConstraint constraint = new ConstraintDSL().ofData()
                .withLabel("Sensitivity", "Personal")
//                .withoutLabel("Encryption", "Encrypted")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Location", "nonEU")
                .create();

        var translation = new CNFTranslation(constraint);
        translation.constructCNF();

        logger.info(translation.formulaToString());
        logger.info(translation.cnfToString());
//        logger.info(translation.getCNFStatistics());

        assertTrue(true);
    }

    @Test
    public void simpleConstraint() {

        AnalysisConstraint constraint = new ConstraintDSL().ofData()
                .withLabel("Data", "Pos")
                .withoutLabel("NoData", "Neg")
                .withLabel("Positivity", List.of("A", "B", "C"))
                .withoutLabel("Negativity", List.of("A", "B", "C"))
                .neverFlows()
                .toVertex()
                .withCharacteristic("Node", "Pos")
                .withoutCharacteristic("NoNode", "Neg")
                .withCharacteristic("Positivity", List.of("A", "B", "C"))
                .withoutCharacteristic("Negativity", List.of("A", "B", "C"))
                .create();

        var translation = new CNFTranslation(constraint, dfd);
        translation.constructCNF();

        logger.info(translation.formulaToString());
        logger.info(translation.simpleCNFToString());
        logger.info(translation.getCNFStatistics());

        assertTrue(true);
    }

    @Test
    public void complexConstraint() {

        AnalysisConstraint constraint = new ConstraintDSL().ofData()
                .withLabel("Monitoring", ConstraintVariable.of("MonitoringDashboard"))
                .withLabel("Endpoints", ConstraintVariable.of("Endpoints"))
                .withoutLabel("Bad", "Data")
                .withLabel("Positivity", List.of("A", "B", "C"))
                .withoutLabel("Negativity", List.of("A", "B", "C"))
                .fromNode()
                .withCharacteristic("Out", "Node")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Role", "Clerk")
                .withoutCharacteristic("Role", "User")
                .withCharacteristic("Positivity", List.of("A", "B", "C"))
                .withoutCharacteristic("Negativity", List.of("A", "B", "C"))
                .withCharacteristic("Circuit", ConstraintVariable.of("CircuitBreaker"))
                .withCharacteristic("NodePort", ConstraintVariable.of("Port"))
                .where()
                .isEmpty(ConstraintVariable.of("Endpoints"))
                .isNotEmpty(ConstraintVariable.of("Port"))
                .isEmpty(Intersection.of(ConstraintVariable.of("MonitoringDashboard"), ConstraintVariable.of("CircuitBreaker")))
                .create();

        var translation = new CNFTranslation(constraint, dfd);
        translation.constructCNF();

        logger.info(translation.formulaToString());
        logger.info(translation.simpleCNFToString());
        logger.info(translation.getCNFStatistics());

        assertTrue(true);
    }
}
