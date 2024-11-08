package dev.abunai.confidentiality.mitigation.tests;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.mitigation.ranking.MitigationStrategy;
import dev.abunai.confidentiality.mitigation.ranking.RankerType;
import dev.abunai.confidentiality.mitigation.ranking.RankingAggregationMethod;

/*
 * Class for the evaluation of the scaleability of the mitigation approach
 * Was not used in thesis because the model was too small.
 * */
public abstract class LoginFrameWorkTestBase extends MitigationTestBase {

	private boolean chooseBest = true;

	private boolean useConstraint1 = true;
	private boolean useConstraint2 = true;

	protected abstract String getFolderName();

	protected String getFilesName() {
		return "default";
	}

	protected String customPythonPath() {
		return "python3";
	}

	protected List<Predicate<? super AbstractVertex<?>>> getConstraints() {
		List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();
		if (useConstraint1) {
			constraints.add(it -> {
				return this.retrieveNodeLabels(it).contains("external")
						&& !(this.retrieveAllDataLabels(it).contains("trusted")
								|| this.retrieveNodeLabels(it).contains("trusted"));
			});
		}

		if (useConstraint2) {
			constraints.add(it -> {
				return this.retrieveNodeLabels(it).contains("internal")
						&& !this.retrieveNodeLabels(it).contains("local_logging");
			});
		}

		return constraints;
	}

	@Override
	protected RankerType getRankerType() {
		if (!chooseBest) {
			return RankerType.FAMD;
		}
		return RankerType.LOGISTIC_REGRESSION;
	}

	@Override
	protected RankingAggregationMethod getAggregationMethod() {
		if (!chooseBest) {
			return RankingAggregationMethod.SUM;
		}
		return RankingAggregationMethod.EXPONENTIAL_RANKS;
	}

	@Test
	public void executeMitigationBestAllConstraints() {
		var id = "AllConstraints_Best_" + getFolderName();
		useConstraint1 = true;
		useConstraint2 = true;
		chooseBest = true;
		executeMitigationRuns(MitigationStrategy.INCREASING, id);
		storeMeassurementResult(seeAverageRuntime(), id);
	}

	@Test
	public void executeMitigationWorseAllConstraints() {
		var id = "AllConstraints_Worse_" + getFolderName();
		useConstraint1 = true;
		useConstraint2 = true;
		chooseBest = false;
		executeMitigationRuns(MitigationStrategy.HALF, id);
		storeMeassurementResult(seeAverageRuntime(), id);
	}

	@Test
	public void executeBruteForceAllConstraints() throws Exception {
		var id = "AllConstraints_BruteForce_" + getFolderName();
		useConstraint1 = true;
		useConstraint2 = true;
		executeMitigationRuns(MitigationStrategy.BRUTE_FORCE, id);
		storeMeassurementResult(seeAverageRuntime(), id);
	}

	@Test
	public void executeMitigationBestOneConstraint() {
		var id = "OneConstraint_Best_" + getFolderName();
		chooseBest = true;
		useConstraint1 = true;
		useConstraint2 = false;
		deleteOldMeassurement();
		executeMitigationRuns(MitigationStrategy.INCREASING, id);
		var rt = seeAverageRuntime();
		useConstraint1 = false;
		useConstraint2 = true;
		executeMitigationRuns(MitigationStrategy.INCREASING, id);
		storeMeassurementResult((float) (rt + seeAverageRuntime()) / 2, id);
	}

	@Test
	public void executeMitigationWorseOneConstraint() {
		var id = "OneConstraint_Worse_" + getFolderName();
		chooseBest = false;
		useConstraint1 = true;
		useConstraint2 = false;
		deleteOldMeassurement();
		executeMitigationRuns(MitigationStrategy.HALF, id);
		var rt = seeAverageRuntime();
		useConstraint1 = false;
		useConstraint2 = true;
		executeMitigationRuns(MitigationStrategy.HALF, id);
		storeMeassurementResult((float) (rt + seeAverageRuntime()) / 2, id);
	}

	@Test
	public void executeBruteForceOneConstraint() throws Exception {
		var id = "OneConstraint_BruteForce_" + getFolderName();
		useConstraint1 = true;
		useConstraint2 = false;
		deleteOldMeassurement();
		executeMitigationRuns(MitigationStrategy.BRUTE_FORCE, id);
		var rt = seeAverageRuntime();
		useConstraint1 = false;
		useConstraint2 = true;
		executeMitigationRuns(MitigationStrategy.BRUTE_FORCE, id);
		storeMeassurementResult((float) (rt + seeAverageRuntime()) / 2, id);
	}

	private void executeMitigationRuns(MitigationStrategy strategy, String id) {
		deleteOldMeassurement();
		List<Long> training_durations = new ArrayList<>();
		for (int i = 0; i < MITIGATION_RUNS; i++) {
			var startTime = System.currentTimeMillis();
			mitigationStrategy = strategy;
			if (!mitigationStrategy.equals(MitigationStrategy.BRUTE_FORCE)) {
				createTrainData();
			}
			var duration = System.currentTimeMillis() - startTime;
			training_durations.add(duration);
			createMitigationCandidatesAutomatically();
			duration = System.currentTimeMillis() - startTime;
			storeMeassurement(duration);
		}
		var avg_amount = 2 * MITIGATION_RUNS / 3;
		storeMeassurementResult(
				training_durations.stream().sorted(Comparator.reverseOrder()).limit(avg_amount).reduce(0L, Long::sum)
					/ avg_amount,
				"T"+id);
	}
}