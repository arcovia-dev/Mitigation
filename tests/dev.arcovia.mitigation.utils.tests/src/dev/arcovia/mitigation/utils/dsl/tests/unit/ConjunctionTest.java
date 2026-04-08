package dev.arcovia.mitigation.utils.dsl.tests.unit;

import dev.arcovia.mitigation.sat.Constraint;
import dev.arcovia.mitigation.utils.dsl.CNFTranslation;
import dev.arcovia.mitigation.utils.dsl.tests.dummy.DInData;
import dev.arcovia.mitigation.utils.dsl.tests.dummy.DNode;
import dev.arcovia.mitigation.utils.dsl.tests.utility.CNFUtil;

import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConjunctionTest {

    private final Logger logger = Logger.getLogger(ConjunctionTest.class);

    static DInData dInDataPos1, dInDataPos2, dInDataNeg1, dInDataNeg2;
    static DNode dNodePos1, dNodePos2, dNodeNeg1, dNodeNeg2;
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
    }

    @Test
    public void singleConjunction() {
        constraint = new ConstraintDSL().ofData()
                .withLabel(dInDataPos1.type(), dInDataPos1.value())
                .neverFlows()
                .toVertex()
                .withoutCharacteristic(dNodeNeg1.type(), dNodeNeg1.value())
                .create();

        translation = new CNFTranslation(constraint);
        expected = List.of(CNFUtil.generateClause(List.of(dInDataPos1), List.of(dNodeNeg1)));

        actual = translation.constructCNF();
        logger.info("Evaluating CNF with Base Formula:" + translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());

        assertEquals(Collections.emptyList(), CNFUtil.getGreatestDifference(expected, actual));

        constraint = new ConstraintDSL().ofData()
                .withoutLabel(dInDataNeg1.type(), dInDataNeg1.value())
                .neverFlows()
                .toVertex()
                .withCharacteristic(dNodePos1.type(), dNodePos1.value())
                .create();

        translation = new CNFTranslation(constraint);
        expected = List.of(CNFUtil.generateClause(List.of(dInDataNeg1), List.of(dNodePos1)));

        actual = translation.constructCNF();
        logger.info("Evaluating CNF with Base Formula:" + translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());

        assertEquals(Collections.emptyList(), CNFUtil.getGreatestDifference(expected, actual));
    }

    @Test
    public void conjunctionLeft() {
        constraint = new ConstraintDSL().ofData()
                .withLabel(dInDataPos1.type(), dInDataPos1.value())
                .withoutLabel(dInDataNeg1.type(), dInDataNeg1.value())
                .neverFlows()
                .toVertex()
                .withCharacteristic(dNodePos1.type(), dNodePos1.value())
                .create();

        translation = new CNFTranslation(constraint);
        expected = List.of(CNFUtil.generateClause(List.of(dInDataPos1, dInDataNeg1), List.of(dNodePos1)));
        actual = translation.constructCNF();

        logger.info("Evaluating CNF with Base Formula:" + translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());

        assertEquals(Collections.emptyList(), CNFUtil.getGreatestDifference(expected, actual));

        constraint = new ConstraintDSL().ofData()
                .withLabel(dInDataPos1.type(), dInDataPos1.value())
                .withoutLabel(dInDataNeg1.type(), dInDataNeg1.value())
                .neverFlows()
                .toVertex()
                .withoutCharacteristic(dNodeNeg1.type(), dNodeNeg1.value())
                .create();

        translation = new CNFTranslation(constraint);
        expected = List.of(CNFUtil.generateClause(List.of(dInDataPos1, dInDataNeg1), List.of(dNodeNeg1)));

        actual = translation.constructCNF();
        logger.info("Evaluating CNF with Base Formula:" + translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());

        assertEquals(Collections.emptyList(), CNFUtil.getGreatestDifference(expected, actual));
    }

    @Test
    public void conjunctionRight() {
        constraint = new ConstraintDSL().ofData()
                .withLabel(dInDataPos1.type(), dInDataPos1.value())
                .neverFlows()
                .toVertex()
                .withCharacteristic(dNodePos1.type(), dNodePos1.value())
                .withoutCharacteristic(dNodeNeg1.type(), dNodeNeg1.value())
                .create();

        translation = new CNFTranslation(constraint);
        expected = List.of(CNFUtil.generateClause(List.of(dInDataPos1), List.of(dNodePos1, dNodeNeg1)));
        actual = translation.constructCNF();

        logger.info("Evaluating CNF with Base Formula:" + translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());
        assertEquals(Collections.emptyList(), CNFUtil.getGreatestDifference(expected, actual));

        constraint = new ConstraintDSL().ofData()
                .withoutLabel(dInDataNeg1.type(), dInDataNeg1.value())
                .neverFlows()
                .toVertex()
                .withCharacteristic(dNodePos1.type(), dNodePos1.value())
                .withoutCharacteristic(dNodeNeg1.type(), dNodeNeg1.value())
                .create();

        translation = new CNFTranslation(constraint);
        expected = List.of(CNFUtil.generateClause(List.of(dInDataNeg1), List.of(dNodePos1, dNodeNeg1)));

        actual = translation.constructCNF();
        logger.info("Evaluating CNF with Base Formula:" + translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());

        assertEquals(Collections.emptyList(), CNFUtil.getGreatestDifference(expected, actual));
    }

    @Test
    public void conjunctionLeftAndRight() {
        constraint = new ConstraintDSL().ofData()
                .withLabel(dInDataPos1.type(), dInDataPos1.value())
                .withoutLabel(dInDataNeg1.type(), dInDataNeg1.value())
                .neverFlows()
                .toVertex()
                .withCharacteristic(dNodePos1.type(), dNodePos1.value())
                .withoutCharacteristic(dNodeNeg1.type(), dNodeNeg1.value())
                .create();

        translation = new CNFTranslation(constraint);
        expected = List.of(CNFUtil.generateClause(List.of(dInDataPos1, dInDataNeg1), List.of(dNodePos1, dNodeNeg1)));
        actual = translation.constructCNF();

        logger.info("Evaluating CNF with Base Formula:" + translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());
        assertEquals(Collections.emptyList(), CNFUtil.getGreatestDifference(expected, actual));
    }
}
