package dev.arcovia.mitigation.uncertainty.evaluation.fixtures;

import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDExternalUncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DfdFactory;
import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.uncertainty.fixtures.TestModelBuilder;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Generates a synthetic model whose three cost drivers move independently, so a scalability sweep
 * can vary one of them while the other two stay fixed.
 */
public final class FactorSweepFixture {

    private FactorSweepFixture() {
    }

    /**
     * One point of a factor sweep.
     *
     * @param sourceCount      uncertainty sources on the uncertain chain, {@code n}
     * @param choicesPerSource choices per source including the default, {@code k}
     * @param violatingChains  independent statically violating chains
     */
    public record Spec(int sourceCount, int choicesPerSource, int violatingChains) {

        /**
         * Size of the scenario space this specification produces.
         *
         * @return {@code k^n}
         */
        public long scenarioCount() {
            return (long) Math.pow(choicesPerSource, sourceCount);
        }
    }

    /**
     * Builds the model and its uncertainty sources.
     *
     * @param spec the point to generate
     * @return the loaded model the repair pipeline consumes
     */
    public static LoadedUncertaintyModel build(Spec spec) {
        TestModelBuilder builder = new TestModelBuilder()
                .labelType("Sensitivity", "Personal")
                .labelType("Encryption", "Encrypted")
                .labelType("Location", locationValues(spec.choicesPerSource()));

        List<UncertaintySource> sources = new ArrayList<>();
        if (spec.sourceCount() > 0) {
            builder.source("UUser", "Personal");
            IntStream.range(0, spec.sourceCount()).forEach(index ->
                    builder.forwarder("UP" + index).nodeLabels("UP" + index, "EU"));
            builder.store("UStore", "EU");
            builder.flow("uf0", "UUser", "UP0");
            IntStream.range(1, spec.sourceCount()).forEach(index ->
                    builder.flow("uf" + index, "UP" + (index - 1), "UP" + index));
            builder.flow("uf" + spec.sourceCount(), "UP" + (spec.sourceCount() - 1), "UStore");
            IntStream.range(0, spec.sourceCount()).forEach(index ->
                    sources.add(locationSource(builder, "UP" + index, spec.choicesPerSource())));
        }

        IntStream.range(0, spec.violatingChains()).forEach(chain -> builder
                .source("VUser" + chain, "Personal")
                .forwarder("VProc" + chain)
                .store("VStore" + chain, violatingValue(0))
                .flow("vf" + chain + "a", "VUser" + chain, "VProc" + chain)
                .flow("vf" + chain + "b", "VProc" + chain, "VStore" + chain));

        return builder.build(sources);
    }

    /**
     * The residency-and-encryption constraint, covering every violating location the given number
     * of choices can install.
     *
     * @param choicesPerSource choices per source including the default
     * @return the single constraint evaluated in every scenario
     */
    public static List<Constraint> constraints(int choicesPerSource) {
        return List.of(new Constraint(new ConstraintDSL().ofData()
                .withLabel("Sensitivity", "Personal")
                .withoutLabel("Encryption", "Encrypted")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Location", violatingValues(choicesPerSource))
                .create()));
    }

    /**
     * Declares one external source on a forwarder: {@code EU} by default, one violating location
     * per alternative.
     */
    private static DFDExternalUncertaintySource locationSource(TestModelBuilder builder, String nodeName,
                                                               int choicesPerSource) {
        DFDExternalUncertaintySource source = DfdFactory.eINSTANCE.createDFDExternalUncertaintySource();
        source.setId("u" + nodeName);
        source.setEntityName(nodeName + "_location");
        source.setTarget(builder.nodeByName(nodeName));
        source.getTargetProperties().add(builder.labelByName("EU"));

        IntStream.range(0, choicesPerSource - 1).forEach(alternative -> {
            var scenario = DfdFactory.eINSTANCE.createDFDExternalUncertaintyScenario();
            scenario.setId("s" + nodeName + "_" + alternative);
            scenario.setEntityName(nodeName + "_" + violatingValue(alternative));
            scenario.getTargetProperties().add(builder.labelByName(violatingValue(alternative)));
            source.getScenarios().add(scenario);
        });
        return source;
    }

    private static String[] locationValues(int choicesPerSource) {
        List<String> values = new ArrayList<>();
        values.add("EU");
        values.addAll(violatingValues(choicesPerSource));
        return values.toArray(String[]::new);
    }

    private static List<String> violatingValues(int choicesPerSource) {
        return IntStream.range(0, Math.max(1, choicesPerSource - 1))
                .mapToObj(FactorSweepFixture::violatingValue)
                .toList();
    }

    private static String violatingValue(int index) {
        return "nonEU" + index;
    }
}
