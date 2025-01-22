package dev.arcovia.mitigation.sat.tests;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import dev.arcovia.mitigation.sat.*;
import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.dataflowanalysis.examplemodels.Activator;
import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import com.google.common.collect.ImmutableMap;

import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

public class TUHHTest {
    @Test
    public void tuhhTest() throws ContradictionException, TimeoutException, IOException, StandaloneInitializationException {
        var dfdConverter = new DataFlowDiagramConverter();
        final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
        final String location = Paths.get("casestudies", "TUHH-Models")
                .toString();

        var entryViaGatewayOnly = new Constraint(List.of(
                new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
                new Literal(false, new IncomingDataLabel(new Label("Stereotype" , "entrypoint"))),
                new Literal(true, new IncomingDataLabel(new Label("Stereotype" , "gateway")))));
        var nonInternalGateway = new Constraint(List.of(
                new Literal(false, new NodeLabel(new Label("Stereotype", "gateway"))),
                new Literal(false, new NodeLabel(new Label("Stereotype", "internal")))));
        var authenticatedRequest = new Constraint(List.of(
                new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
                new Literal(true, new IncomingDataLabel(new Label("Stereotype" , "authenticated_request")))));
        var transformedEntry = new Constraint(List.of(
                new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
                new Literal(false, new IncomingDataLabel(new Label("Stereotype" , "entrypoint"))),
                new Literal(true, new IncomingDataLabel(new Label("Stereotype" , "transform_identity_representation")))));
        var tokenValidation = new Constraint(List.of(
                new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
                new Literal(false, new IncomingDataLabel(new Label("Stereotype" , "entrypoint"))),
                new Literal(true, new IncomingDataLabel(new Label("Stereotype" , "token_validation")))));
        var loginAttempts = new Constraint(List.of(
                new Literal(false, new NodeLabel(new Label("Stereotype", "authorization_server" ))),
                new Literal(true, new NodeLabel(new Label("Stereotype" , "login_attempts_regulation")))));
        var encryptedEntry = new Constraint(List.of(
                new Literal(false, new IncomingDataLabel(new Label("Stereotype" , "entrypoint"))),
                new Literal(true, new IncomingDataLabel(new Label("Stereotype" , "encrypted_connection")))
        ));
        var encryptedInternals = new Constraint(List.of(
                new Literal(false, new IncomingDataLabel(new Label("Stereotype" , "internal"))),
                new Literal(true, new IncomingDataLabel(new Label("Stereotype" , "encrypted_connection")))
        ));
        var localLogging = new Constraint(List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
                new Literal(true, new NodeLabel(new Label("Stereotype", "local_logging")))));
        var logSanitization = new Constraint(List.of(
                new Literal(false, new NodeLabel(new Label("Stereotype", "local_logging"))),
                new Literal(true, new NodeLabel(new Label("Stereotype", "log_sanitization")))
        ));
        var constraints = List.of(entryViaGatewayOnly,nonInternalGateway,authenticatedRequest,transformedEntry,
                tokenValidation,loginAttempts,encryptedEntry,encryptedInternals,localLogging);

        Map<Label, Integer> costs = ImmutableMap.<Label, Integer>builder()
                .put(new Label("Stereotype", "internal"), 3)
                .put(new Label("Stereotype", "local_logging"), 1)
                .build();

        var dfd = dfdConverter.loadDFD(PROJECT_NAME, Paths.get(location, "jferrater", "jferrater_0.dataflowdiagram")
                .toString(),
                Paths.get(location, "jferrater", "jferrater_0.datadictionary")
                        .toString(),
                Activator.class);
        
        var repairedDfdCosts = new Mechanic(dfd, constraints, costs, "jferrater").repair();
        
        dfdConverter.storeWeb(dfdConverter.dfdToWeb(repairedDfdCosts), "testresults/jferrater-repaired.json");
        
    }
}
