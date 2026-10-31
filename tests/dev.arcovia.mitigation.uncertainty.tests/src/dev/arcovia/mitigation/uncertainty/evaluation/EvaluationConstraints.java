package dev.arcovia.mitigation.uncertainty.evaluation;

import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.ilp.MitigationStrategy;
import dev.arcovia.mitigation.ilp.MitigationType;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.OutgoingDataLabel;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The confidentiality constraints the evaluation runs, declared once under the identifiers the
 * thesis cites.
 */
public final class EvaluationConstraints {

    private EvaluationConstraints() {
    }

    /**
     * An entrypoint that is not a gateway never reaches an internal vertex.
     */
    public static final AnalysisConstraint ENTRYPOINT_ONLY_VIA_GATEWAY =
            new ConstraintDSL().ofData()
                    .withLabel("Stereotype", "entrypoint")
                    .withoutLabel("Stereotype", "gateway")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "internal")
                    .create();

    /**
     * No vertex is both a gateway and internal.
     */
    public static final AnalysisConstraint GATEWAY_IS_NEVER_INTERNAL =
            new ConstraintDSL().ofData()
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "gateway")
                    .withCharacteristic("Stereotype", "internal")
                    .create();

    /**
     * Unauthenticated data never reaches an internal vertex.
     */
    public static final AnalysisConstraint UNAUTHENTICATED_NEVER_REACHES_INTERNAL =
            new ConstraintDSL().ofData()
                    .withoutLabel("Stereotype", "authenticated_request")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "internal")
                    .create();

    /**
     * An entrypoint reaches an internal vertex only after transforming the identity.
     */
    public static final AnalysisConstraint ENTRY_MUST_TRANSFORM_IDENTITY =
            new ConstraintDSL().ofData()
                    .withLabel("Stereotype", "entrypoint")
                    .withoutLabel("Stereotype", "transform_identity_representation")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "internal")
                    .create();

    /**
     * An entrypoint reaches an internal vertex only after validating the token.
     */
    public static final AnalysisConstraint ENTRY_MUST_VALIDATE_TOKEN =
            new ConstraintDSL().ofData()
                    .withLabel("Stereotype", "entrypoint")
                    .withoutLabel("Stereotype", "token_validation")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "internal")
                    .create();

    /**
     * An authorization server regulates login attempts.
     */
    public static final AnalysisConstraint AUTH_SERVER_MUST_REGULATE_LOGINS =
            new ConstraintDSL().ofData()
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "authorization_server")
                    .withoutCharacteristic("Stereotype", "login_attempts_regulation")
                    .create();

    /**
     * Data leaves an entrypoint only over an encrypted connection.
     */
    public static final AnalysisConstraint ENTRY_MUST_BE_ENCRYPTED =
            new ConstraintDSL().ofData()
                    .withLabel("Stereotype", "entrypoint")
                    .withoutLabel("Stereotype", "encrypted_connection")
                    .neverFlows()
                    .toVertex()
                    .create();

    /**
     * Data leaves an internal vertex only over an encrypted connection.
     */
    public static final AnalysisConstraint INTERNAL_MUST_BE_ENCRYPTED =
            new ConstraintDSL().ofData()
                    .withLabel("Stereotype", "internal")
                    .withoutLabel("Stereotype", "encrypted_connection")
                    .neverFlows()
                    .toVertex()
                    .create();

    /**
     * An internal vertex logs locally.
     */
    public static final AnalysisConstraint INTERNAL_MUST_LOG_LOCALLY =
            new ConstraintDSL().ofData()
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "internal")
                    .withoutCharacteristic("Stereotype", "local_logging")
                    .create();

    /**
     * A vertex that logs locally sanitizes its logs.
     */
    public static final AnalysisConstraint LOGS_MUST_BE_SANITIZED =
            new ConstraintDSL().ofData()
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "local_logging")
                    .withoutCharacteristic("Stereotype", "log_sanitization")
                    .create();

    /**
     * Personal data never reaches a vertex outside the EU.
     */
    public static final AnalysisConstraint PERSONAL_NEVER_LEAVES_EU =
            new ConstraintDSL().ofData()
                    .withLabel("Sensitivity", "Personal")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Location", "nonEU")
                    .create();

    /**
     * Public data never reaches a vertex outside the EU.
     */
    public static final AnalysisConstraint PUBLIC_NEVER_LEAVES_EU =
            new ConstraintDSL().ofData()
                    .withLabel("Sensitivity", "Public")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Location", "nonEU")
                    .create();

    /**
     * Personal data reaches a vertex outside the EU only when encrypted.
     */
    public static final AnalysisConstraint PERSONAL_OUTSIDE_EU_MUST_BE_ENCRYPTED =
            new ConstraintDSL().ofData()
                    .withLabel("Sensitivity", "Personal")
                    .withoutLabel("Encryption", "Encrypted")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Location", "nonEU")
                    .create();

    /**
     * Personal data never reaches a development vertex.
     */
    public static final AnalysisConstraint PERSONAL_NEVER_REACHES_DEVELOPMENT =
            new ConstraintDSL().ofData()
                    .withLabel("Sensitivity", "Personal")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Sensitivity", "Develop")
                    .create();

    /**
     * Encrypted data never reaches a vertex that can process it.
     */
    public static final AnalysisConstraint ENCRYPTED_NEVER_BECOMES_PROCESSABLE =
            new ConstraintDSL().ofData()
                    .withLabel("Encryption", "Encrypted")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Encryption", "Processable")
                    .create();

    /**
     * Unencrypted data never reaches a vertex outside the EU.
     */
    public static final AnalysisConstraint UNENCRYPTED_NEVER_LEAVES_EU =
            new ConstraintDSL().ofData()
                    .withLabel("Encryption", "Unencrypted")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Location", "nonEU")
                    .create();

    /**
     * Untrusted data never reaches an external vertex that is not itself trusted.
     */
    public static final AnalysisConstraint UNTRUSTED_NEVER_REACHES_UNTRUSTED_EXTERNAL =
            new ConstraintDSL().ofData()
                    .withoutLabel("Stereotype", "trusted")
                    .neverFlows()
                    .toVertex()
                    .withCharacteristic("Stereotype", "external")
                    .withoutCharacteristic("Stereotype", "trusted")
                    .create();

    /**
     * Identifier, origin, and predicate for every declared constraint, in citation order.
     * Used for automatically generating a latex table.
     */
    private static final List<Declared> DECLARED = List.of(
            new Declared("C1", "microSecEnD variant rule" + " \\cite{schneider_microsecend_2023}", ENTRYPOINT_ONLY_VIA_GATEWAY),
            new Declared("C2", "microSecEnD variant rule", GATEWAY_IS_NEVER_INTERNAL),
            new Declared("C3", "microSecEnD variant rule", UNAUTHENTICATED_NEVER_REACHES_INTERNAL),
            new Declared("C4", "microSecEnD variant rule", ENTRY_MUST_TRANSFORM_IDENTITY),
            new Declared("C5", "microSecEnD variant rule", ENTRY_MUST_VALIDATE_TOKEN),
            new Declared("C6", "same rule family, used in the uncertainty cases only", AUTH_SERVER_MUST_REGULATE_LOGINS),
            new Declared("C7", "microSecEnD variant rule", ENTRY_MUST_BE_ENCRYPTED),
            new Declared("C8", "microSecEnD variant rule", INTERNAL_MUST_BE_ENCRYPTED),
            new Declared("C9", "microSecEnD variant rule", INTERNAL_MUST_LOG_LOCALLY),
            new Declared("C10", "microSecEnD variant rule", LOGS_MUST_BE_SANITIZED),
            new Declared("R1", "residency control, used in the Banking and ABUNAI per-type cases", PERSONAL_NEVER_LEAVES_EU),
            new Declared("R2", "residency control", PUBLIC_NEVER_LEAVES_EU),
            new Declared("R3", "residency-and-encryption control, used in the running example, \\Cref{chap:running-example}", PERSONAL_OUTSIDE_EU_MUST_BE_ENCRYPTED),
            new Declared("R4", "ABUNAI behavior test model" + " \\cite{hahner_architecture-based_2025}", UNENCRYPTED_NEVER_LEAVES_EU),
            new Declared("M1", "Online Banking" + " \\cite{niehues_architecture-based_2025}", PERSONAL_NEVER_REACHES_DEVELOPMENT),
            new Declared("M2", "Online Banking", ENCRYPTED_NEVER_BECOMES_PROCESSABLE),
            new Declared("M3", "Online Banking, same predicate as R1", PERSONAL_NEVER_LEAVES_EU),
            new Declared("L1", "LoginFramework, ranking line" + " \\cite{niehues_mitigation_2026}", UNTRUSTED_NEVER_REACHES_UNTRUSTED_EXTERNAL)
    );

    /**
     * Identifier, origin, and members of every constraint set a case checks together, in citation
     * order. Used for automatically generating a latex table.
     */
    private static final List<Combined> COMBINED = List.of(
            new Combined("V1", "variant of Rank's evaluation" + " \\cite{rank_mitigating_2026}",
                    List.of(ENTRYPOINT_ONLY_VIA_GATEWAY, GATEWAY_IS_NEVER_INTERNAL)),
            new Combined("V7", "variant of Rank's evaluation",
                    List.of(ENTRY_MUST_BE_ENCRYPTED, ENTRYPOINT_ONLY_VIA_GATEWAY, GATEWAY_IS_NEVER_INTERNAL)),
            new Combined("V11", "variant of Rank's evaluation",
                    List.of(INTERNAL_MUST_LOG_LOCALLY, LOGS_MUST_BE_SANITIZED))
    );


    /**
     * Returns fresh constraints for the given predicates.
     *
     * @param predicates the predicates to build
     * @return one constraint per predicate, in the order given
     */
    public static List<Constraint> of(AnalysisConstraint... predicates) {
        return of(List.of(predicates));
    }

    /**
     * Returns fresh constraints for the given predicates.
     *
     * @param predicates the predicates to build
     * @return one constraint per predicate, in the order given
     */
    public static List<Constraint> of(List<AnalysisConstraint> predicates) {
        return predicates.stream().map(Constraint::new).toList();
    }

    /**
     * Returns a fresh constraint whose candidates also include removing a flow into a violating
     * node.
     *
     * @param predicate the predicate to build
     * @return the constraint with the derived label actions and flow removal as candidates
     */
    public static Constraint withFlowRemoval(AnalysisConstraint predicate) {
        List<MitigationStrategy> strategies = new ArrayList<>();
        strategies.add(new MitigationStrategy(
                List.of(new OutgoingDataLabel(new Label("Sensitivity", "Personal"))), 1.0, MitigationType.DeleteFlow));
        Constraint constraint = new Constraint(predicate, strategies);
        constraint.findAlternativeMitigations();
        return constraint;
    }

    /**
     * Returns the identifier the thesis cites for one declared predicate.
     *
     * @param predicate one of the predicates declared here
     * @return its identifier, for example {@code C3}
     * @throws IllegalArgumentException when the predicate is not declared here
     */
    public static String idOf(AnalysisConstraint predicate) {
        return DECLARED.stream()
                .filter(declared -> declared.dsl() == predicate)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Predicate is not declared in "
                                                                + EvaluationConstraints.class.getSimpleName()))
                .id();
    }

    /**
     * Returns one row per declared constraint, then one per combined constraint set, in citation
     * order.
     *
     * @return the rows of the generated thesis table
     */
    public static List<TableRow> tableRows() {
        var single = DECLARED.stream()
                .map(declared -> new TableRow(declared.id(), declared.dsl().toString(), declared.origin(), List.of()));
        var combined = COMBINED.stream()
                .map(set -> new TableRow(set.id(), "", set.origin(),
                        set.members().stream().map(EvaluationConstraints::idOf).toList()));
        return Stream.concat(single, combined).toList();
    }

    /**
     * One row of the generated thesis constraint table.
     *
     * @param id        the identifier the thesis cites
     * @param predicate the constraint as the analysis serializes it, empty for a combined set
     * @param origin    where the constraint comes from, in LaTeX
     * @param members   the identifiers a combined set checks together, empty for a single constraint
     */
    public record TableRow(String id, String predicate, String origin, List<String> members) {
    }

    /**
     * How one constraint is registered here.
     * <p>
     * Private on purpose: a suite names a predicate and receives constraints or an identifier, and
     * never handles the registration itself, so how a constraint is stored stays changeable.
     *
     * @param id     the identifier the thesis cites
     * @param origin where the constraint comes from, in LaTeX
     * @param dsl    builds a fresh analysis constraint
     */
    private record Declared(String id, String origin, AnalysisConstraint dsl) {
    }

    private record Combined(String id, String origin, List<AnalysisConstraint> members) {
    }
}
