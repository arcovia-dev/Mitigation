package dev.arcovia.mitigation.uncertainty.solving;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import dev.arcovia.mitigation.cost.*;
import dev.arcovia.mitigation.ilp.*;
import dev.arcovia.mitigation.sat.LabelCategory;
import dev.arcovia.mitigation.uncertainty.preparation.RepairPreparationResult;
import dev.arcovia.mitigation.uncertainty.solving.UncertaintyControlledLabelDetector.ControlledLabelKey;
import org.apache.log4j.Logger;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.eclipse.jdt.annotation.NonNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Builds and solves the joint robust-repair ILP. Pipeline stage 5.
 */
public final class RobustILPSolver {

    private static final Logger LOGGER = Logger.getLogger(RobustILPSolver.class);

    /**
     * Action types the solver may select; empty means every type is available.
     */
    private Set<ActionType> allowedActionTypes = Set.of();

    /**
     * Solves one joint robust-repair ILP and excludes removals that scenario materialization would undo.
     *
     * @param scenarioPreparations the non-empty stage-4 preparations for all considered scenarios
     * @param forbiddenLabels      alternative-injected labels that must not be removed
     * @return the selected canonical mitigations and solver statistics
     * @throws IllegalArgumentException      if {@code scenarioPreparations} is empty
     * @throws NoRobustRepairExistsException if no feasible robust repair exists
     * @throws IllegalStateException         if SCIP cannot be created
     */
    public RobustSolverResult solveWithResult(@NonNull List<RepairPreparationResult> scenarioPreparations,
                                              @NonNull Set<ControlledLabelKey> forbiddenLabels)
            throws NoRobustRepairExistsException {
        return solveWithResult(scenarioPreparations, forbiddenLabels,
                RepairCostSpecification.standardPreference(), null);
    }

    /**
     * Solves one joint robust-repair ILP restricted to a declared action vocabulary.
     * <p>
     * Restriction clamps the variable of every action outside the vocabulary to zero, which is
     * exact. Pricing such an action at a very large cost would leave it selectable and would put a
     * numerically hostile coefficient in the objective, so it is not used. A coverage set whose
     * every member lies outside the vocabulary therefore becomes infeasible and is reported as
     * such, which is the honest answer for a vocabulary that cannot repair the model.
     *
     * @param scenarioPreparations the non-empty stage-4 preparations for all considered scenarios
     * @param forbiddenLabels      alternative-injected labels that must not be removed
     * @param costSpecification    the direct and shared repair costs
     * @param baseModel            the unrepaired model used for action descriptors
     * @param allowedActionTypes   the action types the solver may select; an empty set allows all
     * @return the selected canonical mitigations, solver statistics, and objective breakdown
     * @throws NoRobustRepairExistsException if no feasible repair exists within the vocabulary
     */
    public RobustSolverResult solveWithResult(@NonNull List<RepairPreparationResult> scenarioPreparations,
                                              @NonNull Set<ControlledLabelKey> forbiddenLabels,
                                              @NonNull RepairCostSpecification costSpecification,
                                              DataFlowDiagramAndDictionary baseModel,
                                              @NonNull Set<ActionType> allowedActionTypes)
            throws NoRobustRepairExistsException {
        Objects.requireNonNull(allowedActionTypes, "allowedActionTypes must not be null");
        this.allowedActionTypes = Set.copyOf(allowedActionTypes);
        try {
            return solveWithResult(scenarioPreparations, forbiddenLabels, costSpecification, baseModel);
        } finally {
            this.allowedActionTypes = Set.of();
        }
    }

