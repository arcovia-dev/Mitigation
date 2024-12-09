package dev.arcovia.mitigation.ranking.tests.sum;

import dev.arcovia.mitigation.ranking.RankerType;
import dev.arcovia.mitigation.ranking.RankingAggregationMethod;
import dev.arcovia.mitigation.ranking.tests.MitigationModelTestBase;

public class SumLogisticRegressionMitigationTest extends MitigationModelTestBase {
	
	protected RankerType getRankerType() {
		return RankerType.LOGISTIC_REGRESSION;
	}

	protected RankingAggregationMethod getAggregationMethod() {
		return RankingAggregationMethod.SUM;
	}
}