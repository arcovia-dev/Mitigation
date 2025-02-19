package dev.arcovia.mitigation.sat.tests;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.arcovia.mitigation.sat.*;
import org.dataflowanalysis.analysis.core.AbstractTransposeFlowGraph;
import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.DataFlowDiagramConverter;
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

    final Map<Label, Integer> costs = Map.ofEntries(Map.entry(new Label("Stereotype", "internal"), 10),
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

                var repairedDfdCosts = runRepair(model, name, variant == 0);

                if (variant == 0)
                    dfdConverter.storeWeb(dfdConverter.dfdToWeb(repairedDfdCosts), "testresults/" + name + "-repaired.json");

                assertTrue(verifyRepairs(repairedDfdCosts));
            }
        }
    }

    @Disabled
    @Test
    void specificTUHHTest() throws ContradictionException, TimeoutException, IOException, StandaloneInitializationException {
        var dfdConverter = new DataFlowDiagramConverter();
        String model = "ewolff-kafka";
        int variant = 0;

        String name = model + "_" + variant;

        var repairedDfdCosts = runRepair(model, name, true);
        dfdConverter.storeWeb(dfdConverter.dfdToWeb(repairedDfdCosts), "testresults/specific_" + name + "-repaired.json");
    }

    private DataFlowDiagramAndDictionary runRepair(String model, String name, Boolean store)
            throws StandaloneInitializationException, ContradictionException, IOException, TimeoutException {
        var dfdConverter = new DataFlowDiagramConverter();
        final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
        final String location = Paths.get("casestudies", "TUHH-Models")
                .toString();

        var dfd = dfdConverter.loadDFD(PROJECT_NAME, Paths.get(location, model, (name + ".dataflowdiagram"))
                .toString(), Paths.get(location, model, (name + ".datadictionary"))
                .toString(), Activator.class);
        if (!store)
            name = null;
        return new Mechanic(dfd, name, constraints, costs).repair();
    }

    private Boolean verifyRepairs(DataFlowDiagramAndDictionary repairedDfd) {
        var resourceProvider = new DFDModelResourceProvider(repairedDfd.dataDictionary(), repairedDfd.dataFlowDiagram());
        var analysis = new DFDDataFlowAnalysisBuilder().standalone()
                .useCustomResourceProvider(resourceProvider)
                .build();
        analysis.initializeAnalysis();
        var flowGraph = analysis.findFlowGraphs();
        flowGraph.evaluate();

        for (var tfg : flowGraph.getTransposeFlowGraphs()) {
            if (checkConstraints(tfg))
                return false;
        }
        return true;
    }

    private boolean checkConstraints(AbstractTransposeFlowGraph tfg) {
        for (var constraint : constraints) {
            if (checkConstraint(tfg, constraint.literals())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkConstraint(AbstractTransposeFlowGraph tfg, List<Literal> constraint) {
        List<String> negativeLiterals = new ArrayList<>();
        List<String> positiveLiterals = new ArrayList<>();
        for (var literal : constraint) {
            if (literal.positive())
                positiveLiterals.add(literal.compositeLabel()
                        .toString());
            else
                negativeLiterals.add(literal.compositeLabel()
                        .toString());
        }

        for (var node : tfg.getVertices()) {

            Set<String> nodeLiterals = new HashSet<>();
            for (var nodeChar : node.getAllVertexCharacteristics()) {
                nodeLiterals.add(new NodeLabel(new Label(nodeChar.getTypeName(), nodeChar.getValueName())).toString());
            }
            for (var variables : node.getAllIncomingDataCharacteristics()) {
                for (var dataChar : variables.getAllCharacteristics()) {
                    nodeLiterals.add(new IncomingDataLabel(new Label(dataChar.getTypeName(), dataChar.getValueName())).toString());
                }
            }

            if (nodeLiterals.stream()
                    .anyMatch(positiveLiterals::contains)) {
                continue;
            } else if (!nodeLiterals.containsAll(negativeLiterals)) {
                continue;
            } else if (node.getAllIncomingDataCharacteristics()
                    .isEmpty()) {
                var missingLiterals = new HashSet<>(positiveLiterals);
                missingLiterals.removeAll(nodeLiterals);
                for (var lit : missingLiterals) {
                    if (!lit.contains("IncomingData"))
                        return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }
}
