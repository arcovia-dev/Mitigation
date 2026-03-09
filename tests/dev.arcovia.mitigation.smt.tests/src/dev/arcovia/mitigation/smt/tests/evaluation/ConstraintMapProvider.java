package dev.arcovia.mitigation.smt.tests.evaluation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.junit.jupiter.api.Test;

public final class ConstraintMapProvider {

    private ConstraintMapProvider() {
    }

    // -------------------------
    // AnalysisConstraint definitions
    // -------------------------

    private static AnalysisConstraint entryViaGatewayOnly() {
        return new ConstraintDSL().ofData()
                .withLabel("Stereotype", "entrypoint")
                .withoutLabel("Stereotype", "gateway")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Stereotype", "internal")
                .create();
    }

    private static AnalysisConstraint nonInternalGateway() {
        return new ConstraintDSL().ofData()
                .neverFlows()
                .toVertex()
                .withCharacteristic("Stereotype", "gateway")
                .withCharacteristic("Stereotype", "internal")
                .create();
    }

    private static AnalysisConstraint authenticatedRequest() {
        return new ConstraintDSL().ofData()
                .withoutLabel("Stereotype", "authenticated_request")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Stereotype", "internal")
                .create();
    }

    private static AnalysisConstraint transformedEntry() {
        return new ConstraintDSL().ofData()
                .withLabel("Stereotype", "entrypoint")
                .withoutLabel("Stereotype", "transform_identity_representation")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Stereotype", "internal")
                .create();
    }

    private static AnalysisConstraint tokenValidation() {
        return new ConstraintDSL().ofData()
                .withLabel("Stereotype", "entrypoint")
                .withoutLabel("Stereotype", "token_validation")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Stereotype", "internal")
                .create();
    }

    private static AnalysisConstraint encryptedEntry() {
        return new ConstraintDSL().ofData()
                .withLabel("Stereotype", "entrypoint")
                .withoutLabel("Stereotype", "encrypted_connection")
                .neverFlows()
                .toVertex()
                .create();
    }

    private static AnalysisConstraint encryptedInternals() {
        return new ConstraintDSL().ofData()
                .withLabel("Stereotype", "internal")
                .withoutLabel("Stereotype", "encrypted_connection")
                .neverFlows()
                .toVertex()
                .create();
    }

    private static AnalysisConstraint localLogging() {
        return new ConstraintDSL().ofData()
                .neverFlows()
                .toVertex()
                .withCharacteristic("Stereotype", "internal")
                .withoutCharacteristic("Stereotype", "local_logging")
                .create();
    }

    private static AnalysisConstraint logSanitization() {
        return new ConstraintDSL().ofData()
                .neverFlows()
                .toVertex()
                .withCharacteristic("Stereotype", "local_logging")
                .withoutCharacteristic("Stereotype", "log_sanitization")
                .create();
    }

    private static final Map<Integer, List<AnalysisConstraint>> CONSTRAINTS = Map.of(1, List.of(entryViaGatewayOnly(), nonInternalGateway()), 2,
            List.of(authenticatedRequest()), 4, List.of(transformedEntry()), 5, List.of(tokenValidation()), 7,
            List.of(encryptedEntry(), entryViaGatewayOnly(), nonInternalGateway()), 8, List.of(encryptedInternals()), 10, List.of(localLogging()), 11,
            List.of(localLogging(), logSanitization()));

    public static Map<Integer, List<AnalysisConstraint>> buildConstraintMap() {
        return CONSTRAINTS;
    }

    public static List<Integer> variantIds() {
        return CONSTRAINTS.keySet()
                .stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    public static List<AnalysisConstraint> getOrThrow(int variantId) {
        List<AnalysisConstraint> constraint = CONSTRAINTS.get(variantId);
        if (constraint == null)
            throw new IllegalArgumentException("Constraint undefined: " + variantId);
        return constraint;
    }

    public void printConstraints() {
        for (Entry<Integer, List<AnalysisConstraint>> entry : CONSTRAINTS.entrySet()) {
            System.out.println(entry.getKey());
            for (AnalysisConstraint constr : entry.getValue()) {
                System.out.println(constr);
            }
        }
    }
}
