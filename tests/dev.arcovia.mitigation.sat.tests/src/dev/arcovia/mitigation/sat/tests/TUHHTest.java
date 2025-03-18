package dev.arcovia.mitigation.sat.tests;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.arcovia.mitigation.sat.*;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.dataflowanalysis.dfd.datadictionary.Behavior;
import org.dataflowanalysis.examplemodels.Activator;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;



public class TUHHTest {
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

    final Map<Label, Integer> costs = Map.ofEntries(
            entry(new Label("Stereotype", "authenticated_request"), 4), entry(new Label("Stereotype", "transform_identity_representation"), 3),
            entry(new Label("Stereotype", "token_validation"), 1), entry(new Label("Stereotype", "login_attempts_regulation"), 2),
            entry(new Label("Stereotype", "encrypted_connection"), 3), entry(new Label("Stereotype", "log_sanitization"), 2),
            entry(new Label("Stereotype", "local_logging"), 2));

    @Test
    public void tuhhTest() throws ContradictionException, TimeoutException, IOException, StandaloneInitializationException {
        var dfdConverter = new DataFlowDiagramConverter();

        var tuhhModels = TuhhModels.getTuhhModels();

        for (var model : tuhhModels.keySet()) {
            for (int variant : tuhhModels.get(model)) {
                String name = model + "_" + variant;

                System.out.println(name);

                var repairedDfdCosts = runRepair(model, name, variant == 0, constraints);

                if (variant == 0)
                    dfdConverter.storeWeb(dfdConverter.dfdToWeb(repairedDfdCosts), "testresults/" + name + "-repaired.json");

                assertTrue(new Mechanic(repairedDfdCosts,null, null).violatesDFD(repairedDfdCosts,constraints));
            }
        }
    }
    @Test
    void efficiencyTest() throws ContradictionException, TimeoutException, IOException, StandaloneInitializationException {
        var tuhhModels = TuhhModels.getTuhhModels();
        for (var model : tuhhModels.keySet()) {
            System.out.println("Checking " + model);
            if (!tuhhModels.get(model).contains(0)) continue;

            var repairedDfd = runRepair(model, model+"_0", false, constraints);

            for (int variant : tuhhModels.get(model)) {
                List<Constraint> constraint = switch (variant) {
                    case 1 -> List.of(entryViaGatewayOnly, nonInternalGateway);
                    case 2 -> List.of(authenticatedRequest);
                    case 4 -> List.of(transformedEntry);
                    case 5 -> List.of(tokenValidation);
                    case 7 -> List.of(encryptedEntry, entryViaGatewayOnly, nonInternalGateway);
                    case 8 -> List.of(encryptedInternals);
                    case 10 -> List.of(localLogging);
                    case 11 -> List.of(localLogging, logSanitization);
                    default -> null;
                };
                if (constraint == null) continue;

                var cost = new ModelCostCalculator(repairedDfd, constraint, costs).calculateCost();
                var cost2 = new ModelCostCalculator(loadDFD(model, model + "_" + variant), constraint, costs).calculateCost();
                System.out.println("Comparing to " + model + "_" + variant);
                System.out.println(cost + " <= " + cost2 + " : "+ (cost <= cost2));


            }
        }
    }

    @Disabled
    @Test
    void specificTUHHTest() throws ContradictionException, TimeoutException, IOException, StandaloneInitializationException {
        var dfdConverter = new DataFlowDiagramConverter();
        String model = "ewolff-kafka";
        int variant = 7;

        String name = model + "_" + variant;

        var repairedDfdCosts = runRepair(model, name, true, constraints);
        dfdConverter.storeWeb(dfdConverter.dfdToWeb(repairedDfdCosts), "testresults/specific_" + name + "-repaired.json");
        assertTrue(new Mechanic(repairedDfdCosts,null, null).violatesDFD(repairedDfdCosts,constraints));
    }

    private DataFlowDiagramAndDictionary runRepair(String model, String name, Boolean store, List<Constraint> constraints)
            throws StandaloneInitializationException, ContradictionException, IOException, TimeoutException {
        var dfd = loadDFD(model, name);
        if (!store)
            name = null;
        return new Mechanic(dfd, name, constraints, costs).repair();
    }
    private DataFlowDiagramAndDictionary loadDFD(String model, String name) throws StandaloneInitializationException {
        var dfdConverter = new DataFlowDiagramConverter();
        final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
        final String location = Paths.get("casestudies", "TUHH-Models")
                .toString();

        return  dfdConverter.loadDFD(PROJECT_NAME, Paths.get(location, model, (name + ".dataflowdiagram"))
                .toString(), Paths.get(location, model, (name + ".datadictionary"))
                .toString(), Activator.class);
    }
}
