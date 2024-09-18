package dev.abunai.confidentiality.mitigation.tests.sum;

import dev.abunai.confidentiality.mitigation.ranking.RankerType;
import dev.abunai.confidentiality.mitigation.ranking.RankingAggregationMethod;
import dev.abunai.confidentiality.mitigation.tests.MitigationModelTestBase;

public class SumLinearRegressionMitigationTest extends MitigationModelTestBase {

	protected RankerType getRankerType() {
		return RankerType.LINEAR_REGRESSION;
	}

	protected RankingAggregationMethod getAggregationMethod() {
		return RankingAggregationMethod.LINEAR_RANKS;
	}
}