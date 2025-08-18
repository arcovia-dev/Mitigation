package dev.arcovia.mitigation.sat.cnf.tests;

import dev.arcovia.mitigation.sat.cnf.CNFTranslation;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.analysis.dsl.selectors.Intersection;
import org.dataflowanalysis.analysis.dsl.variable.ConstraintVariable;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.examplemodels.Activator;
import org.junit.jupiter.api.Test;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;
import tools.mdsd.modelingfoundations.identifier.NamedElement;
import org.apache.log4j.Logger;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DemoTest {

    private final Logger logger = Logger.getLogger(DemoTest.class);

    @Test
    public void testCNF() throws StandaloneInitializationException {
        String model = "ewolff";
        int variant = 5;

        String name = model + "_" + variant;

        var dfd = loadDFD(model, name);
        var variables = variables(dfd);
        assertTrue(true);

        AnalysisConstraint constraint = new ConstraintDSL().ofData()
                .withLabel("Data", "Pos")
                .withoutLabel("NoData", "Neg")
                .withLabel("Positivity", List.of("A", "B", "C"))
                .withoutLabel("Negativity", List.of("A", "B", "C"))
//                .fromNode()
                .neverFlows()
                .toVertex()
                .withCharacteristic("Node", "Pos")
                .withoutCharacteristic("NoNode", "Neg")
                .withCharacteristic("Positivity", List.of("A", "B", "C"))
                .withoutCharacteristic("Negativity", List.of("A", "B", "C"))
//                .where()
                .create();

        assertTrue(true);

        var translation = new CNFTranslation(constraint, dfd);
        translation.initialiseTranslation();

        logger.info(translation.formulaToString());

        translation.constructCNF();
        logger.info(translation.simpleCNFToString());
    }

    @Test
    public void testImplementation() throws StandaloneInitializationException {
        String model = "ewolff";
        int variant = 5;

        String name = model + "_" + variant;

        var dfd = loadDFD(model, name);
        var variables = variables(dfd);
        assertTrue(true);

        AnalysisConstraint constraint = new ConstraintDSL().ofData()
                .withLabel("Monitoring", ConstraintVariable.of("MonitoringDashboard"))
                .withLabel("Endpoints", ConstraintVariable.of("Endpoints"))
                .withoutLabel("Bad", "Data")
                .withLabel("Positivity", List.of("A", "B", "C"))
                .withoutLabel("Negativity", List.of("A", "B", "C"))
                .fromNode()
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

        assertTrue(true);

        var translation = new CNFTranslation(constraint, dfd);
        translation.initialiseTranslation();

        logger.info(translation.formulaToString());

        translation.constructCNF();
        logger.info(translation.simpleCNFToString());
    }

    private HashMap<String, List<String>> variables(DataFlowDiagramAndDictionary dfd){
        var variables = new HashMap<String, List<String>>();
        dfd.dataDictionary().getLabelTypes().forEach(it -> variables.put(
                it.getEntityName(),
                it.getLabel().stream().map(NamedElement::getEntityName).toList()));
        return variables;
    }

    private DataFlowDiagramAndDictionary loadDFD(String model, String name) throws StandaloneInitializationException {
        final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
        final String location = Paths.get("scenarios","dfd", "TUHH-Models")
                .toString();
        return new DataFlowDiagramAndDictionary(PROJECT_NAME,
                Paths.get(location, model, (name + ".dataflowdiagram")).toString(),
                Paths.get(location, model, (name + ".datadictionary"))
                        .toString(), Activator.class);
    }
}
