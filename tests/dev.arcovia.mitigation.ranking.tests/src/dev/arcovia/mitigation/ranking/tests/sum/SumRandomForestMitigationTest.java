package dev.arcovia.mitigation.ranking.tests.sum;

import dev.arcovia.mitigation.ranking.RankerType;
import dev.arcovia.mitigation.ranking.RankingAggregationMethod;
import dev.arcovia.mitigation.ranking.tests.MitigationModelTestBase;

public class SumRandomForestMitigationTest extends MitigationModelTestBase {

	protected RankerType getRankerType() {
		return RankerType.RANDOM_FOREST;
	}

	protected RankingAggregationMethod getAggregationMethod() {
		return RankingAggregationMethod.SUM;
	}
}
