package dev.arcovia.mitigation.utils.dsl.tests.unit;

import dev.arcovia.mitigation.sat.Constraint;
import dev.arcovia.mitigation.sat.LabelCategory;
import dev.arcovia.mitigation.utils.dsl.CNFTranslation;
import dev.arcovia.mitigation.utils.dsl.tests.dummy.DInData;
import dev.arcovia.mitigation.utils.dsl.tests.dummy.DNode;
import dev.arcovia.mitigation.utils.dsl.tests.utility.CNFUtil;

import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.analysis.dsl.selectors.Intersection;
import org.dataflowanalysis.analysis.dsl.variable.ConstraintVariable;
import org.dataflowanalysis.analysis.dsl.variable.ConstraintVariableReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VariableTest {
    private final Logger logger = Logger.getLogger(VariableTest.class);

    static DInData dInDataPos1, dInDataPos2, dInDataNeg1, dInDataNeg2;
    static DNode dNodePos1, dNodePos2, dNodeNeg1, dNodeNeg2;
    static Map<String, List<String>> variablesDataPos, variablesDataNeg, variablesNodePos, variablesNodeNeg, variablesDataNode;
    static AnalysisConstraint constraint;
    static CNFTranslation translation;
    static List<Constraint> expected;
    static List<Constraint> actual;

    @BeforeAll
    public static void setup() {
        dInDataPos1 = new DInData(true);
        dInDataPos2 = new DInData(true);
        dInDataNeg1 = new DInData(false);
        dInDataNeg2 = new DInData(false);
        dNodePos1 = new DNode(true);
        dNodePos2 = new DNode(true);
        dNodeNeg1 = new DNode(false);
        dNodeNeg2 = new DNode(false);
        variablesDataPos = new HashMap<>();
        variablesDataNeg = new HashMap<>();
        variablesNodePos = new HashMap<>();
        variablesNodeNeg = new HashMap<>();
        variablesDataNode = new HashMap<>();
        variablesDataPos.put(dInDataPos1.type(), List.of(dInDataPos1.value(), dInDataPos2.value()));
        variablesDataNeg.put(dInDataNeg1.type(), List.of(dInDataNeg1.value(), dInDataNeg2.value()));
        variablesNodePos.put(dNodePos1.type(), List.of(dNodePos1.value(), dNodePos2.value()));
        variablesNodeNeg.put(dNodeNeg1.type(), List.of(dNodeNeg1.value(), dNodeNeg2.value()));
        variablesDataNode.put(dInDataNeg1.type(), List.of(dInDataNeg1.value(), dInDataNeg2.value()));
        variablesDataNode.put(dNodeNeg1.type(), List.of(dInDataNeg1.value(), dInDataNeg2.value()));
    }

    @Test
    public void isNotEmptyDataCondition() {
        constraint = new ConstraintDSL().ofData()
                .withLabel(dInDataPos1.type(), ConstraintVariableReference.of(dInDataPos1.type()))
                .neverFlows()
                .toVertex()
                .withCharacteristic(dNodePos1.type(), dNodePos1.value())
                .where()
                .isNotEmpty(ConstraintVariable.of(dInDataPos1.type()))
                .create();

        translation = new CNFTranslation(constraint, variablesDataPos);
        expected = List.of(CNFUtil.generateClause(List.of(dInDataPos1), List.of(dNodePos1)),
                CNFUtil.generateClause(List.of(dInDataPos2), List.of(dNodePos1)));

        actual = translation.constructCNF();
        logger.info("Evaluating CNF with Base Formula:" + translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());
        assertEquals(Collections.emptyList(), CNFUtil.getGreatestDifference(expected, actual));
    }

    @Test
    public void isNotEmptyNodeCondition() {

        constraint = new ConstraintDSL().ofData()
                .withLabel(dInDataPos1.type(), dInDataPos1.value())
                .neverFlows()
                .toVertex()
                .withCharacteristic(dNodePos1.type(), ConstraintVariable.of(dNodePos1.type()))
                .where()
                .isNotEmpty(ConstraintVariable.of(dNodePos1.type()))
                .create();

        translation = new CNFTranslation(constraint, variablesNodePos);
        expected = List.of(CNFUtil.generateClause(List.of(dInDataPos1), List.of(dNodePos1)),
                CNFUtil.generateClause(List.of(dInDataPos1), List.of(dNodePos2)));

        actual = translation.constructCNF();
        logger.info("Evaluating CNF with Base Formula:" + translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());
        assertEquals(Collections.emptyList(), CNFUtil.getGreatestDifference(expected, actual));
    }

    @Test
    public void isEmptyDataCondition() {

        constraint = new ConstraintDSL().ofData()
                .withLabel(dInDataNeg1.type(), ConstraintVariable.of(dInDataNeg1.type()))
                .neverFlows()
                .toVertex()
                .withCharacteristic(dNodePos1.type(), dNodePos1.value())
                .where()
                .isEmpty(ConstraintVariable.of(dInDataNeg1.type()))
                .create();

        translation = new CNFTranslation(constraint, variablesDataNeg);
        expected = List.of(CNFUtil.generateClause(List.of(dInDataNeg1, dInDataNeg2), List.of(dNodePos1)));

        actual = translation.constructCNF();
        logger.info("Evaluating CNF with Base Formula:" + translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());
        assertEquals(Collections.emptyList(), CNFUtil.getGreatestDifference(expected, actual));
    }

    @Test
    public void isEmptyNodeCondition() {

        constraint = new ConstraintDSL().ofData()
                .withLabel(dInDataPos1.type(), dInDataPos1.value())
                .neverFlows()
                .toVertex()
                .withCharacteristic(dNodeNeg1.type(), ConstraintVariable.of(dNodeNeg1.type()))
                .where()
                .isEmpty(ConstraintVariable.of(dNodeNeg1.type()))
                .create();

        translation = new CNFTranslation(constraint, variablesNodeNeg);
        expected = List.of(CNFUtil.generateClause(List.of(dInDataPos1), List.of(dNodeNeg1, dNodeNeg2)));

        actual = translation.constructCNF();
        logger.info("Evaluating CNF with Base Formula:" + translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());
        assertEquals(Collections.emptyList(), CNFUtil.getGreatestDifference(expected, actual));
    }

    @Test
    public void emptyIntersectionCondition() {

        constraint = new ConstraintDSL().ofData()
                .withLabel(dInDataNeg1.type(), ConstraintVariable.of(dInDataNeg1.type()))
                .neverFlows()
                .toVertex()
                .withCharacteristic(dNodeNeg1.type(), ConstraintVariable.of(dNodeNeg1.type()))
                .where()
                .isEmpty(Intersection.of(ConstraintVariable.of(dInDataNeg1.type()), ConstraintVariable.of(dNodeNeg1.type())))
                .create();

        translation = new CNFTranslation(constraint, variablesDataNode);
        expected = List.of(
                CNFUtil.generateClause(List.of(dInDataNeg1),
                        List.of(new DNode(dInDataNeg1.positive(), LabelCategory.Node.name(), dInDataNeg1.value()))),
                CNFUtil.generateClause(List.of(dInDataNeg2),
                        List.of(new DNode(dInDataNeg2.positive(), LabelCategory.Node.name(), dInDataNeg2.value()))));

        actual = translation.constructCNF();
        logger.info("Evaluating CNF with Base Formula:" + translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());
        assertEquals(Collections.emptyList(), CNFUtil.getGreatestDifference(expected, actual));
    }
}
