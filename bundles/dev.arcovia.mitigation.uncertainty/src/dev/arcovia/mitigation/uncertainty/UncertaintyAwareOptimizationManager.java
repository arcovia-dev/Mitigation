package dev.arcovia.mitigation.uncertainty;

import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.arcovia.mitigation.cost.ObjectiveCostBreakdown;
import dev.arcovia.mitigation.ilp.*;
import dev.arcovia.mitigation.sat.timeMeasurement;
import dev.arcovia.mitigation.uncertainty.RobustRepairResult.ValidationStatus;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelLoader;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelSpec;
import dev.arcovia.mitigation.uncertainty.materialization.PinWideLabelApplication;
import dev.arcovia.mitigation.uncertainty.output.RepairOutput;
import dev.arcovia.mitigation.uncertainty.pipeline.PipelineStage;
import dev.arcovia.mitigation.uncertainty.pipeline.PipelineTiming;
import dev.arcovia.mitigation.uncertainty.pipeline.ScenarioPreparation;
import dev.arcovia.mitigation.uncertainty.pipeline.ScenarioPreparationService;
import dev.arcovia.mitigation.uncertainty.pruning.ClusterCouplingGuard;
import dev.arcovia.mitigation.uncertainty.pruning.SourceImpactFilter;
import dev.arcovia.mitigation.uncertainty.pruning.SourcePartitioner;
import dev.arcovia.mitigation.uncertainty.solving.NoRobustRepairExistsException;
import dev.arcovia.mitigation.uncertainty.solving.RobustILPSolver;
import dev.arcovia.mitigation.uncertainty.solving.RobustSolverResult;
import dev.arcovia.mitigation.uncertainty.solving.UncertaintyControlledLabelDetector;
import dev.arcovia.mitigation.uncertainty.validation.RobustRepairValidationException;
import dev.arcovia.mitigation.uncertainty.validation.RobustRepairValidationResult;
import dev.arcovia.mitigation.uncertainty.validation.RobustRepairValidator;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jdt.annotation.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;


/**
 * Public facade for uncertainty-aware repair.
 *
 * <p>Without an uncertainty model, it delegates to {@link OptimizationManager}.
 */
public class UncertaintyAwareOptimizationManager extends OptimizationManager {

    private final LoadedUncertaintyModel loadedUncertaintyModel;
    private final List<UncertaintySource> selectedUncertaintySources;
    private final List<Constraint> configuredConstraints;
    private final DataFlowDiagramAndDictionary uncertaintyBaseModel;
    private boolean impactPruningEnabled = true;
    private boolean cyclicModelsAllowed;
    private boolean partitioningEnabled = true;
    private List<Mitigation> robustResult = List.of();
    private boolean robustRepairExecuted = false;
    private RobustRepairResult robustRepairResult;
    private RepairOutput repairOutput = RepairOutput.disabled();

    /**
     * Creates a baseline-compatible manager for an in-memory model and DSL constraints.
     *
     * @param dfd         the model to repair
     * @param constraints the confidentiality constraints expressed in the analysis DSL
     */
    public UncertaintyAwareOptimizationManager(@NonNull DataFlowDiagramAndDictionary dfd,
                                               @NonNull List<AnalysisConstraint> constraints) {
        super(dfd, constraints);
        this.loadedUncertaintyModel = null;
        this.selectedUncertaintySources = List.of();
        this.configuredConstraints = constraints.stream()
                .map(Constraint::new)
                .toList();
        this.uncertaintyBaseModel = dfd;
    }

    /**
     * Creates a baseline-compatible manager for an in-memory model and prepared constraints.
     *
     * @param dfd                      the model to repair
     * @param constraints              the already prepared repair constraints
     * @param addAdditionalMitigations whether baseline additional mitigations are enabled
     */
    public UncertaintyAwareOptimizationManager(
            @NonNull DataFlowDiagramAndDictionary dfd,
            @NonNull List<Constraint> constraints,
            boolean addAdditionalMitigations
    ) {
        super(dfd, constraints, addAdditionalMitigations);
        this.loadedUncertaintyModel = null;
        this.selectedUncertaintySources = List.of();
        this.configuredConstraints = List.copyOf(constraints);
        this.uncertaintyBaseModel = dfd;
    }

