package dev.abunai.confidentiality.mitigation.tests.ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.mitigation.tests.MitigationStrategy;
import dev.abunai.confidentiality.mitigation.tests.MitigationTestBase;
import dev.abunai.confidentiality.mitigation.ranking.RankerType;
import dev.abunai.confidentiality.mitigation.ranking.RankingAggregationMethod;

public class InterfaceUncertaintyMitigationTest extends MitigationTestBase {

	protected String getFolderName() {
		return "DFDInterfaceUncertaintyMitigation";
	}

	protected String getFilesName() {
		return "int";
	}
	
	protected Optional<String> customPythonPath() {
		return Optional.empty();
	}
	
	@Override
	protected RankerType getRankerType() {
		return RankerType.RANDOM_FOREST;
	}

	@Override
	protected RankingAggregationMethod getAggregationMethod() {
		return RankingAggregationMethod.EXPONENTIAL_RANKS;
	}

	protected List<Predicate<? super AbstractVertex<?>>> getConstraints() {
		List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();

		constraints.add(it -> {
			boolean vio = this.retrieveNodeLabels(it).contains("Develop")
					&& this.retrieveDataLabels(it).contains("Personal");
			return vio;
		});
		constraints.add(it -> {
			boolean vio = this.retrieveNodeLabels(it).contains("nonEU")
					&& this.retrieveDataLabels(it).contains("Personal");
			return vio;
		});
		return constraints;
	}

	@Test
	public void executeMitigation() {
		// For meassuring at least 30 runs are required
		deleteOldMeassurement();
		for (int i = 0; i < MITIGATION_RUNS; i++) {
			var startTime = System.currentTimeMillis();
			mitigationStrategy = MitigationStrategy.INCREASING;
			createTrainData();
			createMitigationCandidatesAutomatically();
			var duration = System.currentTimeMillis() - startTime;
			storeMeassurement(duration);
		}
	}
	
	@Test
	public void executeBruteForce() throws Exception {
		// For meassuring at least 30 runs are required
		deleteOldMeassurement();
		for (int i = 0; i < MITIGATION_RUNS; i++) {
			var startTime = System.currentTimeMillis();
			mitigationStrategy = MitigationStrategy.BRUTE_FORCE;
			createMitigationCandidatesAutomatically();
			var duration = System.currentTimeMillis() - startTime;
			storeMeassurement(duration);
		}
	}
}
