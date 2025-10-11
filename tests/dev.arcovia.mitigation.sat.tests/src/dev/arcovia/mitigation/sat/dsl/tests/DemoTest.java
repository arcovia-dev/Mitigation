package dev.arcovia.mitigation.sat.dsl.tests;

import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.Literal;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.sat.dsl.BaseFormula;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import dev.arcovia.mitigation.sat.dsl.tests.utility.CNFUtil;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.analysis.dsl.selectors.Intersection;
import org.dataflowanalysis.analysis.dsl.variable.ConstraintVariable;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;
import dev.arcovia.mitigation.sat.dsl.tests.utility.DataLoader;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

import org.apache.log4j.Logger;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DemoTest {

    private final Logger logger = Logger.getLogger(DemoTest.class);

    @Test
    public void verySimpleConstraint() {
        AnalysisConstraint constraint = new ConstraintDSL().ofData()
                .withLabel("Sensitivity", "Personal")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Location", "nonEU")
                .create();

        var translation = new CNFTranslation(constraint);
        translation.constructCNF();

        logger.info(translation.formulaToString());
        logger.info(translation.cnfToString());
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

        var translation = new CNFTranslation(constraint);
        translation.constructCNF();

        logger.info(translation.formulaToString());
        logger.info(translation.simpleCNFToString());
        logger.info(translation.getCNFStatistics());
    }

    @Test
    public void complexConstraint() throws StandaloneInitializationException {

        String model = "ewolff";
        int variant = 5;
        String name = model + "_" + variant;
        String location = Paths.get("scenarios", "dfd", "TUHH-Models")
                .toString();
        String inputDataFlowDiagram = Paths.get(location, model, (name + ".dataflowdiagram"))
                .toString();
        String inputDataDictionary = Paths.get(location, model, (name + ".datadictionary"))
                .toString();
        DataFlowDiagramAndDictionary dfd = DataLoader.loadDFDFromPath(inputDataFlowDiagram, inputDataDictionary);

        AnalysisConstraint constraint = new ConstraintDSL().ofData()
                .withLabel("Monitoring", ConstraintVariable.of("MonitoringDashboard"))
                .withLabel("Endpoints", ConstraintVariable.of("Endpoints"))
                .withoutLabel("Bad", "Data")
                .withLabel("Positivity", List.of("A", "B", "C"))
                .withoutLabel("Negativity", List.of("A", "B", "C"))
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
    }

    @Test
    public void duplicateLiteralTest() {
        AnalysisConstraint con = new ConstraintDSL().ofData()
                .withLabel("Sensitivity", "Personal")
                .withLabel("Sensitivity", "Personal")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Location", "nonEU")
                .create();

        var translation = new CNFTranslation(con).constructCNF();
        var checkCnf = BaseFormula.fromCNF(translation)
                .toCNF();

        logger.info(CNFUtil.cnfToString(checkCnf));
        assertEquals(Collections.emptyList(), CNFUtil.getGreatestDifference(translation, checkCnf));

        translation.get(0)
                .literals()
                .add(new Literal(false, new NodeLabel(new Label("Location", "nonEU"))));
        var newCnf = BaseFormula.fromCNF(translation)
                .toCNF();

        logger.info(CNFUtil.cnfToString(translation));
        logger.info(CNFUtil.cnfToString(newCnf));

        assertNotEquals(Collections.emptyList(), CNFUtil.getGreatestDifference(translation, newCnf));
    }
}