    /**
     * Solves one joint robust-repair ILP with a declared thesis cost specification.
     *
     * @param scenarioPreparations the non-empty stage-4 preparations for all considered scenarios
     * @param forbiddenLabels      alternative-injected labels that must not be removed
     * @param costSpecification    the direct and shared repair costs
     * @param baseModel            the unrepaired model used for action descriptors
     * @return the selected canonical mitigations, solver statistics, and objective breakdown
     * @throws IllegalArgumentException      if the preparations are empty or a required descriptor is unavailable
     * @throws NoRobustRepairExistsException if no feasible robust repair exists
     * @throws IllegalStateException         if SCIP cannot be created
     */
    public RobustSolverResult solveWithResult(@NonNull List<RepairPreparationResult> scenarioPreparations,
                                              @NonNull Set<ControlledLabelKey> forbiddenLabels,
                                              @NonNull RepairCostSpecification costSpecification,
                                              DataFlowDiagramAndDictionary baseModel)
            throws NoRobustRepairExistsException {
        Objects.requireNonNull(scenarioPreparations, "scenarioPreparations must not be null");
        Objects.requireNonNull(forbiddenLabels, "forbiddenLabels must not be null");
        Objects.requireNonNull(costSpecification, "costSpecification must not be null");
        if (scenarioPreparations.isEmpty()) {
            throw new IllegalArgumentException("scenarioPreparations must not be empty");
        }

        ILPSolver.ensureNativeLibrariesLoaded();

        Map<RepairActionKey, Mitigation> canonicalMitigations = collectCanonicalMitigations(scenarioPreparations);
        MPSolver solver = MPSolver.createSolver("SCIP_MIXED_INTEGER_PROGRAMMING");
        if (solver == null) {
            throw new IllegalStateException(
                    "SCIP solver is unavailable: OR-Tools could not create a SCIP_MIXED_INTEGER_PROGRAMMING solver. "
                    + "Check that the OR-Tools native libraries are loaded.");
        }
        Map<RepairActionKey, MPVariable> mitigationVariables =
                createMitigationVariables(solver, canonicalMitigations, forbiddenLabels);

        scenarioPreparations.forEach(scenarioPreparation -> addCoverageConstraints(solver, mitigationVariables, scenarioPreparation));
        addContradictionConstraints(solver, mitigationVariables, scenarioPreparations);
        addRequiredConstraints(solver, mitigationVariables, scenarioPreparations);

        Map<RepairActionKey, ActionCostDescriptor> actionCosts = new ActionCostEvaluator()
                .evaluateAll(mitigationVariables.keySet(), costSpecification, baseModel);
        CostObjectiveEncoder.ConfiguredObjective configuredObjective = new CostObjectiveEncoder()
                .configure(solver, mitigationVariables, costSpecification, actionCosts);

        MPSolver.ResultStatus status = solver.solve();
        if (status != MPSolver.ResultStatus.OPTIMAL && status != MPSolver.ResultStatus.FEASIBLE) {
            throw new NoRobustRepairExistsException(
                    "No robust repair exists for " + scenarioPreparations.size()
                    + " prepared scenarios (SCIP status: " + status + ").",
                    status.name());
        }
        if (status == MPSolver.ResultStatus.FEASIBLE) {
            LOGGER.warn("SCIP returned FEASIBLE instead of OPTIMAL: the selected repair plan is valid but may not be cost-minimal.");
        }

        List<Mitigation> selectedMitigations = mitigationVariables.entrySet().stream()
                .filter(entry -> entry.getValue().solutionValue() > 0.5)
                .map(Map.Entry::getKey)
                .map(canonicalMitigations::get)
                .toList();
        return new RobustSolverResult(selectedMitigations, status.name(),
                solver.numVariables(), solver.numConstraints(), configuredObjective.selectedCostBreakdown());
    }

    /**
     * Keeps the first scenario representative for each action key.
     *
     * @param scenarioPreparations the preparations to collect from
     * @return canonical actions indexed by key
     */
    private Map<RepairActionKey, Mitigation> collectCanonicalMitigations(List<RepairPreparationResult> scenarioPreparations) {
        return scenarioPreparations.stream()
                .flatMap(scenarioPreparation -> scenarioPreparation.allMitigations().stream())
                .collect(Collectors.toMap(
                        this::actionKey,
                        mitigation -> mitigation,
                        (first, ignored) -> first,
                        TreeMap::new
                ));
    }

    /**
     * Creates one binary variable for each canonical action.
     *
     * @param solver               the solver to populate
     * @param canonicalMitigations the available actions
     * @param forbiddenLabels      labels that cannot be removed
     * @return action variables indexed by action key
     */
    private Map<RepairActionKey, MPVariable> createMitigationVariables(MPSolver solver,
                                                                       Map<RepairActionKey, Mitigation> canonicalMitigations, Set<ControlledLabelKey> forbiddenLabels) {
        Map<RepairActionKey, MPVariable> mitigationVariables = new TreeMap<>();
        int i = 0;
        for (Map.Entry<RepairActionKey, Mitigation> entry : canonicalMitigations.entrySet()) {
            // Forbidden actions get an upper bound of 0: the variable still exists (so all
            // coverage / contradiction / required references remain valid), but the solver
            // cannot select it. A coverage set consisting only of forbidden actions therefore
            // becomes infeasible, surfacing as NoRobustRepairExistsException.
            double upperBound = isForbidden(entry.getValue().mitigation(), forbiddenLabels)
                                || isOutsideVocabulary(entry.getValue().mitigation()) ? 0 : 1;
            mitigationVariables.put(entry.getKey(),
                    solver.makeIntVar(0, upperBound, "x_" + i + "_" + safeName(entry.getKey().stableId())));
            i++;
        }
        return mitigationVariables;
    }

