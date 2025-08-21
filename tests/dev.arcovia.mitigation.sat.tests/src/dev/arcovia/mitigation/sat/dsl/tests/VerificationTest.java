package dev.arcovia.mitigation.sat.dsl.tests;

import dev.arcovia.mitigation.sat.*;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import dev.arcovia.mitigation.sat.dsl.tests.utility.DataLoader;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DFD2WebConverter;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.results.dfd.DFDExampleModelResult;
import org.dataflowanalysis.examplemodels.results.dfd.models.BranchingResult;
import org.dataflowanalysis.examplemodels.results.dfd.scenarios.CWANoViolation;
import org.dataflowanalysis.examplemodels.results.dfd.scenarios.CWAPersonalDataViolation;
import org.dataflowanalysis.examplemodels.results.dfd.scenarios.OnlineShopResult;
import org.dataflowanalysis.examplemodels.results.dfd.scenarios.SimpleOnlineShopResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class VerificationTest {

    private final Logger logger = Logger.getLogger(VerificationTest.class);

    private static DFDExampleModelResult exampleModelResult;
    private static DataFlowDiagramAndDictionary dfd;

    private static List<AnalysisConstraint> analysisConstraints;
    private static List<Constraint> constraints;
    private static Map<String, List<String>> variables;
    private static final Map<Label, Integer> costs = new HashMap<>();
    private static final Map<Label, Integer> minCosts = new HashMap<>();
    private static final Map<Label, Integer> labelRanking = new HashMap<>();

    @BeforeAll
    public static void setup() throws StandaloneInitializationException {

        exampleModelResult = new SimpleOnlineShopResult();

        dfd = DataLoader.loadDFDfromPath(exampleModelResult.getDataFlowDiagram(), exampleModelResult.getDataDictionary());
        analysisConstraints = exampleModelResult.getDSLConstraints();

        constraints = analysisConstraints.stream().map(it -> new CNFTranslation(it, dfd))
                .map(CNFTranslation::constructCNF)
                .flatMap(Collection::stream).toList();

        int max = 10;
        int min = 2;

        variables = DataLoader.variables(dfd);
        variables.forEach((key, values) -> values.forEach(value -> {
            var randomNumber = new Random().nextInt(max - min + 1) + min;
            costs.put(new Label(key, value), randomNumber);
            minCosts.put(new Label(key, value), randomNumber-1);
            labelRanking.put(new Label(key, value), new Random().nextInt(max - min + 1) + min);
        }));
    }

    @Test
    public void validate() throws ContradictionException, TimeoutException, IOException, StandaloneInitializationException {
        var dfdConverter = new DFD2WebConverter();

        List<Scalability> scalabilityValues = new ArrayList<>();
        var rankedCosts = getRankedCosts(labelRanking);

        var name = exampleModelResult.getModelName();

        logger.info("Model Name: " + name);
        logger.info("Constraints: " + constraints.toString());


        var costsTest = costs;
        var minCostsTest = minCosts;
        var labelRankingTest = labelRanking;
        var constraintsTest = constraints;
        var variablesTest = variables;

        var repairResult = runRepair(true, costs);
        var repairedDfdCosts = repairResult.repairedDfd();

        int amountClauses = extractClauseCount("testresults/aName.cnf");
        scalabilityValues.add(new Scalability(amountClauses,repairResult.runtimeInMilliseconds));

        dfdConverter.convert(repairedDfdCosts).save("testresults/",  name + "-repaired.json");

        assertTrue(new Mechanic(repairedDfdCosts,null, null).isViolationFree(repairedDfdCosts,constraints));

        repairResult = runRepair(false, rankedCosts);
        repairedDfdCosts = repairResult.repairedDfd();
        assertTrue(new Mechanic(repairedDfdCosts,null, null).isViolationFree(repairedDfdCosts,constraints));

        logger.info(scalabilityValues);
    }

    private record Scalability(
            int amountClause,
            long runtimeInMilliseconds
        ) {}
    
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
            int violationsBefore,
            int violationsAfter,
            long runtimeInMilliseconds
        ) {}

    private RepairResult runRepair(boolean store, Map<Label,Integer> costMap)
            throws ContradictionException, IOException, TimeoutException {
        var name = store ? exampleModelResult.getModelName() : "aName";
//        var name = exampleModelResult.getModelName();
//        Mechanic mechanic = new Mechanic(dfd, name, constraints, costMap);
        Mechanic mechanic = new Mechanic(dfd, name, constraints);
        long startTime = System.currentTimeMillis();
        var repairedDfd = mechanic.repair();
        long endTime = System.currentTimeMillis();
//        int violationsAfter = new Mechanic(repairedDfd,null, null).amountOfViolations(repairedDfd,constraints);
        int violationsAfter = new Mechanic(repairedDfd,null, null).amountOfViolations(repairedDfd,constraints);
        return new RepairResult(repairedDfd,mechanic.getViolations(),violationsAfter,endTime-startTime);
    }
    
    private Map<Label, Integer> getRankedCosts(Map<Label, Integer> rankedLabels) {
        int maxRank = rankedLabels.values().stream()
            .max(Integer::compareTo)
            .orElse(0);

        int[] fibs = fibonacciNumbers(maxRank);

        Map<Label, Integer> costMap = new HashMap<>();
        for (Map.Entry<Label, Integer> entry : rankedLabels.entrySet()) {
            costMap.put(entry.getKey(), fibs[entry.getValue()]);
        }

        return costMap;
    }

    private int[] fibonacciNumbers(int n) {
        int[] fibs = new int[Math.max(n + 1, 2)];
        fibs[0] = 0;
        fibs[1] = 1;

        for (int i = 2; i <= n; i++) {
            fibs[i] = fibs[i - 1] + fibs[i - 2];
        }

        return fibs;
    }
    
    @AfterEach
    void cleanup() throws IOException {
//        Files.deleteIfExists(Paths.get("testresults/aName-literalMapping.json"));
//        Files.deleteIfExists(Paths.get("testresults/aName.cnf"));
    }
}
