package dev.arcovia.mitigation.sat.cnf.tests.unit;

import dev.arcovia.mitigation.sat.cnf.CNFTranslation;
import dev.arcovia.mitigation.sat.cnf.tests.utility.CNFUtil;
import dev.arcovia.mitigation.sat.cnf.tests.utility.DCNF;
import dev.arcovia.mitigation.sat.cnf.tests.utility.DInData;
import dev.arcovia.mitigation.sat.cnf.tests.utility.DNode;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DisjunctionTest {

    private final Logger logger = Logger.getLogger(DisjunctionTest.class);

    static DInData dInDataPos1, dInDataPos2, dInDataNeg1, dInDataNeg2;
    static DNode dNodePos1, dNodePos2,  dNodeNeg1, dNodeNeg2;
    static AnalysisConstraint constraint;
    static CNFTranslation translation;
    static DCNF expected;
    static DCNF actual;

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
    public void disjunctionLeft() {
        constraint = new ConstraintDSL().ofData()
                .withLabel(dInDataPos1.type(), List.of(dInDataPos1.value(), dInDataPos2.value()))
                .neverFlows()
                .toVertex()
                .withCharacteristic(dNodePos1.type(), dNodePos1.value())
                .create();

        translation = new CNFTranslation(constraint, null);
        expected = new DCNF(List.of(
                CNFUtil.generateClause(List.of(dInDataPos1), List.of(), List.of(dNodePos1)),
                CNFUtil.generateClause(List.of(dInDataPos2), List.of(), List.of(dNodePos1))
        ));
        actual = new DCNF(translation.constructCNF());

        logger.info("Evaluating CNF with Base Formula:"+ translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());
        assertEquals(expected, actual);

        constraint = new ConstraintDSL().ofData()
                .withoutLabel(dInDataNeg1.type(), List.of(dInDataNeg1.value(), dInDataNeg2.value()))
                .neverFlows()
                .toVertex()
                .withCharacteristic(dNodePos1.type(), dNodePos1.value())
                .create();

        translation = new CNFTranslation(constraint, null);
        expected = new DCNF(List.of(
                CNFUtil.generateClause(List.of(dInDataNeg1, dInDataNeg2), List.of(), List.of(dNodePos1))
        ));
        actual = new DCNF(translation.constructCNF());

        logger.info("Evaluating CNF with Base Formula:"+ translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());
        assertEquals(expected, actual);
    }

    @Test
    public void disjunctionRight() {
        constraint = new ConstraintDSL().ofData()
                .withLabel(dInDataPos1.type(), dInDataPos1.value())
                .neverFlows()
                .toVertex()
                .withCharacteristic(dNodePos1.type(), List.of(dNodePos1.value(), dNodePos2.value()))
                .create();

        translation = new CNFTranslation(constraint, null);
        expected = new DCNF(List.of(
                CNFUtil.generateClause(List.of(dInDataPos1), List.of(), List.of(dNodePos1)),
                CNFUtil.generateClause(List.of(dInDataPos1), List.of(), List.of(dNodePos2))
        ));
        actual = new DCNF(translation.constructCNF());

        logger.info("Evaluating CNF with Base Formula:"+ translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());
        assertEquals(expected, actual);

        constraint = new ConstraintDSL().ofData()
                .withLabel(dInDataPos1.type(), dInDataPos1.value())
                .neverFlows()
                .toVertex()
                .withoutCharacteristic(dNodeNeg1.type(), List.of(dNodeNeg1.value(), dNodeNeg2.value()))
                .create();

        translation = new CNFTranslation(constraint, null);
        expected = new DCNF(List.of(
                CNFUtil.generateClause(List.of(dInDataPos1), List.of(), List.of(dNodeNeg1, dNodeNeg2))
        ));
        actual = new DCNF(translation.constructCNF());

        logger.info("Evaluating CNF with Base Formula:"+ translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());
        assertEquals(expected, actual);
    }

    @Test
    public void conjunctionLeftAndRight() {
        constraint = new ConstraintDSL().ofData()
                .withLabel(dInDataPos1.type(), List.of(dInDataPos1.value(), dInDataPos2.value()))
                .neverFlows()
                .toVertex()
                .withCharacteristic(dNodePos1.type(), List.of(dNodePos1.value(), dNodePos2.value()))
                .create();

        translation = new CNFTranslation(constraint, null);
        expected = new DCNF(List.of(
                CNFUtil.generateClause(List.of(dInDataPos1), List.of(), List.of(dNodePos1)),
                CNFUtil.generateClause(List.of(dInDataPos2), List.of(), List.of(dNodePos1)),
                CNFUtil.generateClause(List.of(dInDataPos1), List.of(), List.of(dNodePos2)),
                CNFUtil.generateClause(List.of(dInDataPos2), List.of(), List.of(dNodePos2))
        ));
        actual = new DCNF(translation.constructCNF());

        logger.info("Evaluating CNF with Base Formula:"+ translation.formulaToString());
        logger.info("Generated CNF as:" + translation.cnfToString());
        assertEquals(expected, actual);
    }
}
