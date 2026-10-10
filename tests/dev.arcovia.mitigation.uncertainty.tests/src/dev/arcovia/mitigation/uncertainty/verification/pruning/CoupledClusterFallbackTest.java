package dev.arcovia.mitigation.uncertainty.verification.pruning;

import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDExternalUncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DfdFactory;
import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.uncertainty.RobustRepairResult;
import dev.arcovia.mitigation.uncertainty.UncertaintyAwareOptimizationManager;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationConstraints;
import dev.arcovia.mitigation.uncertainty.fixtures.TestModelBuilder;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import dev.arcovia.mitigation.uncertainty.pruning.SourceImpactFilter;
import dev.arcovia.mitigation.uncertainty.pruning.SourcePartitioner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.arcovia.mitigation.uncertainty.evaluation.EvaluationConstraints.PERSONAL_OUTSIDE_EU_MUST_BE_ENCRYPTED;
import static dev.arcovia.mitigation.uncertainty.evaluation.fixtures.RunningExampleFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The running example extended by a third source on a disjoint audit branch cluster check
 */
class CoupledClusterFallbackTest {

    @Test
    public void partitionerSeparatesAllThreeSourcesByFlowGraph() {
        var loaded = extendedRunningExample();
        var relevant = new SourceImpactFilter().retainImpactful(loaded.baseModel(), loaded.sources());

        var clusters = new SourcePartitioner().partition(loaded.baseModel(), relevant);

        assertEquals(3, relevant.size(), "every source targets a node on some flow graph");
        assertEquals(3, clusters.size(), "the three targets lie on three different flow graphs");
    }

    @Test
    public void sharedEncryptionCandidatesDisableTheDecompositionEntirely() throws Exception {
        var loaded = extendedRunningExample();
        var robust = new UncertaintyAwareOptimizationManager(loaded, constraints(), false, loaded.sources());
        robust.repair();
        var result = robust.getRobustRepairResult().orElseThrow();

        assertEquals(8, result.consideredScenarioCount(),
                "the fraud and recommendation clusters share the Encrypted candidates, so the pipeline must fall back to the full product");
        assertEquals(RobustRepairResult.ValidationStatus.PASSED, result.validationStatus(),
                "the undecomposed repair still holds in every scenario");
    }


    private static List<Constraint> constraints() {
        return List.of(EvaluationConstraints.withFlowRemoval(PERSONAL_OUTSIDE_EU_MUST_BE_ENCRYPTED));
    }

    private static LoadedUncertaintyModel extendedRunningExample() {
        var builder = new TestModelBuilder()
                .labelType("Sensitivity", "Personal")
                .labelType("Encryption", "Encrypted")
                .labelType("Location", "EU", "nonEU")
                .source(CUSTOMER, "Personal")
                .forwarder(CHECKOUT)
                .store(PAYMENT, "EU")
                .forwarder(ADAPTER)
                .store(FRAUD, "EU")
                .store(RECOMMENDATION, "EU")
                .source("AuditClient", "Personal")
                .store("AuditStore", "EU")
                .flow("checkout", CUSTOMER, CHECKOUT)
                .flow("payment", CHECKOUT, PAYMENT)
                .flow("telemetry", CHECKOUT, ADAPTER)
                .flow("fraud", ADAPTER, FRAUD)
                .flow("recommendation", ADAPTER, RECOMMENDATION)
                .flow("audit", "AuditClient", "AuditStore");
        return builder.build(List.of(
                locationSource(builder, FRAUD),
                locationSource(builder, RECOMMENDATION),
                locationSource(builder, "AuditStore")));
    }

    private static DFDExternalUncertaintySource locationSource(TestModelBuilder builder, String node) {
        var source = DfdFactory.eINSTANCE.createDFDExternalUncertaintySource();
        source.setId("u" + node);
        source.setEntityName(node + "_Location");
        source.setTarget(builder.nodeByName(node));
        source.getTargetProperties().add(builder.labelByName("EU"));
        var alternative = DfdFactory.eINSTANCE.createDFDExternalUncertaintyScenario();
        alternative.setId("s" + node + "_nonEU");
        alternative.setEntityName(node + "_nonEU");
        alternative.getTargetProperties().add(builder.labelByName("nonEU"));
        source.getScenarios().add(alternative);
        return source;
    }
}
