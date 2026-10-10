package dev.arcovia.mitigation.uncertainty.evaluation.scalability;

import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.ilp.Mitigation;
import dev.arcovia.mitigation.ilp.RepairActionKeys;
import dev.arcovia.mitigation.uncertainty.UncertaintyAwareOptimizationManager;
import dev.arcovia.mitigation.uncertainty.evaluation.fixtures.FactorSweepFixture;
import dev.arcovia.mitigation.uncertainty.evaluation.fixtures.FactorSweepFixture.Spec;
import dev.arcovia.mitigation.uncertainty.evaluation.fixtures.ScalabilitySubjectFixtures;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import dev.arcovia.mitigation.uncertainty.pipeline.ScenarioPreparation;
import dev.arcovia.mitigation.uncertainty.pipeline.ScenarioPreparationService;
import dev.arcovia.mitigation.uncertainty.preparation.RepairPreparationResult;
import dev.arcovia.mitigation.uncertainty.solving.NoRobustRepairExistsException;
import dev.arcovia.mitigation.uncertainty.solving.RobustILPSolver;
import dev.arcovia.mitigation.uncertainty.solving.RobustSolverResult;
import dev.arcovia.mitigation.uncertainty.solving.UncertaintyControlledLabelDetector;
import dev.arcovia.mitigation.uncertainty.validation.RobustRepairValidationException;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;

import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

interface FactorSubject {

    String scope();

    FactorSubject.Structure structure(int value);

    long executeUs(int value) throws Exception;

    static Structure failedStructure(Throwable failure) {
        return new Structure(-1, -1, -1, -1, -1, -1, failure.getClass().getSimpleName(), 0, 0, 0);
    }

