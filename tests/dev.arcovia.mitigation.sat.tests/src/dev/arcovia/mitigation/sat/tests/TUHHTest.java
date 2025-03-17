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

    final Map<Label, Integer> costs = Map.ofEntries(entry(new Label("Stereotype", "internal"), 10),
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

                System.out.println("Comparing to " + model + "_" + variant);

                var repairedDfdCosts = runRepair(model, model+"_0", false, constraint);

                compareDeltas(model, repairedDfdCosts, variant);

                System.out.println("Test");
            }
        }
    }
    private void compareDeltas(String model, DataFlowDiagramAndDictionary repairedDfd, int variant) throws StandaloneInitializationException {
        var repairedDiff = getDiff(repairedDfd, loadDFD(model, model + "_0"));
        var optimalDiff = getDiff(loadDFD(model, model + "_0"), loadDFD(model, model + "_" + variant));
        //assertEquals(repairedDiff, optimalDiff);
    }
    private List<String> getDiff(DataFlowDiagramAndDictionary original, DataFlowDiagramAndDictionary toCompare) {
        List<org.dataflowanalysis.dfd.dataflowdiagram.Node> nodesOriginal = original.dataFlowDiagram().getNodes();
        var nodesCompare = toCompare.dataFlowDiagram().getNodes();
        var diff = new ArrayList<String>();
        for (var node : nodesOriginal){

            var compareNode = nodesCompare.stream()
                    .filter(n -> n.getEntityName().equals(node.getEntityName()))
                    .findFirst()
                    .orElse(null);

            if (compareNode != null){
                diff.addAll(compareProperties(node.getProperties(), compareNode.getProperties(), node.getEntityName()));
                diff.addAll(compareBehavior(node.getBehavior(), compareNode.getBehavior(), node.getEntityName()));
            }
            else {
                System.out.println("Something went wrong");
            }
        }
        return diff;
    }
    private List<String> compareProperties(List<org.dataflowanalysis.dfd.datadictionary.Label> properties,
            List<org.dataflowanalysis.dfd.datadictionary.Label> compareNodeProperties, String nodeName) {
        var diff = new ArrayList<String>();
        // Extrahiere die entityNames in Sets
        Set<String> propertyNames = properties.stream()
                .map(org.dataflowanalysis.dfd.datadictionary.Label::getEntityName)
                .collect(Collectors.toSet());

        Set<String> comparePropertyNames = compareNodeProperties.stream()
                .map(org.dataflowanalysis.dfd.datadictionary.Label::getEntityName)
                .collect(Collectors.toSet());

        // 1. Elemente in propertyNames, die NICHT in comparePropertyNames sind
        Set<String> differenceA = propertyNames.stream()
                .filter(name -> !comparePropertyNames.contains(name))
                .collect(Collectors.toSet());

        // 2. Elemente in comparePropertyNames, die NICHT in propertyNames sind
        Set<String> differenceB = comparePropertyNames.stream()
                .filter(name -> !propertyNames.contains(name))
                .collect(Collectors.toSet());

        // Kombiniere beide Unterschiede
        Set<String> diffNames = new HashSet<>();
        diffNames.addAll(differenceA);
        diffNames.addAll(differenceB);

        // Jetzt in deine diff-Liste schreiben
        for (String diffName : diffNames) {
            diff.add(nodeName + "_" + diffName);
            System.out.println(nodeName + "_" + diffName);
        }
        return diff;
    }
    private List<String> compareBehavior(Behavior behavior, Behavior compareNodeBehavior, String nodeName) {
        var diff = new ArrayList<String>();

        return diff;
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
