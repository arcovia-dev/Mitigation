package dev.abunai.confidentiality.mitigation.tests.sum;

import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.mitigation.ranking.RankerType;
import dev.abunai.confidentiality.mitigation.ranking.RankingAggregationMethod;
import dev.abunai.confidentiality.mitigation.tests.MitigationModelTestBase;

public class SumFactorAnalysisMitigationTest extends MitigationModelTestBase {

	
	protected RankerType getRankerType() {
		return RankerType.FAMD;
	}

	protected RankingAggregationMethod getAggregationMethod() {
		return RankingAggregationMethod.LINEAR_RANKS;
	}

	@Test
	public void executeMitigation() {
		// For meassuring at least 30 runs are required
		deleteOldMeassurement();
		for (int i = 0; i < MITIGATION_RUNS; i++) {
			var startTime = System.currentTimeMillis();
			createTrainData();
			createMitigationCandidatesAutomatically();
			var duration = System.currentTimeMillis() - startTime;
			storeMeassurement(duration);
		}
	}
}