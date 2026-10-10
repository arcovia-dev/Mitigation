package dev.arcovia.mitigation.uncertainty.verification.integration;

import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.arcovia.mitigation.cost.ActionType;
import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.ilp.MitigationStrategy;
import dev.arcovia.mitigation.ilp.MitigationType;
import dev.arcovia.mitigation.ilp.OptimizationManager;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.uncertainty.UncertaintyAwareOptimizationManager;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelLoader;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelSpec;
import dev.arcovia.mitigation.uncertainty.pipeline.ScenarioPreparation;
import dev.arcovia.mitigation.uncertainty.pipeline.ScenarioPreparationService;
import dev.arcovia.mitigation.uncertainty.solving.NoRobustRepairExistsException;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RobustRepairPipelineTest {

    private static final String BANKING_LOCATION = "Banking_Data_Location_Uncertain";

    private final Path models = Path.of(System.getProperty("user.dir")).resolve("models");

    @Test
    public void emptyUncertaintyDelegatesToCurrentIlpRepair() throws Exception {
        String minDfd = models.resolve("minsat.json").toString();
        var baseline = new OptimizationManager(minDfd, List.of(deleteInternalNode()), false);
        var baselineResult = baseline.repair();
        var robust = new UncertaintyAwareOptimizationManager(minDfd, List.of(deleteInternalNode()), false);
        var robustResult = robust.repair();

        assertTrue(baseline.isViolationFree(baselineResult));
        assertTrue(robust.isViolationFree(robustResult));
        assertEquals(baseline.getCost(), robust.getCost());
        assertEquals(baselineResult.dataFlowDiagram().getNodes().size(), robustResult.dataFlowDiagram().getNodes().size());
        assertEquals(baselineResult.dataFlowDiagram().getFlows().size(), robustResult.dataFlowDiagram().getFlows().size());
    }

    @Test
    public void surfacesNoRobustRepairExistsForInfeasibleRepair() {
        var loaded = load("DFDExternalUncertaintyMitigation", "ext");
        var sources = sourcesNamed(loaded, BANKING_LOCATION);
        var withoutMitigations = new Constraint(personalToNonEu(), List.of());
        assertTrue(violated(prepare(loaded.baseModel(), sources, withoutMitigations)) > 0);

        var manager = new UncertaintyAwareOptimizationManager(loaded, List.of(withoutMitigations), false, sources);
        var exception = assertThrows(NoRobustRepairExistsException.class, manager::repair);

        assertTrue(exception.getMessage().contains("No robust repair exists"));
        assertTrue(exception.solverStatus().isPresent());
    }

    @Test
    public void repairsTheViolatingDefaultOfOneSource() throws Exception {
        var loaded = load("DFDExternalUncertaintyMitigation", "ext");
        var sources = sourcesNamed(loaded, BANKING_LOCATION);
        var constraint = new Constraint(personalToNonEu());

        var before = prepare(loaded.baseModel(), sources, constraint);
        assertEquals(2, before.size(), "one binary source spans two scenarios");
        assertEquals(1, violated(before), "only the default Banking_DB=nonEU scenario violates");

        var after = prepare(repair(loaded, sources, constraint), sources, constraint);
        assertEquals(2, after.size());
        assertEquals(0, violated(after), "the repair must hold in both scenarios");
    }

    @Test
    public void repairsTheCartesianProductOfTwoSources() throws Exception {
        var loaded = load("DFDExternalUncertaintyMitigation", "ext");
        var sources = sourcesNamed(loaded, BANKING_LOCATION, "User_Location_Uncertain");
        var constraint = new Constraint(personalToNonEu());

        var before = prepare(loaded.baseModel(), sources, constraint);
        assertEquals(4, before.size(), "two binary sources span 2 x 2 scenarios");
        assertEquals(2, violated(before), "exactly the two Banking=nonEU combinations violate");
        assertTrue(before.stream().filter(RobustRepairPipelineTest::isViolated)
                        .allMatch(preparation -> preparation.scenario().id().contains(BANKING_LOCATION + ":default")),
                "every violated scenario keeps Banking_DB at its nonEU default");

        var after = prepare(repair(loaded, sources, constraint), sources, constraint);
        assertEquals(4, after.size());
        assertEquals(0, violated(after), "the repair must hold in all four scenarios");
    }

    @Test
    public void deletesBothUncertainNodesWhenOnlyDeletionIsOffered() throws Exception {
        var loaded = load("DFDExternalUncertaintyMitigation", "ext");
        var sources = sourcesNamed(loaded, BANKING_LOCATION, "Transactions_DB_Location_Uncertain");
        var deleteNonEuNode = new Constraint(personalToNonEu(), List.of(new MitigationStrategy(
                List.of(new NodeLabel(new Label("Location", "nonEU"))), 1, MitigationType.DeleteNode)));

        var before = prepare(loaded.baseModel(), sources, deleteNonEuNode);
        assertEquals(4, before.size(), "two binary sources span 2 x 2 scenarios");
        assertEquals(3, violated(before), "only Banking=EU, Transactions=EU is clean");

        var manager = new UncertaintyAwareOptimizationManager(loaded, List.of(deleteNonEuNode), false, sources);
        var repaired = manager.repair();
        var actions = manager.getRobustRepairResult().orElseThrow().selectedActions();
        assertEquals(2, actions.size(), "Banking_DB and Transactions_DB are both deleted");
        assertTrue(actions.stream().allMatch(action -> action.type() == ActionType.RemoveNode),
                "only node deletion is offered");

        var after = prepare(repaired, sources, deleteNonEuNode);
        assertEquals(4, after.size());
        assertEquals(0, violated(after),
                "the deletion must hold in every scenario, including those where the deleted node was the target");
    }

    @Test
    public void repairsAcrossAnInterfaceSource() throws Exception {
        var loaded = load("DFDInterfaceUncertaintyMitigation", "int");
        var sources = sourcesNamed(loaded, "loan_req_uncertain");
        assertEquals(1, sources.size(), "the fixture declares loan_req_uncertain once");
        var constraint = new Constraint(personalToNonEu());

        // The violating stores are not on the rerouted loan flow, so both routes violate.
        var before = prepare(loaded.baseModel(), sources, constraint);
        assertEquals(2, before.size(), "one interface source with one alternative spans two scenarios");
        assertEquals(2, violated(before), "both scenarios carry violations from the always-nonEU stores");

        var repaired = new UncertaintyAwareOptimizationManager(loaded, List.of(constraint), false).repair();
        var after = prepare(repaired, sources, constraint);
        assertEquals(2, after.size());
        assertEquals(0, violated(after), "the repair must hold on both routes");
    }

    @Test
    public void robustRepairLeavesCallerInputModelUntouched() throws Exception {
        var loaded = load("DFDExternalUncertaintyMitigation", "ext");
        var sources = sourcesNamed(loaded, BANKING_LOCATION);
        var constraint = new Constraint(personalToNonEu());
        long violatedBefore = violated(prepare(loaded.baseModel(), sources, constraint));
        assertTrue(violatedBefore > 0, "the base model must be violated before repair");

        var repaired = repair(loaded, sources, constraint);

        assertNotSame(loaded.baseModel(), repaired, "the repaired model must be a separate artefact");
        assertEquals(violatedBefore, violated(prepare(loaded.baseModel(), sources, constraint)),
                "the caller's input model must remain untouched by the repair");
        assertEquals(0, violated(prepare(repaired, sources, constraint)),
                "the returned model must be violation-free in every scenario");
    }

    private static DataFlowDiagramAndDictionary repair(LoadedUncertaintyModel loaded, List<UncertaintySource> sources,
                                                       Constraint constraint) throws Exception {
        return new UncertaintyAwareOptimizationManager(loaded, List.of(constraint), false, sources).repair();
    }

    private static List<ScenarioPreparation> prepare(DataFlowDiagramAndDictionary model,
                                                     List<UncertaintySource> sources, Constraint constraint) {
        return new ScenarioPreparationService().prepare(model, sources, List.of(constraint));
    }

    private static boolean isViolated(ScenarioPreparation preparation) {
        return !preparation.preparation().violatingNodes().isEmpty();
    }

    private static long violated(List<ScenarioPreparation> preparations) {
        return preparations.stream().filter(RobustRepairPipelineTest::isViolated).count();
    }

    private LoadedUncertaintyModel load(String folder, String prefix) {
        Path directory = models.resolve(folder);
        return new UncertaintyModelLoader().load(new UncertaintyModelSpec(
                directory.resolve(prefix + ".dataflowdiagram"),
                directory.resolve(prefix + ".datadictionary"),
                directory.resolve(prefix + ".uncertainty")));
    }

    private static List<UncertaintySource> sourcesNamed(LoadedUncertaintyModel model, String... names) {
        List<String> wanted = List.of(names);
        return model.sources().stream().filter(source -> wanted.contains(source.getEntityName())).toList();
    }

    private static AnalysisConstraint personalToNonEu() {
        return new ConstraintDSL().ofData()
                .withLabel("Sensitivity", "Personal")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Location", "nonEU")
                .create();
    }

    private static Constraint deleteInternalNode() {
        AnalysisConstraint dsl = new ConstraintDSL().ofData()
                .withLabel("Sensitivity", "Personal")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Stereotype", "internal")
                .create();
        return new Constraint(dsl, List.of(new MitigationStrategy(
                List.of(new NodeLabel(new Label("Stereotype", "internal"))), 1, MitigationType.DeleteNode)));
    }
}
