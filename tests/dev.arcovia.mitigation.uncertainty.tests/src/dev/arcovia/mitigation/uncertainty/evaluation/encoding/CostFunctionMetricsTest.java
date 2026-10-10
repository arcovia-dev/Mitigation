package dev.arcovia.mitigation.uncertainty.evaluation.encoding;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.arcovia.mitigation.cost.ActionType;
import dev.arcovia.mitigation.cost.RepairActionKey;
import dev.arcovia.mitigation.cost.RepairCostSpecification;
import dev.arcovia.mitigation.cost.SharedCostGroup;
import dev.arcovia.mitigation.ilp.ActionTerm;
import dev.arcovia.mitigation.ilp.ILPSolver;
import dev.arcovia.mitigation.ilp.Mitigation;
import dev.arcovia.mitigation.ilp.RepairActionKeys;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationArtifacts;
import dev.arcovia.mitigation.uncertainty.evaluation.RecordCsv;
import dev.arcovia.mitigation.uncertainty.helper.RecordTableRenderer;
import dev.arcovia.mitigation.uncertainty.helper.ReportColumn;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the cost ILP against exhaustively enumerated possible action subsets and records
 * the comparison.
 */
class CostFunctionMetricsTest {

    private static final Logger LOGGER = Logger.getLogger(CostFunctionMetricsTest.class);

    private static final double EPSILON = 1e-9;