    /**
     * Reports whether an action lies outside the declared vocabulary.
     *
     * @param action the candidate action
     * @return {@code true} when a vocabulary is declared and does not contain the action's type
     */
    private boolean isOutsideVocabulary(ActionTerm action) {
        return !allowedActionTypes.isEmpty() && !allowedActionTypes.contains(action.type());
    }

    private boolean isForbidden(ActionTerm action, Set<ControlledLabelKey> forbiddenLabels) {
        if (forbiddenLabels.isEmpty() || action.type() != ActionType.Removing) {
            return false;
        }
        return action.compositeLabels().stream()
                .filter(compositeLabel -> compositeLabel.category() == LabelCategory.Node
                                          || compositeLabel.category() == LabelCategory.OutgoingData)
                .map(compositeLabel -> new ControlledLabelKey(
                        action.domain(), compositeLabel.label().type(), compositeLabel.label().value()))
                .anyMatch(forbiddenLabels::contains);
    }

    /**
     * Adds one coverage clause for each violation.
     *
     * @param solver              the solver to populate
     * @param mitigationVariables the action variables
     * @param scenarioPreparation the scenario coverage alternatives
     */
    private void addCoverageConstraints(MPSolver solver, Map<RepairActionKey, MPVariable> mitigationVariables, RepairPreparationResult scenarioPreparation) {
        List<List<Mitigation>> mitigations = ILPSolver.canonicalRows(scenarioPreparation.mitigations());
        IntStream.range(0, mitigations.size()).forEach(i -> {
            List<Mitigation> coverageAlternatives = mitigations.get(i);
            MPConstraint cover = solver.makeConstraint(1.0, Double.POSITIVE_INFINITY, "cover_" + i);
            coverageAlternatives.forEach(mitigation -> cover.setCoefficient(getMitigationVariable(mitigationVariables, mitigation), 1.0));
        });
    }

    /**
     * Adds mutually exclusive action pairs.
     *
     * @param solver               the solver to populate
     * @param mitigationVariables  the action variables
     * @param scenarioPreparations the scenario contradictions
     */
    private void addContradictionConstraints(MPSolver solver, Map<RepairActionKey, MPVariable> mitigationVariables, List<RepairPreparationResult> scenarioPreparations) {
        int counter = 0;

        Set<ContradictionKey> seenContradictions = new LinkedHashSet<>();

        for (RepairPreparationResult scenarioPreparation : scenarioPreparations) {
            for (List<Mitigation> contradiction : ILPSolver.canonicalRows(scenarioPreparation.contradictions())) {
                if (contradiction.size() != 2) {
                    continue;
                }

                ContradictionKey key = new ContradictionKey(actionKey(contradiction.get(0)),
                        actionKey(contradiction.get(1)));
                if (!seenContradictions.add(key)) {
                    continue;
                }

                MPConstraint conflict = solver.makeConstraint(Double.NEGATIVE_INFINITY, 1.0, "conflict_" + counter++);
                conflict.setCoefficient(getMitigationVariable(mitigationVariables, contradiction.get(0)), 1.0);
                conflict.setCoefficient(getMitigationVariable(mitigationVariables, contradiction.get(1)), 1.0);
            }
        }

    }

    /**
     * Adds required-action clauses.
     *
     * @param solver               the solver to populate
     * @param mitigationVariables  the action variables
     * @param scenarioPreparations the scenario requirements
     */
    private void addRequiredConstraints(MPSolver solver, Map<RepairActionKey, MPVariable> mitigationVariables,
                                        List<RepairPreparationResult> scenarioPreparations) {
        int counter = 0;
        for (int scenarioIndex = 0; scenarioIndex < scenarioPreparations.size(); scenarioIndex++) {
            RepairPreparationResult scenarioPreparation = scenarioPreparations.get(scenarioIndex);

            for (Mitigation mitigation : scenarioPreparation.allMitigations()) {
                counter = enforceMitigationRequirements(solver, mitigationVariables, counter, mitigation, scenarioIndex);
            }
        }
    }

