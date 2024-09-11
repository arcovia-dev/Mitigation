package dev.abunai.confidentiality.mitigation.tests.top3;


import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.mitigation.tests.MitigationModelTestBase;
import dev.abunai.confidentiality.mitigation.ranking.RankerType;
import dev.abunai.confidentiality.mitigation.ranking.RankingAggregationMethod;

public class Top3RandomForestMitigationTest extends MitigationModelTestBase {

	protected RankerType getRankerType() {
		return RankerType.RANDOM_FOREST;
	}

	protected RankingAggregationMethod getAggregationMethod() {
		return RankingAggregationMethod.EXPONENTIAL_RANKS;
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