    /**
     * Creates a baseline-compatible manager that loads a model from disk for DSL constraints.
     *
     * @param dfdLocation the location accepted by the baseline optimization manager
     * @param constraints the confidentiality constraints expressed in the analysis DSL
     */
    public UncertaintyAwareOptimizationManager(@NonNull String dfdLocation,
                                               @NonNull List<AnalysisConstraint> constraints) {
        super(dfdLocation, constraints);
        this.loadedUncertaintyModel = null;
        this.selectedUncertaintySources = List.of();
        this.configuredConstraints = constraints.stream()
                .map(Constraint::new)
                .toList();
        this.uncertaintyBaseModel = null;
    }

    /**
     * Creates a baseline-compatible manager that loads a model from disk for prepared constraints.
     *
     * @param dfdLocation              the location accepted by the baseline optimisation manager
     * @param constraints              the already prepared repair constraints
     * @param addAdditionalMitigations whether baseline additional mitigations are enabled
     */
    public UncertaintyAwareOptimizationManager(@NonNull String dfdLocation, @NonNull List<Constraint> constraints,
                                               boolean addAdditionalMitigations) {
        super(dfdLocation, constraints, addAdditionalMitigations);
        this.loadedUncertaintyModel = null;
        this.selectedUncertaintySources = List.of();
        this.configuredConstraints = List.copyOf(constraints);
        this.uncertaintyBaseModel = null;
    }

    /**
     * Creates a robust-repair manager considering every source declared by a loaded model.
     *
     * @param loadedModel              the loaded uncertainty model
     * @param constraints              the repair constraints to enforce in every scenario
     * @param addAdditionalMitigations whether baseline additional mitigations are enabled
     */
    public UncertaintyAwareOptimizationManager(
            @NonNull LoadedUncertaintyModel loadedModel,
            @NonNull List<Constraint> constraints,
            boolean addAdditionalMitigations
    ) {
        this(loadedModel, constraints, addAdditionalMitigations, loadedModel.sources());
    }

    /**
     * Creates a robust-repair manager restricted to a caller-selected subset of sources.
     *
     * @param loadedModel                the loaded uncertainty model
     * @param constraints                the repair constraints to enforce
     * @param addAdditionalMitigations   whether baseline additional mitigations are enabled
     * @param selectedUncertaintySources the declared sources included in the scenario space
     */
    public UncertaintyAwareOptimizationManager(
            @NonNull LoadedUncertaintyModel loadedModel, @NonNull List<Constraint> constraints,
            boolean addAdditionalMitigations,
            @NonNull List<UncertaintySource> selectedUncertaintySources
    ) {
        super(Objects.requireNonNull(loadedModel, "loadedModel must not be null").baseModel(), constraints,
                addAdditionalMitigations);
        this.loadedUncertaintyModel = loadedModel;
        this.selectedUncertaintySources = List.copyOf(
                Objects.requireNonNull(selectedUncertaintySources, "selectedUncertaintySources must not be null"));
        this.configuredConstraints = List.copyOf(constraints);
        this.uncertaintyBaseModel = loadedModel.baseModel();
    }

    /**
     * Loads an uncertainty model and creates a robust-repair manager for all of its sources.
     *
     * @param uncertaintyModelSpec     the three resource paths that identify the uncertainty model
     * @param constraints              the repair constraints to enforce in every scenario
     * @param addAdditionalMitigations whether baseline additional mitigations are enabled
     */
    public UncertaintyAwareOptimizationManager(
            @NonNull UncertaintyModelSpec uncertaintyModelSpec,
            @NonNull List<Constraint> constraints,
            boolean addAdditionalMitigations
    ) {
        this(new UncertaintyModelLoader().load(uncertaintyModelSpec), constraints, addAdditionalMitigations);
    }

    /**
     * Loads an uncertainty model and creates a robust-repair manager for selected sources only.
     *
     * @param uncertaintyModelSpec       the three resource paths that identify the uncertainty model
     * @param constraints                the repair constraints to enforce in every scenario
     * @param addAdditionalMitigations   whether baseline additional mitigations are enabled
     * @param selectedUncertaintySources the declared sources included in the scenario space
     */
    public UncertaintyAwareOptimizationManager(
            @NonNull UncertaintyModelSpec uncertaintyModelSpec,
            @NonNull List<Constraint> constraints,
            boolean addAdditionalMitigations,
            @NonNull List<UncertaintySource> selectedUncertaintySources
    ) {
        this(new UncertaintyModelLoader().load(uncertaintyModelSpec), constraints, addAdditionalMitigations,
                selectedUncertaintySources);
    }

