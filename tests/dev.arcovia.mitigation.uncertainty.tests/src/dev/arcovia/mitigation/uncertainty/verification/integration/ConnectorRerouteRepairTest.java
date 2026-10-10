package dev.arcovia.mitigation.uncertainty.verification.integration;

import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDConnectorUncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DfdFactory;
import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.uncertainty.RobustRepairResult;
import dev.arcovia.mitigation.uncertainty.UncertaintyAwareOptimizationManager;
import dev.arcovia.mitigation.uncertainty.fixtures.TestModelBuilder;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import dev.arcovia.mitigation.uncertainty.pruning.TopologyEffect;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repairs a model whose only uncertainty is where a flow is routed.
 */
class ConnectorRerouteRepairTest {

    @Test
    public void theFixtureIsolatesConnectorUncertaintyAndIsSecondary() {
        var loaded = load();

        assertEquals(1, loaded.sources().size(), "the fixture declares one uncertainty source");
        assertInstanceOf(DFDConnectorUncertaintySource.class, loaded.sources().get(0), "the source must be a connector source, otherwise the fixture does not isolate the type");
        assertTrue(TopologyEffect.anyChangesTopology(loaded.sources()),
                "a connector source rewires the diagram, so both reductions must be disabled");
    }

    @Test
    public void reroutingIsRepairedUpstreamOfTheRerouteItself() throws Exception {
        var result = repair();

        assertEquals(RobustRepairResult.ValidationStatus.PASSED, result.validationStatus(),
                "one plan must hold whether the flow is routed to the EU store or the nonEU one");
        assertEquals(2, result.consideredScenarioCount(),
                "one binary source gives the default route and the alternative route");
        assertFalse(result.selectedActions().isEmpty(), "the nonEU route violates the constraint, so some action must be selected");
    }

    private RobustRepairResult repair() throws Exception {
        LoadedUncertaintyModel loaded = load();
        var manager = new UncertaintyAwareOptimizationManager(
                loaded, List.of(personalAtNonEuMustBeEncrypted()), false, loaded.sources());
        manager.repair();
        return manager.getRobustRepairResult().orElseThrow();
    }

    private static Constraint personalAtNonEuMustBeEncrypted() {
        return new Constraint(new ConstraintDSL().ofData()
                .withLabel("Sensitivity", "Personal")
                .withoutLabel("Encryption", "Encrypted")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Location", "nonEU")
                .create());
    }

    private LoadedUncertaintyModel load() {
        var builder = new TestModelBuilder()
                .labelType("Sensitivity", "Personal", "Public")
                .labelType("Location", "EU", "nonEU")
                .labelType("Encryption", "Encrypted")
                .source("User", "Personal")
                .forwarder("Router")
                .store("StoreEU", "EU")
                .store("StoreNonEU", "nonEU")
                .flow("fIn", "User", "Router")
                .flow("fRouted", "Router", "StoreEU");

        var source = DfdFactory.eINSTANCE.createDFDConnectorUncertaintySource();
        source.setId("uConnector");
        source.setEntityName("routing_destination_uncertain");
        source.setTargetFlow(builder.flowById("fRouted"));

        var alternative = DfdFactory.eINSTANCE.createDFDConnectorUncertaintyScenario();
        alternative.setId("sRouteNonEu");
        alternative.setTargetNode(builder.nodeByName("StoreNonEU"));
        alternative.setTargetPin(builder.inPinOf("StoreNonEU"));
        source.getScenarios().add(alternative);

        return builder.build(List.of(source));
    }
}
