package dev.arcovia.mitigation.smt.tests.evaluation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import dev.arcovia.mitigation.smt.utils.ParsingUtils;

/**
 * Common helper methods/objects for evaluation tests.
 */
public final class EvaluationSupport {

    private EvaluationSupport() {
    }

    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static void writeJson(Path out, Object value) throws Exception {
        Files.createDirectories(out.getParent());
        JSON.writeValue(out.toFile(), value);
    }

    public static <T> T readJson(Path path, TypeReference<T> typeRef) throws Exception {
        return JSON.readValue(path.toFile(), typeRef);
    }

    public record Configuration(String model, int variantId, List<AnalysisConstraint> constraints) {
    }

    public static List<Configuration> configurations() throws Exception {
        var tuhhModels = TuhhModels.getTuhhModels();
        List<Configuration> out = new ArrayList<>();

        for (var entry : tuhhModels.entrySet()) {
            String model = entry.getKey();
            List<Integer> definedVariants = entry.getValue();

            if (!definedVariants.contains(0))
                continue;

            for (int variantId : ConstraintMapProvider.variantIds()) {
                if (!definedVariants.contains(variantId))
                    continue;

                List<AnalysisConstraint> constraints = ConstraintMapProvider.getOrThrow(variantId);
                out.add(new Configuration(model, variantId, constraints));
            }
        }

        return out;
    }

    /**
     * Prints maximum number of nodes of TUHH dataset
     * @throws Exception
     */
    public void test() throws Exception {
        int max = 0;
        for (Configuration cfg : configurations()) {
            var dfd = ParsingUtils.loadDFD(cfg.model, cfg.model + "_0");
            if (dfd.dataFlowDiagram()
                    .getNodes()
                    .size() > max) {
                max = dfd.dataFlowDiagram()
                        .getNodes()
                        .size();
            }
        }
        System.out.println(max);
    }
}