    /**
     * Determines whether this manager should execute the robust path.
     *
     * @return {@code true} when a loaded uncertainty model is configured; {@code false} for baseline delegation
     */
    private boolean hasLoadedUncertaintyModel() {
        return this.loadedUncertaintyModel != null;
    }

    /**
     * Returns the result from the most recent robust-repair execution.
     *
     * @return the robust result, or empty when no robust repair has completed
     */
    public Optional<RobustRepairResult> getRobustRepairResult() {
        return Optional.ofNullable(robustRepairResult);
    }

    /**
     * Enables output persistence for subsequent robust repairs.
     *
     * @param repairOutput the configured output sink
     */
    public void enableRepairOutput(@NonNull RepairOutput repairOutput) {
        this.repairOutput = Objects.requireNonNull(repairOutput, "repairOutput must not be null");
    }

    /**
     * Toggles A2 independence partitioning (default on). When disabled, the repair always solves and
     * validates over the full scenario product of all relevant sources. Intended for tests that
     * compare the per-cluster path against the whole-set baseline; production callers leave it on.
     *
     * @param partitioningEnabled {@code true} to allow safe decomposition; {@code false} for the full product
     */
    public void setPartitioningEnabled(boolean partitioningEnabled) {
        this.partitioningEnabled = partitioningEnabled;
    }

    /**
     * Toggles A1 impact pruning (default on).
     *
     * @param impactPruningEnabled {@code true} to remove sources that cannot affect a constraint
     */
    public void setImpactPruningEnabled(boolean impactPruningEnabled) {
        this.impactPruningEnabled = impactPruningEnabled;
    }

    /**
     * Allows the repair to analyse cyclic models (default off).
     * <p>
     * Transpose flow graph extraction unrolls cycles heuristically, so the default refuses such
     * models. A caller that enables this accepts the heuristic and takes on the obligation to
     * report the results as a stratum of their own, separate from acyclic results.
     *
     * @param cyclicModelsAllowed {@code true} to analyse cyclic models instead of rejecting them
     */
    public void setCyclicModelsAllowed(boolean cyclicModelsAllowed) {
        this.cyclicModelsAllowed = cyclicModelsAllowed;
    }

    /**
     * Executes the robust repair pipeline of the Approach chapter. Stage 1 (loading) has
     * already happened in the constructor; this method runs stages 2--7 in order, each
     * delegated to the component of the corresponding pipeline package.
     *
     * @param timer optional baseline-compatible timing recorder, or {@code null}
     * @return the repaired base model after successful validation
     * @throws Exception if solving, applying, or validation fails
     */
    private DataFlowDiagramAndDictionary repairLoadedUncertainty(timeMeasurement timer) throws Exception {
        return repairLoadedUncertainty(timer, PipelineTiming.none());
    }

