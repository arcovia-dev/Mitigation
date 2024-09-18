package dev.abunai.confidentiality.mitigation.tests.exponential;

import dev.abunai.confidentiality.mitigation.tests.MitigationModelTestBase;
import dev.abunai.confidentiality.mitigation.ranking.RankerType;
import dev.abunai.confidentiality.mitigation.ranking.RankingAggregationMethod;

public class ExponentialRandomForestMitigationTest extends MitigationModelTestBase {

	protected RankerType getRankerType() {
		return RankerType.RANDOM_FOREST;
	}

	protected RankingAggregationMethod getAggregationMethod() {
		return RankingAggregationMethod.EXPONENTIAL_RANKS;
	}
}
