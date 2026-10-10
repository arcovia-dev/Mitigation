package dev.arcovia.mitigation.uncertainty.evaluation.fixtures;

import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDExternalUncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DfdFactory;
import dev.arcovia.mitigation.uncertainty.fixtures.TestModelBuilder;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Two subsystems that share no node, flow, or pin, each carrying two external uncertainty sources.
 */
public final class TwoSubsystemsFixture {

    private TwoSubsystemsFixture() {
    }

    /**
     * Builds the model with its four uncertainty sources.
     *
     * @return the loaded model the repair pipeline consumes
     */
    public static LoadedUncertaintyModel build() {
        TestModelBuilder builder = new TestModelBuilder()
                .labelType("Sensitivity", "Personal", "Public")
                .labelType("Location", "EU", "nonEU")
                .labelType("Encryption", "Encrypted");

        final List<String> list = List.of("A", "B");
        list.forEach(subsystem -> builder.source("User" + subsystem, "Personal")
                .nodeLabels("User" + subsystem, "EU")
                .forwarder("Proc" + subsystem)
                .store("Store" + subsystem, "EU")
                .flow("f" + subsystem + "1", "User" + subsystem, "Proc" + subsystem)
                .flow("f" + subsystem + "2", "Proc" + subsystem, "Store" + subsystem));

        List<UncertaintySource> sources = new ArrayList<>();
        list.forEach(subsystem -> {
            sources.add(locationSource(builder, "User" + subsystem));
            sources.add(locationSource(builder, "Store" + subsystem));
        });
        return builder.build(sources);
    }

    /**
     * Declares that one node is located in the EU by default and outside it in the alternative.
     */
    private static DFDExternalUncertaintySource locationSource(TestModelBuilder builder, String nodeName) {
        Node target = builder.nodeByName(nodeName);

        DFDExternalUncertaintySource source = DfdFactory.eINSTANCE.createDFDExternalUncertaintySource();
        source.setId("u" + nodeName);
        source.setEntityName(nodeName + "_location");
        source.setTarget(target);
        source.getTargetProperties().add(builder.labelByName("EU"));

        var alternative = DfdFactory.eINSTANCE.createDFDExternalUncertaintyScenario();
        alternative.setId("s" + nodeName);
        alternative.setEntityName(nodeName + "_nonEU");
        alternative.getTargetProperties().add(builder.labelByName("nonEU"));
        source.getScenarios().add(alternative);

        return source;
    }
}