    /**
     * Runs stages 2--7 and reports each stage boundary to an optional timing sink.
     *
     * @param timer          the legacy timing recorder, or {@code null}
     * @param pipelineTiming the stage timing sink
     * @return the repaired base model after successful validation
     * @throws Exception if solving, applying, or validation fails
     */
    private DataFlowDiagramAndDictionary repairLoadedUncertainty(timeMeasurement timer,
                                                                 PipelineTiming pipelineTiming) throws Exception {
        Objects.requireNonNull(pipelineTiming, "pipelineTiming must not be null");
        RepairOutput output = repairOutput;
        output.record("start-robust-repair", () -> "selectedSources=" + selectedUncertaintySources.size()
                                                   + ", constraints=" + configuredConstraints.size());
        if (timer != null) {
            timer.start();
        }

        // A1 -- impact pruning: drop sources that cannot affect any constraint before enumerating,
        // so the Cartesian product (and the post-repair validation) is taken only over relevant
        // sources. The SAME pruned set must feed both preparation and validation, or validation would
        // re-introduce scenarios the repair never considered.
        List<UncertaintySource> relevantSources = impactPruningEnabled
                ? new SourceImpactFilter().retainImpactful(
                loadedUncertaintyModel.baseModel(), selectedUncertaintySources)
                : selectedUncertaintySources;
        output.record("prune-sources", () -> "selectedSources=" + selectedUncertaintySources.size()
                                             + ", relevantSources=" + relevantSources.size());

        // Stages 2--4: enumerate the source selections, materialize each selection, and derive
        // its violation coverage and contradiction constraints. ScenarioPreparationService is the
        // explicit hand-off between those three packages and yields one preparation per scenario.
        // A2 -- independence partitioning. Split the relevant sources into clusters that share no
        // constraint-reachable element (SourcePartitioner), then, when the partition is free of
        // cross-cluster ILP coupling (ClusterCouplingGuard), solve and validate over the per-cluster
        // union of scenarios (∑ 2^{n_i}) instead of the full product (∏ 2^{Σ n_i}). The shared
        // RepairActionKey variables compose the per-cluster plans automatically. Any coupling, or a
        // single cluster, falls back to the whole-set solve -- byte-identical to the pre-A2 behavior.
        ScenarioPreparationService scenarioPreparationService =
                new ScenarioPreparationService(cyclicModelsAllowed);
        RobustRepairValidator repairValidator = new RobustRepairValidator(scenarioPreparationService);

        List<List<UncertaintySource>> clusters = partitioningEnabled
                ? new SourcePartitioner().partition(uncertaintyBaseModel, relevantSources)
                : List.of(relevantSources);
        List<List<ScenarioPreparation>> perClusterPreparations = clusters.stream()
                .map(cluster -> scenarioPreparationService.prepare(
                        uncertaintyBaseModel, cluster, configuredConstraints, pipelineTiming))
                .toList();
        boolean decomposed = clusters.size() > 1 && new ClusterCouplingGuard().isSafeToDecompose(
                perClusterPreparations.stream()
                        .map(cluster -> cluster.stream().map(ScenarioPreparation::preparation).toList())
                        .toList());
        List<List<UncertaintySource>> clustersUsed = decomposed ? clusters : List.of(relevantSources);
        List<ScenarioPreparation> scenarioPreparations = decomposed
                ? perClusterPreparations.stream().flatMap(List::stream).toList()
                : clusters.size() == 1
                ? perClusterPreparations.get(0)
                : scenarioPreparationService.prepare(
                uncertaintyBaseModel, relevantSources, configuredConstraints, pipelineTiming);
        output.record("partition-sources", () -> "clusters=" + clusters.size()
                                                 + ", decomposed=" + decomposed
                                                 + ", scenariosConsidered=" + scenarioPreparations.size());
        int preRepairViolationCount = repairValidator.fromPreparations(scenarioPreparations).totalViolationCount();
        List<String> consideredScenarioIds = scenarioPreparations.stream()
                .map(preparation -> preparation.scenario().id())
                .toList();
        output.record("prepare-scenarios", () -> "scenarioCount=" + scenarioPreparations.size()
                                                 + ", scenarioIds=" + consideredScenarioIds
                                                 + ", preRepairViolations=" + preRepairViolationCount
                                                 + ", candidateActions=" + countCandidateActions(scenarioPreparations)
                                                 + ", coverageConstraints=" + countCoverageConstraints(scenarioPreparations));
        if (timer != null) {
            timer.constraints();
            timer.analysis();
        }

        // Stage 5 -- solve the joint ILP over all scenario preparations.
        pipelineTiming.start(PipelineStage.SOLVING);
        RobustSolverResult solverResult;
        try {
            solverResult = solveJointIlp(scenarioPreparations, output);
        } finally {
            pipelineTiming.stop(PipelineStage.SOLVING);
        }
        robustResult = solverResult.selectedMitigations();
        robustRepairExecuted = true;
        double totalCost = solverResult.objectiveValue();
        output.record("solve-robust-ilp-completed", () -> "solverStatus=" + solverResult.solverStatus()
                                                          + ", selectedActions=" + robustResult.size()
                                                          + ", totalCost=" + totalCost);
        if (timer != null) {
            timer.solving();
        }

        // Stage 6 -- apply the selected actions to a dedicated copy of the base model.
        List<ActionTerm> selectedActions = robustResult.stream()
                .map(Mitigation::mitigation)
                .toList();
        output.record("apply-selected-actions", () -> "actionCount=" + selectedActions.size()
                                                      + ", actions=" + selectedActions);
        pipelineTiming.start(PipelineStage.APPLICATION);
        DataFlowDiagramAndDictionary repairedModel;
        try {
            repairedModel = applyToRepairedCopy(selectedActions);
        } finally {
            pipelineTiming.stop(PipelineStage.APPLICATION);
        }

        // Stage 7 -- validate the repaired model. When the sources were decomposed, each cluster is
        // validated over its own scenario sub-space; because the clusters are independent, per-cluster
        // validity implies global validity (∑ ⇒ ∏). A single cluster validates over the full product.
        output.record("validate-repaired-model", () -> "scenarioCount=" + consideredScenarioIds.size());
        pipelineTiming.start(PipelineStage.VALIDATION);
        RobustRepairValidationResult postRepairValidation;
        try {
            List<ScenarioPreparation> postRepairPreparations = clustersUsed.stream()
                    .flatMap(cluster -> scenarioPreparationService.prepare(
                            repairedModel, cluster, configuredConstraints).stream())
                    .toList();
            postRepairValidation = repairValidator.fromPreparations(postRepairPreparations);
        } finally {
            pipelineTiming.stop(PipelineStage.VALIDATION);
        }
        ValidationStatus validationStatus = postRepairValidation.passed()
                ? ValidationStatus.PASSED
                : ValidationStatus.FAILED;
        output.record("validate-repaired-model-completed", () -> "validationStatus=" + validationStatus
                                                                 + ", postRepairViolations="
                                                                 + postRepairValidation.totalViolationCount());
        robustRepairResult = new RobustRepairResult(
                repairedModel,
                selectedActions,
                totalCost,
                solverResult.costBreakdown(),
                consideredScenarioIds,
                preRepairViolationCount,
                postRepairValidation.totalViolationCount(),
                solverResult.solverStatus(),
                validationStatus);

        output.persist(robustRepairResult);
        if (timer != null) {
            timer.stop();
        }
        if (!postRepairValidation.passed()) {
            throw new RobustRepairValidationException(robustRepairResult);
        }
        return repairedModel;
    }

