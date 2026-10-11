package dev.arcovia.mitigation.uncertainty.preparation;

import dev.arcovia.mitigation.cost.RepairActionKey;
import dev.arcovia.mitigation.ilp.Mitigation;
import dev.arcovia.mitigation.ilp.Node;
import dev.arcovia.mitigation.ilp.RepairActionKeys;
import org.eclipse.jdt.annotation.NonNull;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * Derives deterministic coverage alternatives for violations. Pipeline stage 4.
 */
public final class CoverageDerivation {

    private static final BinaryOperator<Mitigation> CHEAPER_MITIGATION =
            BinaryOperator.minBy(Comparator.comparingDouble(Mitigation::cost));

    private final List<List<Mitigation>> coverageSets = new ArrayList<>();
    private final Map<RepairActionKey, Mitigation> canonicalMitigations = new TreeMap<>();
    private final Map<String, Mitigation> contributorMitigations = new LinkedHashMap<>();

    /**
     * Adds coverage alternatives for one violation.
     *
     * @param violatingNode the violating node
     */
    public void deriveFor(@NonNull Node violatingNode) {
        Objects.requireNonNull(violatingNode, "violatingNode must not be null");
        List<Mitigation> mitigationCandidates = violatingNode.getPossibleMitigations(false);

        List<Mitigation> coverageAlternatives = mitigationCandidates.stream()
                .map(this::canonicalize)
                .sorted(Comparator.comparing(RepairActionKeys::from))
                .collect(Collectors.toCollection(ArrayList::new));
        coverageSets.add(coverageAlternatives);

        for (Mitigation mitigation : mitigationCandidates) {
            recordContributorRecursively(mitigation);
        }
    }

    /**
     * Returns coverage alternatives in deterministic order.
     *
     * @return one action list per violation
     */
    public List<List<Mitigation>> coverageSets() {
        List<List<Mitigation>> sorted = new ArrayList<>(coverageSets);
        sorted.sort(Comparator.comparing(coverage -> coverage.stream()
                .map(mitigation -> RepairActionKeys.from(mitigation).stableId())
                .collect(Collectors.joining(","))));
        return sorted;
    }

    /**
     * Returns one lowest-cost mitigation per action key.
     *
     * @return the canonical mitigations
     */
    public Collection<Mitigation> canonicalMitigations() {
        return canonicalMitigations.values();
    }

    /**
     * Returns all mitigations, including required actions.
     *
     * @return an immutable contributor list
     */
    public List<Mitigation> contributorMitigations() {
        return List.copyOf(contributorMitigations.values());
    }

    /**
     * Records the cheaper mitigation for its action key.
     *
     * @param mitigation the mitigation to canonicalize
     * @return the canonical mitigation
     */
    private Mitigation canonicalize(Mitigation mitigation) {
        RepairActionKey key = RepairActionKeys.from(mitigation);
        canonicalMitigations.merge(key, mitigation, CHEAPER_MITIGATION);
        return canonicalMitigations.get(key);
    }

    /**
     * Records a mitigation and its required actions.
     *
     * @param mitigation the mitigation to record
     */
    private void recordContributorRecursively(Mitigation mitigation) {
        canonicalize(mitigation);
        String contributorKey = RepairActionKeys.from(mitigation).stableId() + "#" + requiredSignature(mitigation);
        contributorMitigations.merge(contributorKey, mitigation, CHEAPER_MITIGATION);
        mitigation.required().stream()
                .flatMap(List::stream)
                .forEach(this::recordContributorRecursively);
    }

    /**
     * Creates a stable signature for required actions.
     *
     * @param mitigation the mitigation to describe
     * @return the sorted clause signature
     */
    private String requiredSignature(Mitigation mitigation) {
        return mitigation.required().stream()
                .map(clause -> clause.stream()
                        .map(child -> RepairActionKeys.from(child).stableId())
                        .sorted()
                        .collect(Collectors.joining("&")))
                .sorted()
                .collect(Collectors.joining("|"));
    }
}
