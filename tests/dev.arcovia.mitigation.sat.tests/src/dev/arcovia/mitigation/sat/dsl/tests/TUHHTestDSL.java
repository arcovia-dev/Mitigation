package dev.arcovia.mitigation.sat.dsl.tests;

import dev.arcovia.mitigation.sat.*;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import dev.arcovia.mitigation.sat.dsl.tests.utility.CNFUtil;
import dev.arcovia.mitigation.sat.tests.TUHHTest;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TUHHTestDSL {

    private final Logger logger = Logger.getLogger(TUHHTestDSL.class);

    final List<Constraint> constraints = new TUHHTest().getConstraints();

    final List<AnalysisConstraint> analysisConstraints = List.of(
            new ConstraintDSL().ofData()
                    .withLabel("Stereotype", "entrypoint")
                    .withoutLabel("Stereotype", "gateway")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "internal")
                    .create(),
            new ConstraintDSL().ofData()
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "gateway")
                    .withCharacteristic("Stereotype", "internal")
                    .create(),
            new ConstraintDSL().ofData()
                    .withoutLabel("Stereotype", "authenticated_request")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "internal")
                    .create(),
            new ConstraintDSL().ofData()
                    .withLabel("Stereotype", "entrypoint")
                    .withoutLabel("Stereotype", "transform_identity_representation")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "internal")
                    .create(),
            new ConstraintDSL().ofData()
                    .withLabel("Stereotype", "entrypoint")
                    .withoutLabel("Stereotype", "token_validation")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "internal")
                    .create(),
            new ConstraintDSL().ofData()
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "authorization_server")
                    .withoutCharacteristic("Stereotype", "login_attempts_regulation")
                    .create(),
            new ConstraintDSL().ofData()
                    .withLabel("Stereotype", "entrypoint")
                    .withoutLabel("Stereotype", "encrypted_connection")
                    .neverFlows()
                    .toVertex()
                    .create(),
            new ConstraintDSL().ofData()
                    .withLabel("Stereotype", "internal")
                    .withoutLabel("Stereotype", "encrypted_connection")
                    .neverFlows()
                    .toVertex()
                    .create(),
            new ConstraintDSL().ofData()
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "internal")
                    .withoutCharacteristic("Stereotype", "local_logging")
                    .create(),
            new ConstraintDSL().ofData()
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "local_logging")
                    .withoutCharacteristic("Stereotype", "log_sanitization")
                    .create()

    );

    @Test
    public void validateConvertedTUHHConstraints() {

        logger.info(CNFUtil.cnfToString(constraints));

        var converted = analysisConstraints.stream()
                .map(CNFTranslation::new)
                .map(CNFTranslation::constructCNF)
                .flatMap(List::stream)
                .toList();

        logger.info(CNFUtil.cnfToString(converted));
        assertEquals(Collections.emptyList(), CNFUtil.getGreatestDifference(constraints, converted));
    }
}
