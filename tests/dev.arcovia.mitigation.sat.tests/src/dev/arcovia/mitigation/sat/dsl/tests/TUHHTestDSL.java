package dev.arcovia.mitigation.sat.dsl.tests;

import dev.arcovia.mitigation.sat.*;
import dev.arcovia.mitigation.sat.dsl.BaseFormula;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import dev.arcovia.mitigation.sat.dsl.tests.utility.CNFUtil;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TUHHTestDSL {

    private final Logger logger = Logger.getLogger(TUHHTestDSL.class);

    final Constraint entryViaGatewayOnly = new Constraint(List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
            new Literal(false, new IncomingDataLabel(new Label("Stereotype", "entrypoint"))),
            new Literal(true, new IncomingDataLabel(new Label("Stereotype", "gateway")))));
    final Constraint nonInternalGateway = new Constraint(List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "gateway"))),
            new Literal(false, new NodeLabel(new Label("Stereotype", "internal")))));
    final Constraint authenticatedRequest = new Constraint(List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
            new Literal(true, new IncomingDataLabel(new Label("Stereotype", "authenticated_request")))));
    final Constraint transformedEntry = new Constraint(List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
            new Literal(false, new IncomingDataLabel(new Label("Stereotype", "entrypoint"))),
            new Literal(true, new IncomingDataLabel(new Label("Stereotype", "transform_identity_representation")))));
    final Constraint tokenValidation = new Constraint(List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
            new Literal(false, new IncomingDataLabel(new Label("Stereotype", "entrypoint"))),
            new Literal(true, new IncomingDataLabel(new Label("Stereotype", "token_validation")))));
    final Constraint loginAttempts = new Constraint(List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "authorization_server"))),
            new Literal(true, new NodeLabel(new Label("Stereotype", "login_attempts_regulation")))));
    final Constraint encryptedEntry = new Constraint(List.of(new Literal(false, new IncomingDataLabel(new Label("Stereotype", "entrypoint"))),
            new Literal(true, new IncomingDataLabel(new Label("Stereotype", "encrypted_connection")))));
    final Constraint encryptedInternals = new Constraint(List.of(new Literal(false, new IncomingDataLabel(new Label("Stereotype", "internal"))),
            new Literal(true, new IncomingDataLabel(new Label("Stereotype", "encrypted_connection")))));
    final Constraint localLogging = new Constraint(List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
            new Literal(true, new NodeLabel(new Label("Stereotype", "local_logging")))));
    final Constraint logSanitization = new Constraint(List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "local_logging"))),
            new Literal(true, new NodeLabel(new Label("Stereotype", "log_sanitization")))));
    // List of TUHH constraints without existence checks like auth-,logging-,secrets-server

    final List<Constraint> constraints = List.of(entryViaGatewayOnly, nonInternalGateway, authenticatedRequest, transformedEntry, tokenValidation,
            loginAttempts, encryptedEntry, encryptedInternals, localLogging, logSanitization);

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
    public void convert() {

        logger.info(CNFUtil.cnfToString(constraints));

        var converted = analysisConstraints.stream()
                .map(CNFTranslation::new)
                .map(CNFTranslation::constructCNF)
                .flatMap(List::stream)
                .toList();

        logger.info(CNFUtil.cnfToString(converted));
        assertEquals(Collections.emptyList(), CNFUtil.compare(constraints, converted));
    }
}
