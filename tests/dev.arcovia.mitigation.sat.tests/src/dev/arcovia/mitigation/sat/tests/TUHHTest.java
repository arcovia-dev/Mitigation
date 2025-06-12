package dev.arcovia.mitigation.sat.tests;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.arcovia.mitigation.sat.*;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.dfd2web.DFD2WebConverter;
import org.dataflowanalysis.examplemodels.Activator;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.AfterEach;
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
            entry(new Label("Stereotype", "gateway"), 4),
            entry(new Label("Stereotype", "authenticated_request"), 4), entry(new Label("Stereotype", "transform_identity_representation"), 3),
            entry(new Label("Stereotype", "token_validation"), 1), entry(new Label("Stereotype", "login_attempts_regulation"), 2),
            entry(new Label("Stereotype", "encrypted_connection"), 3), entry(new Label("Stereotype", "log_sanitization"), 2),
            entry(new Label("Stereotype", "local_logging"), 2));
    
    final Map<Label, Integer> rankingLabels= Map.ofEntries(entry(new Label("Stereotype", "internal"), 7),
            entry(new Label("Stereotype", "gateway"), 6),
            entry(new Label("Stereotype", "authenticated_request"), 6), entry(new Label("Stereotype", "transform_identity_representation"), 3),
            entry(new Label("Stereotype", "token_validation"), 1), entry(new Label("Stereotype", "login_attempts_regulation"), 4),
            entry(new Label("Stereotype", "encrypted_connection"), 5), entry(new Label("Stereotype", "log_sanitization"), 3),
            entry(new Label("Stereotype", "local_logging"), 2));

    @Test
    public void tuhhTest() throws ContradictionException, TimeoutException, IOException, StandaloneInitializationException {
        var dfdConverter = new DFD2WebConverter();

        var tuhhModels = TuhhModels.getTuhhModels();
        
        List<Scalability> scalabilityValues = new ArrayList<>();
        var rankedCosts = getRankedCosts(rankingLabels);

        for (var model : tuhhModels.keySet()) {
            for (int variant : tuhhModels.get(model)) {
                String name = model + "_" + variant;

                System.out.println(name);

                var repairResult = runRepair(model, name, variant == 0, constraints, costs);
                var repairedDfdCosts = repairResult.repairedDfd();
                
                int amountClauses = extractClauseCount("testresults/" +  (variant == 0 ? name : "aName") + ".cnf");
                scalabilityValues.add(new Scalability(amountClauses,repairResult.runtimeInMilliseconds));

                if (variant == 0)
                    dfdConverter.convert(repairedDfdCosts).save("testresults/",  name + "-repaired.json");

                assertTrue(new Mechanic(repairedDfdCosts,null, null).isViolationFree(repairedDfdCosts,constraints));
                
                repairResult = runRepair(model, name, false , constraints, rankedCosts);
                repairedDfdCosts = repairResult.repairedDfd();
                assertTrue(new Mechanic(repairedDfdCosts,null, null).isViolationFree(repairedDfdCosts,constraints));
                
            }
        }
        
        System.out.println(scalabilityValues);
    }
    
    
    private record Scalability(
            int amountClause,
            long runtimeInMilliseconds
        ) {}
    
    @Test
    void efficiencyTest() throws ContradictionException, TimeoutException, IOException, StandaloneInitializationException {
        final Map<Label, Integer> costMap = Map.ofEntries(
                entry(new Label("Stereotype", "gateway"), 4),
                entry(new Label("Stereotype", "authenticated_request"), 4), entry(new Label("Stereotype", "transform_identity_representation"), 3),
                entry(new Label("Stereotype", "token_validation"), 1), entry(new Label("Stereotype", "login_attempts_regulation"), 2),
                entry(new Label("Stereotype", "encrypted_connection"), 3), entry(new Label("Stereotype", "log_sanitization"), 2),
                entry(new Label("Stereotype", "local_logging"), 2));
        
        final Map<Label, Integer> minMap = Map.ofEntries(
                entry(new Label("Stereotype", "gateway"), 1),
                entry(new Label("Stereotype", "authenticated_request"), 1), entry(new Label("Stereotype", "transform_identity_representation"), 1),
                entry(new Label("Stereotype", "token_validation"), 1), entry(new Label("Stereotype", "login_attempts_regulation"), 1),
                entry(new Label("Stereotype", "encrypted_connection"), 1), entry(new Label("Stereotype", "log_sanitization"), 1),
                entry(new Label("Stereotype", "local_logging"), 1));
        
        var tuhhModels = TuhhModels.getTuhhModels();
        List<String> modelRepairMoreExpensive = new ArrayList<>();
        
        Map<String,Integer> tuhhCosts = new LinkedHashMap<>();
        Map<String,Integer> satCosts = new LinkedHashMap<>();
        Map<String,Integer> violationsBefore = new LinkedHashMap<>();
        List<Scalability> scalabilityValues = new ArrayList<>();

        for (var model : tuhhModels.keySet()) {
            if (!tuhhModels.get(model).contains(0)) continue;
            
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
                var repairResult = runRepair(model, model+"_0", false, constraint, minMap);
                var repairedDfd = repairResult.repairedDfd();
                var dfdConverter = new DFD2WebConverter();
                dfdConverter.convert(repairedDfd).save("efficencyTest/",  model + "_" + variant + "-repaired.json");
                var satCost = new ModelCostCalculator(repairedDfd, constraint, minMap).calculateCost();
                var tuhhCost = new ModelCostCalculator(loadDFD(model, model + "_" + variant), constraint, minMap).calculateCost();

                System.out.println(satCost + " <= " + tuhhCost + " : "+ (satCost <= tuhhCost));
                if (satCost > tuhhCost){
                    modelRepairMoreExpensive.add(model + "_" + variant);
                }
                
                satCosts.put(model + "_" + variant, satCost);
                tuhhCosts.put(model + "_" + variant, tuhhCost);
                violationsBefore.put(model + "_" + variant, repairResult.amountViolations());
                
                int amountClauses = extractClauseCount("testresults/aName.cnf");
                scalabilityValues.add(new Scalability(amountClauses,repairResult.runtimeInMilliseconds));
            }
        }
        
        System.out.println(satCosts.values());
        System.out.println(tuhhCosts.values());
        System.out.println(violationsBefore);
        System.out.println(scalabilityValues);
        
        assertEquals(modelRepairMoreExpensive, List.of("callistaenterprise_2", "apssouza22_4", "apssouza22_7"));
    }

    @Disabled
    @Test
    void specificTUHHTest() throws ContradictionException, TimeoutException, IOException, StandaloneInitializationException {
        var dfdConverter = new DFD2WebConverter();
        String model = "mudigal-technologies";
        int variant = 7;

        String name = model + "_" + variant;
        dfdConverter.convert(loadDFD(model,name)).save("testresults/",  "specific_" + name + "-repaired.json");

        var repairedDfdCosts = runRepair(model, name, true, List.of(encryptedEntry, entryViaGatewayOnly, nonInternalGateway), costs).repairedDfd();
        dfdConverter.convert(repairedDfdCosts).save("testresults/",  "specific_" + name + "-repaired.json");
        assertTrue(new Mechanic(repairedDfdCosts,null, null).isViolationFree(repairedDfdCosts,constraints));
    }
    
    private int extractClauseCount(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String firstLine = reader.readLine();
            if (firstLine != null && firstLine.startsWith("p cnf")) {
                String[] parts = firstLine.trim().split("\\s+");
                if (parts.length == 4) {
                    return Integer.parseInt(parts[3]);
                }
            }
        }
        throw new IllegalArgumentException("First line is not in the expected 'p cnf <vars> <clauses>' format.");
    }
    
    private record RepairResult(
            DataFlowDiagramAndDictionary repairedDfd,
            int amountViolations,
            long runtimeInMilliseconds
        ) {}

    private RepairResult runRepair(String model, String name, Boolean store, List<Constraint> constraints, Map<Label,Integer> costMap)
            throws StandaloneInitializationException, ContradictionException, IOException, TimeoutException {
        var dfd = loadDFD(model, name);
        if (!store)
            name = "aName";
        Mechanic mechanic = new Mechanic(dfd, name, constraints, costMap);
        long startTime = System.currentTimeMillis();
        var repairedDfd = mechanic.repair();
        long endTime = System.currentTimeMillis();
        return new RepairResult(repairedDfd,mechanic.getViolations(),endTime-startTime);
    }
    
    private Map<Label, Integer> getRankedCosts(Map<Label, Integer> rankedLabels){
    	Map<Label, Integer> costMap = new HashMap<>();

        for (Map.Entry<Label, Integer> entry : rankedLabels.entrySet()) {
        	Label label = entry.getKey();
            int rank = entry.getValue();
            int cost = fibonacci(rank);
            costMap.put(label, cost);
        }

        return costMap;
    }
    
    private static int fibonacci(int n) {
        if (n <= 0) return 0;
        if (n == 1) return 1;

        int a = 0, b = 1, c = 1;
        for (int i = 2; i <= n; i++) {
            c = a + b;
            a = b;
            b = c;
        }
        return c;
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
    
    @AfterEach
    void cleanup() throws IOException {
        Files.deleteIfExists(Paths.get("testresults/aName-literalMapping.json"));
        Files.deleteIfExists(Paths.get("testresults/aName.cnf"));
    }
}
