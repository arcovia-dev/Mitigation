package dev.arcovia.mitigation.uncertainty.evaluation.correctness;

import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.uncertainty.evaluation.EvaluationConstraints;
import dev.arcovia.mitigation.uncertainty.evaluation.fixtures.RunningExampleFixture;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelLoader;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelSpec;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static dev.arcovia.mitigation.uncertainty.evaluation.EvaluationConstraints.*;

/**
 * Defines the fixed corpus of models, predicates, and source selections.
 */
final class RobustVsBaselineCaseStudies {

    private RobustVsBaselineCaseStudies() {
    }

    private static final String ALL_SOURCE_TYPES = "External|Behavior|Interface|Component|Connector";
    private static final String MICROSECEND_ANNOTATED =
            "microSecEnD benchmark with existing uncertainty annotation";
    private static final String ONLINE_BANKING =
            "mitigation-line Online Banking model with DSL translation";
    private static final String LOGIN_FRAMEWORK = "ranking-line LoginFramework model";
    private static final String PER_TYPE_MITIGATION = "ranking-line per-type mitigation model";
    private static final String ABUNAI_PER_TYPE = "ABUNAI per-type test model";
    private static final String AUTH_SERVER_8 = "auth Server8";
    private static final String CUSTOMER_LOCATION = "Customer_Location_Uncertain";


    /**
     * Creates the cases for the evaluation.
     *
     * @param projectDirectory current project directory used to resolve fixture paths
     * @return immutable case definitions
     */
    static List<CaseStudy> create(Path projectDirectory) {
        final Path testDirectoryPath = projectDirectory.getParent().resolve("dev.arcovia.mitigation.ranking.tests");
        final Path onlineBankingFolder = testDirectoryPath.resolve("models").resolve("OBM");
        final Path jferraterFolder = testDirectoryPath.resolve("models").resolve("jferrater");
        final Path externalModelFolder = projectDirectory.resolve("models").resolve("DFDExternalUncertaintyMitigation");
        final Path mitigationExampleFolder = projectDirectory.resolve("models").resolve("mitigation_example");
        final Path models = projectDirectory.resolve("models");
        final Path rankingModels = testDirectoryPath.resolve("models");
        final Path rankingKoushikkothagal = rankingModels.resolve("koushikkothagal");
        final Path rankingJferrater10 = rankingModels.resolve("jferrater10");
        final Path loginFramework10 = rankingModels.resolve("LoginFramework10");

        return List.of(
                new CaseStudy("Banking/ext (no uncertainty)",
                        dfd(externalModelFolder, "ext"),
                        constraints(PERSONAL_NEVER_LEAVES_EU),
                        source -> false,
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 1, "configured control", "none")),

                new CaseStudy("OnlineShop (4 scenarios)",
                        dfd(projectDirectory.resolve("models").resolve("UncertainOnlineShopDFD"), "onlineshop"),
                        constraints(PERSONAL_NEVER_LEAVES_EU),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 4, "ABUNAI example", "External")),