    /**
     * Stage 5: detects uncertainty-controlled labels (whose removal materialization would
     * overwrite) and solves the joint ILP over all scenario preparations with those removals
     * forbidden.
     *
     * @param scenarioPreparations the prepared scenarios to solve jointly
     * @param output               the trace sink for stage-5 progress
     * @return the robust solver result
     * @throws NoRobustRepairExistsException if the joint ILP is infeasible
     */
    private RobustSolverResult solveJointIlp(List<ScenarioPreparation> scenarioPreparations,
                                             RepairOutput output) throws NoRobustRepairExistsException {
        Set<UncertaintyControlledLabelDetector.ControlledLabelKey> forbiddenLabels =
                new UncertaintyControlledLabelDetector().detect(
                        uncertaintyBaseModel,
                        scenarioPreparations.stream().map(preparation -> preparation.scenario().model()).toList());
        output.record("solve-robust-ilp", () -> "scenarioCount=" + scenarioPreparations.size()
                                                + ", forbiddenControlledLabels=" + forbiddenLabels.size());
        return new RobustILPSolver().solveWithResult(scenarioPreparations.stream()
                .map(ScenarioPreparation::preparation)
                .toList(), forbiddenLabels, getRepairCostSpecification(), uncertaintyBaseModel);
    }

    /**
     * Stage 6: transactional action application. The actions are applied to a deep copy of
     * the base model, so the caller's input model stays untouched; the repaired copy replaces
     * the input only by being returned after validation passes. On validation failure the
     * (failed) repaired copy remains available through the exception's
     * {@link RobustRepairResult} while the input model remains unmodified.
     *
     * @param selectedActions the actions selected by the robust solver
     * @return a deep-copied model with the selected actions applied
     */
    private DataFlowDiagramAndDictionary applyToRepairedCopy(List<ActionTerm> selectedActions) {
        DataFlowDiagramAndDictionary repairedModel = deepCopy(uncertaintyBaseModel);
        applyActions(repairedModel, selectedActions);
        return repairedModel;
    }

