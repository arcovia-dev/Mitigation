package dev.arcovia.mitigation.smt.tests.smt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.Mitigation;
import dev.arcovia.mitigation.smt.SolvingResult;
import dev.arcovia.mitigation.smt.tests.evaluation.ConstraintMapProvider;
import dev.arcovia.mitigation.smt.utils.ParsingUtils;

public class SMTTest {

    @Test
    public void testEndToEnd() throws Exception {
        var tuhhModels = TuhhModels.getTuhhModels();

        Map<Integer, List<AnalysisConstraint>> constraintMap = ConstraintMapProvider.buildConstraintMap();
        for (var model : tuhhModels.keySet()) {
            if (!tuhhModels.get(model)
                    .contains(0))
                continue;
            for (int i : List.of(1, 2, 4, 5, 7, 8, 10, 11)) {
                List<AnalysisConstraint> constraint = constraintMap.get(i);
                if (constraint == null) {
                    continue;
                } else if (!tuhhModels.get(model)
                        .contains(i)) {
                    continue;
                }
                DataFlowDiagramAndDictionary dfd = ParsingUtils.loadDFD(model, model + "_0");
                long before = System.currentTimeMillis();
                SolvingResult solvingResult = Mitigation.run(dfd, constraint, null);
                long after = System.currentTimeMillis();
                long totalTime = after - before;
                int cost = solvingResult.repairCost();
                int referenceCost = REFERENCE.get(model)
                        .get(i);
                // All TUHH Models should be solvable
                assertTrue(solvingResult.satisfiable());
                // Assert that actually found cost is equal to the lowest one we ever found
                assertEquals(cost, referenceCost);
                // Assert that the amount of operations is equal to the cost (uniform cost)
                assertEquals(solvingResult.repairOperations()
                        .size(), cost);
                // Assert no violations after
                int violationsAfter = ParsingUtils.countViolations(solvingResult.repairedDFD(), constraint);
                assertTrue(violationsAfter == 0);

                // Assert that non-configured optionals are not set
                assertTrue(solvingResult.expressionTreeSize()
                        .isEmpty());
                assertTrue(solvingResult.violationsAfter()
                        .isEmpty());

                // Assert that solving only didnt take longer than complete computation
                // (solverTimeMs integrity check)
                assertTrue(solvingResult.solverTimeMs() <= totalTime);

            }
        }
    }

    private static final Map<String, Map<Integer, Integer>> REFERENCE = Map.ofEntries(Map.entry("ewolff-kafka", Map.of(4, 1, 5, 1, 7, 2, 8, 1)),

            Map.entry("anilallewar", Map.of(7, 6, 8, 4, 11, 4)),

            Map.entry("mudigal-technologies", Map.of(2, 2, 4, 2, 5, 2, 7, 2, 8, 5, 11, 3)),

            Map.entry("yidongnan", Map.of(2, 2, 4, 1, 5, 1, 7, 3, 8, 4)),

            Map.entry("koushikkothagal", Map.of(1, 0, 2, 2, 4, 0, 5, 0, 7, 0, 8, 4, 10, 3, 11, 3)),

            Map.entry("spring-petclinic", Map.of(2, 2, 5, 2, 7, 2, 8, 2)),

            Map.entry("callistaenterprise", Map.of(2, 3, 11, 9)),

            Map.entry("jferrater", Map.of(2, 1, 5, 0, 7, 0, 8, 1)),

            Map.entry("apssouza22", Map.of(2, 3, 4, 1, 7, 5, 8, 5)),

            Map.entry("sqshq", Map.of(7, 3, 8, 10, 10, 0, 11, 4)),

            Map.entry("georgwittberger", Map.of(2, 1, 4, 1, 5, 1, 7, 2, 8, 1, 10, 3, 11, 3)));

}