                new CaseStudy("OnlineShopSimple",
                        dfd(projectDirectory.resolve("models").resolve("UncertainOnlineShopDFDsimple"), "onlineshop"),
                        constraints(PERSONAL_NEVER_LEAVES_EU),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 2, "ABUNAI example", "External")),

                new CaseStudy("Banking/ext (Banking only; base-manifest, 2 scn)",
                        dfd(externalModelFolder, "ext"),
                        constraints(PERSONAL_NEVER_LEAVES_EU),
                        sources("Banking_Data_Location_Uncertain"),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 2, "configured control", "External")),

                new CaseStudy("Running example: retail checkout + R3 (External, 4 scn)",
                        RunningExampleFixture::build,
                        () -> List.of(withFlowRemoval(PERSONAL_OUTSIDE_EU_MUST_BE_ENCRYPTED)),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_INSUFFICIENT, 4, "thesis running example", "External")),

                new CaseStudy("Banking+Transactions/ext (LATENT, 4 scn)",
                        dfd(externalModelFolder, "ext"),
                        constraints(PERSONAL_NEVER_LEAVES_EU),
                        sources("Banking_Data_Location_Uncertain", "Transactions_DB_Location_Uncertain"),
                        new CaseExpectation(ComparisonVerdict.robustUnavailable(RepairOutcome.NO_ROBUST_REPAIR), 4, "configured latent control", "External")),

                new CaseStudy("Transactions/ext (LATENT only, 2 scn)",
                        dfd(externalModelFolder, "ext"),
                        constraints(PERSONAL_NEVER_LEAVES_EU),
                        sources("Transactions_DB_Location_Uncertain"),
                        new CaseExpectation(ComparisonVerdict.robustUnavailable(RepairOutcome.NO_ROBUST_REPAIR), 2, "configured latent control", "External")),

                new CaseStudy("Transactions/ext + ENCRYPTION-escape (LATENT, 2 scn)",
                        dfd(externalModelFolder, "ext"),
                        constraints(PERSONAL_OUTSIDE_EU_MUST_BE_ENCRYPTED),
                        sources("Transactions_DB_Location_Uncertain"),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_INSUFFICIENT, 2, "configured latent control", "External")),

                new CaseStudy("Banking+Transactions/ext + ENCRYPTION-escape (LATENT, 4 scn)",
                        dfd(externalModelFolder, "ext"),
                        constraints(PERSONAL_OUTSIDE_EU_MUST_BE_ENCRYPTED),
                        sources("Banking_Data_Location_Uncertain", "Transactions_DB_Location_Uncertain"),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_INSUFFICIENT, 4, "configured interaction control", "External")),

                new CaseStudy("mitigation_example (Personal; couples -> fallback)",
                        dfd(mitigationExampleFolder, "mitigation_example"),
                        constraints(PERSONAL_NEVER_LEAVES_EU),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_INSUFFICIENT, 32, "ARCoViA mitigation example", ALL_SOURCE_TYPES)),

                new CaseStudy("mitigation_example (Public; no violation, 32 scn)",
                        dfd(mitigationExampleFolder, "mitigation_example"),
                        constraints(PUBLIC_NEVER_LEAVES_EU),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 32, "ARCoViA mitigation example", ALL_SOURCE_TYPES)),

                new CaseStudy("microSecEnD jferrater + C3 auth (External, 2 scn)",
                        dfd(jferraterFolder, "jferrater"),
                        constraints(UNAUTHENTICATED_NEVER_REACHES_INTERNAL),
                        sources(AUTH_SERVER_8),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 2, MICROSECEND_ANNOTATED, "External")),

                new CaseStudy("microSecEnD jferrater + C6 login attempts (External, 2 scn)",
                        dfd(jferraterFolder, "jferrater"),
                        constraints(AUTH_SERVER_MUST_REGULATE_LOGINS),
                        sources(AUTH_SERVER_8),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 2, MICROSECEND_ANNOTATED, "External")),

                new CaseStudy("microSecEnD jferrater + C9 logging (External, 2 scn)",
                        dfd(jferraterFolder, "jferrater"),
                        constraints(INTERNAL_MUST_LOG_LOCALLY),
                        sources(AUTH_SERVER_8),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 2, MICROSECEND_ANNOTATED, "External")),

                new CaseStudy("Online Banking + M1 Personal/Develop (External, 2 scn)",
                        dfd(onlineBankingFolder, "OBM"),
                        constraints(PERSONAL_NEVER_REACHES_DEVELOPMENT),
                        sources(CUSTOMER_LOCATION),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 2, ONLINE_BANKING, "External")),

                new CaseStudy("Online Banking + M2 Encrypted/Processable (External, 2 scn)",
                        dfd(onlineBankingFolder, "OBM"),
                        constraints(ENCRYPTED_NEVER_BECOMES_PROCESSABLE),
                        sources(CUSTOMER_LOCATION),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 2, ONLINE_BANKING, "External")),

                new CaseStudy("Online Banking + M3 Personal/nonEU (External, 2 scn)",
                        dfd(onlineBankingFolder, "OBM"),
                        constraints(PERSONAL_NEVER_LEAVES_EU),
                        sources(CUSTOMER_LOCATION),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 2, ONLINE_BANKING, "External")),

                new CaseStudy("microSecEnD jferrater + C5 token validation (External, 2 scn)",
                        dfd(jferraterFolder, "jferrater"),
                        constraints(ENTRY_MUST_VALIDATE_TOKEN),
                        sources(AUTH_SERVER_8),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 2, MICROSECEND_ANNOTATED, "External")),

                new CaseStudy("microSecEnD koushikkothagal + V1 {C1,C2} (ranking annotation, 128 scn)",
                        dfd(rankingKoushikkothagal, "koushikkothagal"),
                        constraints(ENTRYPOINT_ONLY_VIA_GATEWAY, GATEWAY_IS_NEVER_INTERNAL),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 128, MICROSECEND_ANNOTATED, "External|Behavior")),

                new CaseStudy("microSecEnD koushikkothagal + C3 auth (ranking annotation, 128 scn)",
                        dfd(rankingKoushikkothagal, "koushikkothagal"),
                        constraints(UNAUTHENTICATED_NEVER_REACHES_INTERNAL),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 128, MICROSECEND_ANNOTATED, "External|Behavior")),

                new CaseStudy("microSecEnD koushikkothagal + C4 identity transform (ranking annotation, 128 scn)",
                        dfd(rankingKoushikkothagal, "koushikkothagal"),
                        constraints(ENTRY_MUST_TRANSFORM_IDENTITY),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 128, MICROSECEND_ANNOTATED, "External|Behavior")),

                new CaseStudy("microSecEnD koushikkothagal + C5 token validation (ranking annotation, 128 scn)",
                        dfd(rankingKoushikkothagal, "koushikkothagal"),
                        constraints(ENTRY_MUST_VALIDATE_TOKEN),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 128, MICROSECEND_ANNOTATED, "External|Behavior")),

                new CaseStudy("microSecEnD koushikkothagal + V7 {C7,C1,C2} (ranking annotation, 128 scn)",
                        dfd(rankingKoushikkothagal, "koushikkothagal"),
                        constraints(ENTRY_MUST_BE_ENCRYPTED, ENTRYPOINT_ONLY_VIA_GATEWAY, GATEWAY_IS_NEVER_INTERNAL),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 128, MICROSECEND_ANNOTATED, "External|Behavior")),

                new CaseStudy("microSecEnD koushikkothagal + C8 internal encryption (ranking annotation, 128 scn)",
                        dfd(rankingKoushikkothagal, "koushikkothagal"),
                        constraints(INTERNAL_MUST_BE_ENCRYPTED),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 128, MICROSECEND_ANNOTATED, "External|Behavior")),

                new CaseStudy("microSecEnD koushikkothagal + C9 logging (ranking annotation, 128 scn)",
                        dfd(rankingKoushikkothagal, "koushikkothagal"),
                        constraints(INTERNAL_MUST_LOG_LOCALLY),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 128, MICROSECEND_ANNOTATED, "External|Behavior")),

                new CaseStudy("microSecEnD koushikkothagal + V11 {C9,C10} (ranking annotation, 128 scn)",
                        dfd(rankingKoushikkothagal, "koushikkothagal"),
                        constraints(INTERNAL_MUST_LOG_LOCALLY, LOGS_MUST_BE_SANITIZED),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_INSUFFICIENT, 128, MICROSECEND_ANNOTATED, "External|Behavior")),

                new CaseStudy("microSecEnD koushikkothagal + C6 login attempts (ranking annotation, 128 scn)",
                        dfd(rankingKoushikkothagal, "koushikkothagal"),
                        constraints(AUTH_SERVER_MUST_REGULATE_LOGINS),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 128, MICROSECEND_ANNOTATED, "External|Behavior")),

                new CaseStudy("microSecEnD jferrater10 + C3 auth (ranking annotation, 1024 scn)",
                        dfd(rankingJferrater10, "jferrater"),
                        constraints(UNAUTHENTICATED_NEVER_REACHES_INTERNAL),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_INSUFFICIENT, 1024, MICROSECEND_ANNOTATED, "External|Behavior")),

                new CaseStudy("microSecEnD jferrater10 + C5 token validation (ranking annotation, 1024 scn)",
                        dfd(rankingJferrater10, "jferrater"),
                        constraints(ENTRY_MUST_VALIDATE_TOKEN),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_INSUFFICIENT, 1024, MICROSECEND_ANNOTATED, "External|Behavior")),

                new CaseStudy("microSecEnD jferrater10 + V7 {C7,C1,C2} (ranking annotation, 1024 scn)",
                        dfd(rankingJferrater10, "jferrater"),
                        constraints(ENTRY_MUST_BE_ENCRYPTED, ENTRYPOINT_ONLY_VIA_GATEWAY, GATEWAY_IS_NEVER_INTERNAL),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_INSUFFICIENT, 1024, MICROSECEND_ANNOTATED, "External|Behavior")),

                // Several nominal optima of cost 8 exist; the canonical solver order returns the one
                // that leaves 1016 of 1024 scenarios violating, so the verdict below is a property of
                // that order (chapter 6, tie-breaking).
                new CaseStudy("microSecEnD jferrater10 + C8 internal encryption (ranking annotation, 1024 scn)",
                        dfd(rankingJferrater10, "jferrater"),
                        constraints(INTERNAL_MUST_BE_ENCRYPTED),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_INSUFFICIENT, 1024, MICROSECEND_ANNOTATED, "External|Behavior")),

                new CaseStudy("microSecEnD jferrater10 + C6 login attempts (ranking annotation, 1024 scn)",
                        dfd(rankingJferrater10, "jferrater"),
                        constraints(AUTH_SERVER_MUST_REGULATE_LOGINS),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 1024, MICROSECEND_ANNOTATED, "External|Behavior")),

                new CaseStudy("microSecEnD jferrater10 + C9 logging (ranking annotation, 1024 scn)",
                        dfd(rankingJferrater10, "jferrater"),
                        constraints(INTERNAL_MUST_LOG_LOCALLY),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 1024, MICROSECEND_ANNOTATED, "External|Behavior")),

                new CaseStudy("Online Banking + M1 Personal/Develop (all 9 sources, 512 scn)",
                        dfd(onlineBankingFolder, "OBM"),
                        constraints(PERSONAL_NEVER_REACHES_DEVELOPMENT),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_INSUFFICIENT, 512, ONLINE_BANKING, ALL_SOURCE_TYPES)),

                new CaseStudy("Online Banking + M2 Encrypted/Processable (all 9 sources, 512 scn)",
                        dfd(onlineBankingFolder, "OBM"),
                        constraints(ENCRYPTED_NEVER_BECOMES_PROCESSABLE),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 512, ONLINE_BANKING, ALL_SOURCE_TYPES)),

                new CaseStudy("Online Banking + M3 Personal/nonEU (all 9 sources, 512 scn)",
                        dfd(onlineBankingFolder, "OBM"),
                        constraints(PERSONAL_NEVER_LEAVES_EU),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 512, ONLINE_BANKING, ALL_SOURCE_TYPES)),

                new CaseStudy("LoginFramework10 + L1 untrusted external (ranking annotation, 1024 scn)",
                        dfd(loginFramework10, "default"),
                        constraints(UNTRUSTED_NEVER_REACHES_UNTRUSTED_EXTERNAL),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 1024, LOGIN_FRAMEWORK, "External|Behavior")),

                new CaseStudy("LoginFramework10 + C9 logging (ranking annotation, 1024 scn)",
                        dfd(loginFramework10, "default"),
                        constraints(INTERNAL_MUST_LOG_LOCALLY),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 1024, LOGIN_FRAMEWORK, "External|Behavior")),

                new CaseStudy("Banking/ext + M1 Personal/Develop (External, 64 scn)",
                        dfd(models.resolve("DFDExternalUncertaintyMitigation"), "ext"),
                        constraints(PERSONAL_NEVER_REACHES_DEVELOPMENT),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 64, PER_TYPE_MITIGATION, "External")),

                new CaseStudy("Banking/ext + R1 Personal/nonEU (all 6 sources, 64 scn)",
                        dfd(models.resolve("DFDExternalUncertaintyMitigation"), "ext"),
                        constraints(PERSONAL_NEVER_LEAVES_EU),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.robustUnavailable(RepairOutcome.NO_ROBUST_REPAIR), 64, PER_TYPE_MITIGATION, "External")),

                new CaseStudy("Banking/beh + M1 Personal/Develop (Behavior, 16 scn)",
                        dfd(models.resolve("DFDBehaviorUncertaintyMitigation"), "beh"),
                        constraints(PERSONAL_NEVER_REACHES_DEVELOPMENT),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 16, PER_TYPE_MITIGATION, "Behavior")),

                new CaseStudy("Banking/beh + R1 Personal/nonEU (Behavior, 16 scn)",
                        dfd(models.resolve("DFDBehaviorUncertaintyMitigation"), "beh"),
                        constraints(PERSONAL_NEVER_LEAVES_EU),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 16, PER_TYPE_MITIGATION, "Behavior")),

                new CaseStudy("Banking/comp + M1 Personal/Develop (Component, 32 scn)",
                        dfd(models.resolve("DFDComponentUncertaintyMitigation"), "comp"),
                        constraints(PERSONAL_NEVER_REACHES_DEVELOPMENT),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 32, PER_TYPE_MITIGATION, "Component")),

                new CaseStudy("Banking/comp + R1 Personal/nonEU (Component, 32 scn)",
                        dfd(models.resolve("DFDComponentUncertaintyMitigation"), "comp"),
                        constraints(PERSONAL_NEVER_LEAVES_EU),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_INSUFFICIENT, 32, PER_TYPE_MITIGATION, "Component")),

                new CaseStudy("Banking/int + M1 Personal/Develop (Interface, 64 scn)",
                        dfd(models.resolve("DFDInterfaceUncertaintyMitigation"), "int"),
                        constraints(PERSONAL_NEVER_REACHES_DEVELOPMENT),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 64, PER_TYPE_MITIGATION, "Interface")),

                new CaseStudy("Banking/int + R1 Personal/nonEU (Interface, 64 scn)",
                        dfd(models.resolve("DFDInterfaceUncertaintyMitigation"), "int"),
                        constraints(PERSONAL_NEVER_LEAVES_EU),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_INSUFFICIENT, 64, PER_TYPE_MITIGATION, "Interface")),

                new CaseStudy("ABUNAI DFDExternalUncertainty + R1 Personal/nonEU (External, 2 scn)",
                        dfd(models.resolve("DFDExternalUncertainty"), "default"),
                        constraints(PERSONAL_NEVER_LEAVES_EU),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 2, ABUNAI_PER_TYPE, "External")),

                new CaseStudy("ABUNAI DFDBehaviorUncertainty + R4 Unencrypted/nonEU (Behavior, 2 scn)",
                        dfd(models.resolve("DFDBehaviorUncertainty"), "default"),
                        constraints(UNENCRYPTED_NEVER_LEAVES_EU),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 2, ABUNAI_PER_TYPE, "Behavior")),

                new CaseStudy("ABUNAI DFDComponentUncertainty + R1 Personal/nonEU (Component, 2 scn)",
                        dfd(models.resolve("DFDComponentUncertainty"), "default"),
                        constraints(PERSONAL_NEVER_LEAVES_EU),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 2, ABUNAI_PER_TYPE, "Component")),

                new CaseStudy("ABUNAI DFDConnectorUncertainty + R1 Personal/nonEU (Connector, 2 scn)",
                        dfd(models.resolve("DFDConnectorUncertainty"), "default"),
                        constraints(PERSONAL_NEVER_LEAVES_EU),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 2, ABUNAI_PER_TYPE, "Connector")),

                new CaseStudy("ABUNAI DFDInterfaceUncertainty + R3 Personal+Unencrypted/nonEU (Interface, 2 scn)",
                        dfd(models.resolve("DFDInterfaceUncertainty"), "default"),
                        constraints(PERSONAL_OUTSIDE_EU_MUST_BE_ENCRYPTED),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_SUFFICIENT, 2, ABUNAI_PER_TYPE, "Interface")),

                // The only pre-annotated case whose base model is clean while alternative scenarios
                // violate: U11, U12 and U15 can each make a process log locally (scan of 2026-10-05).
                new CaseStudy("LoginFramework10 + C10 log sanitization (ranking annotation, 1024 scn)",
                        dfd(loginFramework10, "default"),
                        constraints(LOGS_MUST_BE_SANITIZED),
                        nullUncertaintySourceFilter(),
                        new CaseExpectation(ComparisonVerdict.NOMINAL_INSUFFICIENT, 1024, LOGIN_FRAMEWORK, "External|Behavior")));
    }

    /**
     * Returns the frozen expectations of the given cases, keyed by case identifier.
     *
     * @param studies the declared cases
     * @return case identifiers mapped to their frozen expectations, in declaration order
     * @throws IllegalStateException when two cases share an identifier
     */
    static Map<String, CaseExpectation> expectations(List<CaseStudy> studies) {
        return studies.stream()
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.toMap(
                                        CaseStudy::name,
                                        CaseStudy::expectation,
                                        (first, second) -> {
                                            throw new IllegalStateException("Two case studies share an identifier");
                                        },
                                        LinkedHashMap::new
                                ),
                                Collections::unmodifiableMap)
                );
    }

    private static Supplier<List<Constraint>> constraints(AnalysisConstraint... predicates) {
        return () -> EvaluationConstraints.of(predicates);
    }

    /**
     * Selects the named uncertainty sources.
     *
     * @param entityNames the entity names to select
     * @return a predicate matching exactly those sources
     */
    private static Predicate<UncertaintySource> sources(String... entityNames) {
        return source -> List.of(entityNames).contains(source.getEntityName());
    }

    /**
     * Selects every uncertainty source the model declares.
     *
     * @return the null filter == no selection
     */
    private static Predicate<UncertaintySource> nullUncertaintySourceFilter() {
        return null;
    }


    /**
     * Creates a model specification from matching fixture files.
     *
     * @param folder directory containing the model files
     * @param base   shared fixture name without an extension
     * @return a loader that reads the files afresh on every call
     */
    private static Supplier<LoadedUncertaintyModel> dfd(Path folder, String base) {
        UncertaintyModelSpec spec = new UncertaintyModelSpec(
                folder.resolve(base + ".dataflowdiagram"),
                folder.resolve(base + ".datadictionary"),
                folder.resolve(base + ".uncertainty"));
        return () -> new UncertaintyModelLoader().load(spec);
    }
}