    /**
     * Applies the actions as the baseline does, then completes outgoing-data label actions on the
     * assignments a selected behavior source can swap in for the same pin, so that the label
     * survives materialization.
     */
    @Override
    protected void applyActions(DataFlowDiagramAndDictionary dfd, List<ActionTerm> actions) {
        super.applyActions(dfd, actions);
        PinWideLabelApplication.complete(dfd.dataDictionary(), actions, selectedUncertaintySources);
    }

    /**
     * Deep-copies the diagram and dictionary with a single shared {@link EcoreUtil.Copier} so
     * that cross-references between the two trees (node properties, behavior assignments,
     * labels) point into the copied dictionary rather than back into the original.
     *
     * @param model the diagram and dictionary to copy
     * @return an independent diagram-and-dictionary pair with preserved cross-references
     */
    private static DataFlowDiagramAndDictionary deepCopy(DataFlowDiagramAndDictionary model) {
        EcoreUtil.Copier copier = new EcoreUtil.Copier();
        DataFlowDiagram diagramCopy = (DataFlowDiagram) copier.copy(model.dataFlowDiagram());
        DataDictionary dictionaryCopy = (DataDictionary) copier.copy(model.dataDictionary());
        copier.copyReferences();
        return new DataFlowDiagramAndDictionary(diagramCopy, dictionaryCopy);
    }

    /**
     * Counts distinct canonical candidate actions across all prepared scenarios for trace output.
     *
     * @param scenarioPreparations the prepared scenarios to inspect
     * @return the number of distinct semantic repair actions
     */
    private int countCandidateActions(List<ScenarioPreparation> scenarioPreparations) {
        return (int) scenarioPreparations.stream()
                .flatMap(scenarioPreparation -> scenarioPreparation.preparation().allMitigations().stream())
                .map(RepairActionKeys::from)
                .distinct()
                .count();
    }

    /**
     * Counts coverage clauses, one for each scenario-and-violation pair.
     *
     * @param scenarioPreparations the prepared scenarios to inspect
     * @return the total number of coverage clauses
     */
    private int countCoverageConstraints(List<ScenarioPreparation> scenarioPreparations) {
        return scenarioPreparations.stream()
                .map(ScenarioPreparation::preparation)
                .mapToInt(preparation -> preparation.mitigations().size())
                .sum();
    }


    /**
     * Returns the latest robust objective breakdown or the baseline breakdown.
     *
     * @return the active repair's objective breakdown, or empty before repair
     */
    @Override
    public Optional<ObjectiveCostBreakdown> getObjectiveCostBreakdown() {
        return robustRepairExecuted
                ? Optional.of(robustRepairResult.costBreakdown())
                : super.getObjectiveCostBreakdown();
    }


    /**
     * Repairs the configured model using the robust pipeline when uncertainty is present.
     *
     * @return the repaired model from the robust or baseline path
     * @throws Exception if the selected repair path fails
     */
    @Override
    public DataFlowDiagramAndDictionary repair() throws Exception {
        if (hasLoadedUncertaintyModel()) {
            return repairLoadedUncertainty(null);
        }
        return super.repair();
    }

    /**
     * Repairs the configured model and records the baseline-compatible phase timings.
     *
     * @param timer the timing recorder supplied by the caller
     * @return the repaired model from the robust or baseline path
     * @throws Exception if the selected repair path fails
     */
    @Override
    public DataFlowDiagramAndDictionary repair(timeMeasurement timer) throws Exception {
        if (hasLoadedUncertaintyModel()) {
            return repairLoadedUncertainty(timer);
        }
        return super.repair(timer);
    }

    /**
     * Repairs the configured uncertainty model and reports the stage boundaries.
     *
     * @param pipelineTiming the stage timing sink
     * @return the repaired model
     * @throws Exception if the selected repair path fails
     */
    public DataFlowDiagramAndDictionary repair(@NonNull PipelineTiming pipelineTiming) throws Exception {
        if (hasLoadedUncertaintyModel()) {
            return repairLoadedUncertainty(null, pipelineTiming);
        }
        return super.repair();
    }

}
