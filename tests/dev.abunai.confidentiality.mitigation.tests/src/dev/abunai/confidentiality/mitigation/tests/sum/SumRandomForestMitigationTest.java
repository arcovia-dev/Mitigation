package dev.abunai.confidentiality.mitigation.tests.sum;

import dev.abunai.confidentiality.mitigation.tests.MitigationModelTestBase;
import dev.abunai.confidentiality.mitigation.ranking.RankerType;
import dev.abunai.confidentiality.mitigation.ranking.RankingAggregationMethod;

public class SumRandomForestMitigationTest extends MitigationModelTestBase {

	protected RankerType getRankerType() {
		return RankerType.RANDOM_FOREST;
	}

	protected RankingAggregationMethod getAggregationMethod() {
		return RankingAggregationMethod.LINEAR_RANKS;
	}
}