    @Test
    void matchesTheExhaustiveOracleOnEveryFixture() throws Exception {
        for (var fixture : fixtures()) {
            var row = observe(fixture);
            assertEquals(fixture.expectedMinimum(), row.oracleMinimum(), EPSILON, fixture.caseId() + ": the oracle must reach the minimum the fixture declares");
            assertEquals(row.oracleMinimum(), row.objective(), EPSILON, fixture.caseId() + ": the ILP objective must equal the enumerated minimum");
            assertEquals(row.objective(), row.directCost() + row.sharedCost(), EPSILON, fixture.caseId() + ": the objective must be its own direct and shared parts");
            assertEquals(fixture.expectedOptima(), row.optimalPlans().size(), fixture.caseId() + ": the fixture declares how many action sets reach the minimum");
            assertTrue(row.optimalPlans().contains(row.selectedActions()), fixture.caseId() + ": the selected action set must be one the oracle found optimal");
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "arcovia.evaluation", matches = "true",
            disabledReason = "opt-in evaluation run; set -Darcovia.evaluation=true")
    void persistsIndependentCostFunctionEvidence() throws Exception {
        var rows = new ArrayList<CostOracleRow>();
        for (Fixture fixture : fixtures()) {
            rows.add(observe(fixture));
        }
        var directory = EvaluationArtifacts.createRunDirectory(Path.of(System.getProperty("user.dir")),
                "cost-function-oracle");
        EvaluationArtifacts.write(directory, "report.txt", report(rows));
        EvaluationArtifacts.write(directory, "summary.csv", RecordCsv.table(CostOracleRow.class, rows));
        EvaluationArtifacts.writeJsonLines(directory, "raw-records.jsonl",
                rows.stream().map(EvaluationArtifacts::toJson).toList());
        LOGGER.info("Evaluation artefacts: " + directory);
    }


    /**
     * Builds the six fixtures, each discriminating one part of the objective.
     *
     * @return the direct-cost, tie, prerequisite-and-contradiction, and shared-cost fixtures
     */
    private static List<Fixture> fixtures() {
        var tieEncrypt = mitigation("tie-encrypt", ActionType.Adding, List.of());
        var tieRemove = mitigation("tie-remove", ActionType.RemoveNode, List.of());

        var prerequisite = mitigation("key-store", ActionType.AddSink, List.of());
        var guarded = mitigation("guarded-encrypt", ActionType.Adding, List.of(List.of(prerequisite)));
        var alternative = mitigation("remove", ActionType.RemoveNode, List.of());
        var prerequisiteSpecification = RepairCostSpecification.builder("exhaustive-oracle", "1")
                .withBaseCost(ActionType.Adding, 1)
                .withBaseCost(ActionType.AddSink, 10)
                .withBaseCost(ActionType.RemoveNode, 6)
                .build();

        var first = mitigation("shared-first", ActionType.Adding, List.of());
        var second = mitigation("shared-second", ActionType.Adding, List.of());
        var sharedSpecification = RepairCostSpecification.builder("shared-oracle", "1")
                .withBaseCost(ActionType.Adding, 1)
                .withSharedCostGroup(new SharedCostGroup("key-management", 5,
                        Set.of(RepairActionKeys.from(first), RepairActionKeys.from(second))))
                .build();

        return List.of(
                direct("direct-encrypt-preferred", 1, 4, 1),
                direct("direct-remove-preferred", 20, 6, 6),
                direct("direct-encrypt-cheaper", 5, 6, 5),
                new Fixture("tied-optimum", List.of(tieEncrypt, tieRemove),
                        List.of(List.of(tieEncrypt, tieRemove)), List.of(), profile(5, 5), 5, 2),
                new Fixture("prerequisite-and-contradiction", List.of(prerequisite, guarded, alternative),
                        List.of(List.of(guarded, alternative)), List.of(List.of(guarded, alternative)),
                        prerequisiteSpecification, 6, 1),
                new Fixture("shared-cost", List.of(first, second),
                        List.of(List.of(first), List.of(second)), List.of(), sharedSpecification, 7, 1));
    }

    /**
     * Builds a two-action fixture whose only choice is between an addition and a removal.
     *
     * @param caseId          the stable fixture identifier
     * @param encryptCost     the direct adding cost
     * @param removeCost      the direct node-removal cost
     * @param expectedMinimum the cheaper of the two
     * @return the fixture
     */
    private static Fixture direct(String caseId, double encryptCost, double removeCost, double expectedMinimum) {
        var encrypt = mitigation(caseId + "-encrypt", ActionType.Adding, List.of());
        var remove = mitigation(caseId + "-remove", ActionType.RemoveNode, List.of());
        var candidates = List.of(encrypt, remove);
        return new Fixture(caseId, candidates, List.of(candidates), List.of(), profile(encryptCost, removeCost), expectedMinimum, 1);
    }


    /**
     * Solves one fixture both ways and pairs the results.
     *
     * @param fixture the bounded fixture
     * @return the solver result beside its independently enumerated optimum
     * @throws Exception if SCIP cannot solve the fixture
     */
    private static CostOracleRow observe(Fixture fixture) throws Exception {
        var solver = new ILPSolver().solveWithResult(fixture.coverage(), Set.copyOf(fixture.candidates()),
                fixture.contradictions(), fixture.specification(), null);
        var oracle = findCheapestActionSubset(fixture);
        var breakdown = solver.costBreakdown();
        var selected = actionKeys(solver.selectedMitigations()).stream().map(Object::toString).sorted().toList();
        return new CostOracleRow(
                fixture.caseId(),
                breakdown.specificationId(),
                breakdown.specificationVersion(),
                breakdown.objectiveValue(),
                breakdown.directActionCost(),
                breakdown.sharedEnablerCost(),
                selected.size(),
                selected,
                oracle.minimumObjective(),
                oracle.optimalPlans().stream()
                        .map(plan -> plan.stream().map(Object::toString).sorted().toList())
                        .toList());
    }

    /**
     * Finds the cheapest possible action subset by trying all of them.
     *
     * @param fixture the bounded fixture
     * @return the exact minimum and every plan that reaches it
     */
    private static OracleResult findCheapestActionSubset(Fixture fixture) {
        var minimum = Double.POSITIVE_INFINITY;
        var optimalPlans = new ArrayList<Set<RepairActionKey>>();
        for (Set<Mitigation> selected : subsets(fixture.candidates())) {
            if (!feasible(selected, fixture.coverage(), fixture.contradictions())) {
                continue;
            }
            var objective = cost(selected, fixture.specification());
            if (objective < minimum - EPSILON) {
                minimum = objective;
                optimalPlans.clear();
            }
            if (objective <= minimum + EPSILON) {
                optimalPlans.add(actionKeys(selected));
            }
        }
        return new OracleResult(minimum, List.copyOf(optimalPlans));
    }

    /**
     * Builds every subset of the candidates, including the empty one.
     *
     * @param candidates the available actions
     * @return all subsets, each an independent set
     */
    private static List<Set<Mitigation>> subsets(List<Mitigation> candidates) {
        var subsets = new ArrayList<Set<Mitigation>>();
        subsets.add(Set.of());
        candidates.forEach(candidate -> List.copyOf(subsets).stream().map(LinkedHashSet::new).forEach(extended -> {
            extended.add(candidate);
            subsets.add(extended);
        }));
        return List.copyOf(subsets);
    }

    /**
     * Prices one action subset under the declared costs.
     *
     * @param selected      the action subset
     * @param specification the declared costs
     * @return the objective value of that subset
     */
    private static double cost(Set<Mitigation> selected, RepairCostSpecification specification) {
        var directCost = selected.stream()
                .map(RepairActionKeys::from)
                .mapToDouble(key -> specification.actionCostOverrideFor(key)
                        .orElse(specification.baseCostFor(key.actionType())))
                .sum();
        var chargedGroups = actionKeys(selected);
        var sharedCost = specification.sharedCostGroups().stream()
                .filter(group -> group.memberActions().stream().anyMatch(chargedGroups::contains))
                .mapToDouble(SharedCostGroup::fixedCost)
                .sum();
        return directCost + sharedCost;
    }

    /**
     * Tests coverage, contradiction, and prerequisite constraints for one action subset.
     *
     * @param selected       the action subset
     * @param coverage       the coverage clauses
     * @param contradictions incompatible action groups
     * @return {@code true} when the subset is feasible
     */
    private static boolean feasible(Set<Mitigation> selected, List<List<Mitigation>> coverage,
                                    List<List<Mitigation>> contradictions) {
        var coversEveryViolation = coverage.stream().allMatch(clause ->
                clause.stream().anyMatch(selected::contains));
        var avoidsContradictions = contradictions.stream().noneMatch(group ->
                group.stream().filter(selected::contains).count() > 1);
        var meetsRequirements = selected.stream().allMatch(mitigation ->
                mitigation.required().isEmpty() || mitigation.required().stream().anyMatch(selected::containsAll));
        return coversEveryViolation && avoidsContradictions && meetsRequirements;
    }

    /**
     * Returns canonical keys for a mitigation collection.
     *
     * @param mitigations the selected mitigations
     * @return their canonical keys
     */
    private static Set<RepairActionKey> actionKeys(Collection<Mitigation> mitigations) {
        return mitigations.stream().map(RepairActionKeys::from).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Creates one action candidate.
     *
     * @param domain   the action domain
     * @param type     the action type
     * @param required the prerequisite alternatives
     * @return a mitigation with a legacy score these fixtures do not use
     */
    private static Mitigation mitigation(String domain, ActionType type, List<List<Mitigation>> required) {
        return new Mitigation(new ActionTerm(domain,
                List.of(new NodeLabel(new Label("Encrypted", "true"))), type), 99, required);
    }

    /**
     * Builds a direct-cost profile without descriptors or shared groups.
     *
     * @param encryptCost the direct adding cost
     * @param removeCost  the direct node-removal cost
     * @return the bounded fixture profile
     */
    private static RepairCostSpecification profile(double encryptCost, double removeCost) {
        return RepairCostSpecification.builder("exhaustive-oracle", "1")
                .withBaseCost(ActionType.Adding, encryptCost)
                .withBaseCost(ActionType.RemoveNode, removeCost)
                .build();
    }

    /**
     * Renders the readable report, taking its heading from the row components.
     *
     * @param rows the measured fixtures
     * @return the complete report text
     */
    private static String report(List<CostOracleRow> rows) {
        return "Cost Function oracle evaluation\n" + "\n" + RecordTableRenderer.render(rows, CostOracleRow.class);
    }

    private record OracleResult(double minimumObjective, List<Set<RepairActionKey>> optimalPlans) {
    }

    /**
     * One measured fixture, and the single schema of the report, the CSV, and the JSON Lines.
     *
     * @param caseId              fixture identifier
     * @param costProfileId       declared cost profile
     * @param costProfileVersion  cost profile version
     * @param objective           the solver objective
     * @param directCost          direct action cost
     * @param sharedCost          shared-enabler cost
     * @param selectedActionCount number of selected actions
     * @param selectedActions     the selected canonical action keys
     * @param oracleMinimum       the independently enumerated minimum
     * @param optimalPlans        every action set reaching that minimum
     */
    private record CostOracleRow(
            @JsonProperty("case") @ReportColumn(value = "case", alignment = ReportColumn.Alignment.LEFT) String caseId,
            @ReportColumn(value = "cost profile", alignment = ReportColumn.Alignment.LEFT) String costProfileId,
            @ReportColumn("version") String costProfileVersion,
            @ReportColumn("objective") double objective,
            @ReportColumn("direct") double directCost,
            @ReportColumn("shared") double sharedCost,
            @ReportColumn("actions") int selectedActionCount,
            @ReportColumn(value = "selected", alignment = ReportColumn.Alignment.LEFT) List<String> selectedActions,
            @ReportColumn("oracle min") double oracleMinimum,
            @ReportColumn(value = "optimal plans", alignment = ReportColumn.Alignment.LEFT) List<List<String>> optimalPlans) {
    }

    /**
     * One bounded fixture, together with the optimum it is built to have.
     *
     * @param caseId          the stable fixture identifier
     * @param candidates      the available actions
     * @param coverage        the coverage clauses
     * @param contradictions  incompatible action groups
     * @param specification   the declared costs
     * @param expectedMinimum the minimum a correct encoding reaches
     * @param expectedOptima  how many action sets reach it, above one for a deliberate tie
     */
    private record Fixture(
            String caseId,
            List<Mitigation> candidates,
            List<List<Mitigation>> coverage,
            List<List<Mitigation>> contradictions,
            RepairCostSpecification specification,
            double expectedMinimum,
            int expectedOptima) {
    }
}
