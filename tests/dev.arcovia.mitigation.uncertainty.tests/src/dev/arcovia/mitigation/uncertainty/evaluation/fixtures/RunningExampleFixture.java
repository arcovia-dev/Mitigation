package dev.arcovia.mitigation.uncertainty.evaluation.fixtures;

import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDExternalUncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DfdFactory;
import dev.arcovia.mitigation.uncertainty.fixtures.TestModelBuilder;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;

import java.util.List;

/**
 * The retail checkout example of the thesis, constructed in code.
 */
public final class RunningExampleFixture {

    public static final String CUSTOMER = "Customer";
    public static final String CHECKOUT = "CheckoutService";
    public static final String PAYMENT = "PaymentProcessor";
    public static final String ADAPTER = "TelemetryAdapter";
    public static final String FRAUD = "FraudAnalytics";
    public static final String RECOMMENDATION = "RecommendationAnalytics";
    public static final String FRAUD_SOURCE = "FraudAnalytics_Location";
    public static final String RECOMMENDATION_SOURCE = "RecommendationAnalytics_Location";

    private RunningExampleFixture() {
    }

    /**
     * Builds a fresh model with its two sources; every call returns independent objects.
     */
    public static LoadedUncertaintyModel build() {
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
                .flow("checkout", CUSTOMER, CHECKOUT)
                .flow("payment", CHECKOUT, PAYMENT)
                .flow("telemetry", CHECKOUT, ADAPTER)
                .flow("fraud", ADAPTER, FRAUD)
                .flow("recommendation", ADAPTER, RECOMMENDATION);
        return builder.build(
                List.of(
                        locationSource(builder, FRAUD, FRAUD_SOURCE),
                        locationSource(builder, RECOMMENDATION, RECOMMENDATION_SOURCE)
                )
        );
    }

    private static DFDExternalUncertaintySource locationSource(TestModelBuilder builder, String node, String name) {
        var source = DfdFactory.eINSTANCE.createDFDExternalUncertaintySource();
        source.setId("u" + node);
        source.setEntityName(name);
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