    private static long liveHeapBytes() {
        System.gc();
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    static FactorSubject generated(IntFunction<Spec> specs) {
        return new FactorSubject() {
            @Override
            public String scope() {
                return "END_TO_END";
            }

            @Override
            public Structure structure(int value) {
                Spec spec = specs.apply(value);
                return endToEndStructure(FactorSweepFixture.build(spec),
                        FactorSweepFixture.constraints(spec.choicesPerSource()));
            }

            @Override
            public long executeUs(int value) throws Exception {
                Spec spec = specs.apply(value);
                return repairUs(FactorSweepFixture.build(spec),
                        FactorSweepFixture.constraints(spec.choicesPerSource()), null);
            }
        };
    }

    /**
     * The published microSecEnD architecture, growing the number of its declared sources selected.
     */
    static FactorSubject published() {
        return new FactorSubject() {
            @Override
            public String scope() {
                return "END_TO_END";
            }

            @Override
            public Structure structure(int value) {
                LoadedUncertaintyModel model = ScalabilitySubjectFixtures.publishedModel();
                return endToEndStructure(model, ScalabilitySubjectFixtures.getAuthenticationConstraints(),
                        ScalabilitySubjectFixtures.publishedSources(model, value));
            }

            @Override
            public long executeUs(int value) throws Exception {
                LoadedUncertaintyModel model = ScalabilitySubjectFixtures.publishedModel();
                return repairUs(model, ScalabilitySubjectFixtures.getAuthenticationConstraints(),
                        ScalabilitySubjectFixtures.publishedSources(model, value));
            }
        };
    }

    static FactorSubject modelGrowth() {
        return new FactorSubject() {
            @Override
            public String scope() {
                return "PREPARE_AND_SOLVE";
            }

            @Override
            public Structure structure(int value) {
                return prepareAndSolve(ScalabilitySubjectFixtures.duplicatedMotifs(value));
            }

            @Override
            public long executeUs(int value) {
                Structure structure = prepareAndSolve(ScalabilitySubjectFixtures.duplicatedMotifs(value));
                return structure.prepareUs() + structure.solveUs();
            }

            private Structure prepareAndSolve(DataFlowDiagramAndDictionary model) {
                long heapBaseBytes = liveHeapBytes();
                long prepareStart = System.nanoTime();
                List<ScenarioPreparation> preparations = new ScenarioPreparationService()
                        .prepare(model, List.of(), ScalabilitySubjectFixtures.motifConstraints());
                long prepareUs = (System.nanoTime() - prepareStart) / 1_000;
                long solveStart = System.nanoTime();
                try {
                    RobustSolverResult result = new RobustILPSolver().solveWithResult(
                            preparations.stream().map(ScenarioPreparation::preparation).toList(),
                            new UncertaintyControlledLabelDetector().detect(model,
                                    preparations.stream().map(p -> p.scenario().model()).toList()));
                    return structureOf(preparations, result.variableCount(), result.constraintCount(),
                            result.solverStatus(), prepareUs, (System.nanoTime() - solveStart) / 1_000,
                            Math.max(0, liveHeapBytes() - heapBaseBytes));
                } catch (Exception | OutOfMemoryError failure) {
                    return structureOf(preparations, -1, -1, failure.getClass().getSimpleName(),
                            prepareUs, (System.nanoTime() - solveStart) / 1_000,
                            Math.max(0, liveHeapBytes() - heapBaseBytes));
                }
            }
        };
    }

    /**
     * A constructed candidate set with no model at all, so only the cost of formulating and solving
     * the program varies.
     */
    static FactorSubject actionSpace() {
        return new FactorSubject() {
            @Override
            public String scope() {
                return "STAGE_5_ONLY";
            }

            @Override
            public Structure structure(int value) {
                long solveStart = System.nanoTime();
                try {
                    RobustSolverResult result = solve(value);
                    return new Structure(1, 1, 1, value, result.variableCount(), result.constraintCount(),
                            result.solverStatus(), 0, (System.nanoTime() - solveStart) / 1_000, 0);
                } catch (Exception | OutOfMemoryError failure) {
                    return new Structure(1, 1, 1, value, -1, -1, failure.getClass().getSimpleName(),
                            0, (System.nanoTime() - solveStart) / 1_000, 0);
                }
            }

            @Override
            public long executeUs(int value) throws Exception {
                long start = System.nanoTime();
                solve(value);
                return (System.nanoTime() - start) / 1_000;
            }

            private RobustSolverResult solve(int actionCount) throws Exception {
                List<Mitigation> candidates = ScalabilitySubjectFixtures.candidateActions(actionCount);
                return new RobustILPSolver().solveWithResult(List.of(new RepairPreparationResult(
                        List.of(), Set.of(), List.of(candidates), candidates, List.of())), Set.of());
            }
        };
    }

    private static Structure endToEndStructure(LoadedUncertaintyModel model, List<Constraint> constraints) {
        return endToEndStructure(model, constraints, model.sources());
    }

    private static Structure endToEndStructure(LoadedUncertaintyModel model, List<Constraint> constraints,
                                               List<UncertaintySource> sources) {
        long heapBaseBytes = liveHeapBytes();
        long prepareStart = System.nanoTime();
        List<ScenarioPreparation> preparations = new ScenarioPreparationService()
                .prepare(model.baseModel(), sources, constraints);
        long prepareUs = (System.nanoTime() - prepareStart) / 1_000;
        long solveStart = System.nanoTime();
        try {
            var forbidden = new UncertaintyControlledLabelDetector().detect(model.baseModel(),
                    preparations.stream().map(p -> p.scenario().model()).toList());
            RobustSolverResult result = new RobustILPSolver().solveWithResult(
                    preparations.stream().map(ScenarioPreparation::preparation).toList(), forbidden);
            return structureOf(preparations, result.variableCount(), result.constraintCount(),
                    result.solverStatus(), prepareUs, (System.nanoTime() - solveStart) / 1_000,
                    Math.max(0, liveHeapBytes() - heapBaseBytes));
        } catch (Exception | OutOfMemoryError failure) {
            return structureOf(preparations, -1, -1, failure.getClass().getSimpleName(),
                    prepareUs, (System.nanoTime() - solveStart) / 1_000,
                    Math.max(0, liveHeapBytes() - heapBaseBytes));
        }
    }

    private static Structure structureOf(List<ScenarioPreparation> preparations, int ilpVariables,
                                         int ilpConstraints, String solverStatus, long prepareUs,
                                         long solveUs, long retainedBytes) {
        return new Structure(preparations.size(),
                preparations.stream().mapToInt(p -> p.preparation().violatingNodes().size()).sum(),
                preparations.stream().mapToInt(p -> p.preparation().mitigations().size()).sum(),
                (int) preparations.stream()
                        .flatMap(p -> p.preparation().allMitigations().stream())
                        .map(RepairActionKeys::from)
                        .distinct()
                        .count(),
                ilpVariables, ilpConstraints, solverStatus, prepareUs, solveUs, retainedBytes);
    }

    /**
     * Runs the whole robust pipeline at once and returns its duration.
     */
    private static long repairUs(LoadedUncertaintyModel model, List<Constraint> constraints,
                                 List<UncertaintySource> sources) throws Exception {
        var manager = new UncertaintyAwareOptimizationManager(model, constraints, false,
                sources == null ? model.sources() : sources);
        manager.setPartitioningEnabled(false);
        long start = System.nanoTime();
        try {
            manager.repair();
        } catch (NoRobustRepairExistsException | RobustRepairValidationException expected) {
            return (System.nanoTime() - start) / 1_000;
        }
        return (System.nanoTime() - start) / 1_000;
    }

    /**
     * Structural size of one driver value, and the two stage timings behind it.
     *
     * @param scenarioCount    materialized scenarios
     * @param violations       violating nodes summed over all scenarios
     * @param coverageRows     coverage constraints in the program
     * @param canonicalActions distinct canonical actions across all scenarios
     * @param ilpVariables     binary action variables, or -1 when the solve did not complete
     * @param ilpConstraints   constraint rows, or -1 when the solve did not complete
     * @param solverStatus     the solver status, or the failure's type
     * @param prepareUs        preparation duration in microseconds
     * @param solveUs          solve duration in microseconds
     */
    record Structure(int scenarioCount, int violations, int coverageRows, int canonicalActions,
                     int ilpVariables, int ilpConstraints, String solverStatus,
                     long prepareUs, long solveUs, long retainedBytes) {
    }
}
