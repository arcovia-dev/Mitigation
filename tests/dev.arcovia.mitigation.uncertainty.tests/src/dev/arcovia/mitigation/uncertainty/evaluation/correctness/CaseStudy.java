package dev.arcovia.mitigation.uncertainty.evaluation.correctness;

import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Defines one reproducible evaluation case together with the outcome it is expected to produce.
 *
 * @param name         case identifier
 * @param model        the model files this case loads
 * @param constraints  The confidentiality predicate this case evaluates
 * @param sourceFilter selected uncertainty-source predicate, or {@code null} for every source
 * @param expectation  the frozen outcome this case must reproduce
 */
record CaseStudy(
        String name,
        Supplier<LoadedUncertaintyModel> model,
        Supplier<List<Constraint>> constraints,
        Predicate<UncertaintySource> sourceFilter,
        CaseExpectation expectation) {
}
