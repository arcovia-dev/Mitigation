package dev.arcovia.mitigation.ranking.tests.top3;

import dev.arcovia.mitigation.ranking.RankerType;
import dev.arcovia.mitigation.ranking.RankingAggregationMethod;
import dev.arcovia.mitigation.ranking.tests.MitigationModelTestBase;

public class Top3LinearRegressionMitigationTest extends MitigationModelTestBase {

	protected RankerType getRankerType() {
		return RankerType.LINEAR_REGRESSION;
	}

	protected RankingAggregationMethod getAggregationMethod() {
		return RankingAggregationMethod.TOP_3;
	}
}