    /**
     * Adds the required-action encoding for one mitigation.
     *
     * @param solver              the solver to populate
     * @param mitigationVariables the action variables
     * @param counter             the next constraint-name suffix
     * @param mitigation          the action with requirements
     * @param scenarioIndex       the scenario index
     * @return the next unused constraint-name suffix
     */
    private int enforceMitigationRequirements(MPSolver solver, Map<RepairActionKey, MPVariable> mitigationVariables, int counter, Mitigation mitigation, int scenarioIndex) {
        MPVariable mitigationVariable = getMitigationVariable(mitigationVariables, mitigation);
        List<MPVariable> requiredAlternatives = new ArrayList<>();
        int clauseIndex = 0;

        for (List<Mitigation> clause : mitigation.required()) {
            MPVariable parentElement = solver.makeIntVar(0, 1,
                    "required_s" + scenarioIndex + "_" + safeName(actionKey(mitigation).stableId()) + "_"
                    + clauseIndex);
            requiredAlternatives.add(parentElement);

            for (Mitigation childMitigation : clause) {
                MPVariable childVariable = getMitigationVariable(mitigationVariables, childMitigation);
                MPConstraint childEnforcement = solver.makeConstraint(Double.NEGATIVE_INFINITY, 0.0,
                        "required_child_" + counter++);
                childEnforcement.setCoefficient(parentElement, 1.0);
                childEnforcement.setCoefficient(childVariable, -1.0);
            }

            int clauseSize = clause.size();
            MPConstraint childrenConstraint = solver.makeConstraint(-(clauseSize - 1), Double.POSITIVE_INFINITY, "required_parent_" + (counter++));
            childrenConstraint.setCoefficient(parentElement, 1.0);

            for (Mitigation childMitigation : clause) {
                childrenConstraint.setCoefficient(getMitigationVariable(mitigationVariables, childMitigation), -1.0);
            }

            clauseIndex++;
        }

        if (!requiredAlternatives.isEmpty()) {
            MPConstraint required = solver.makeConstraint(0.0, Double.POSITIVE_INFINITY, "required_cover_" + counter);
            counter++;
            for (MPVariable parent : requiredAlternatives) {
                required.setCoefficient(parent, 1.0);
            }
            required.setCoefficient(mitigationVariable, -1.0);
        }
        return counter;
    }

    /**
     * Looks up a mitigation's canonical solver variable.
     *
     * @param mitigationVariables the action variables
     * @param mitigation          the mitigation to look up
     * @return its solver variable
     * @throws IllegalStateException if no variable exists
     */
    private MPVariable getMitigationVariable(Map<RepairActionKey, MPVariable> mitigationVariables, Mitigation mitigation) {
        MPVariable variable = mitigationVariables.get(actionKey(mitigation));
        if (variable == null) {
            throw new IllegalStateException("Missing canonical variable for mitigation " + mitigation.mitigation());
        }
        return variable;
    }

    /**
     * Returns a mitigation's canonical action key.
     *
     * @param mitigation the mitigation to identify
     * @return its action key
     */
    private RepairActionKey actionKey(Mitigation mitigation) {
        return RepairActionKeys.from(mitigation);
    }

    /**
     * Converts text into a solver-safe variable-name fragment.
     *
     * @param value the text to sanitize
     * @return an identifier-safe name
     */
    private static String safeName(String value) {
        String sanitized = value.trim().replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_]", "_");
        if (sanitized.isEmpty() || Character.isDigit(sanitized.charAt(0))) {
            return "x_" + sanitized;
        }
        return sanitized;
    }

    /**
     * Order-independent identity for one conflicting pair of canonical actions.
     *
     * @param left  one action in the pair
     * @param right the other action in the pair
     */
    private record ContradictionKey(@NonNull RepairActionKey left, @NonNull RepairActionKey right) {
        /**
         * Normalizes the pair so equal contradiction pairs share one set key.
         *
         * @param left  one action in the pair
         * @param right the other action in the pair
         */
        private ContradictionKey(RepairActionKey left, RepairActionKey right) {
            if (left.compareTo(right) <= 0) {
                this.left = left;
                this.right = right;
            } else {
                this.left = right;
                this.right = left;
            }
        }
    }
}
