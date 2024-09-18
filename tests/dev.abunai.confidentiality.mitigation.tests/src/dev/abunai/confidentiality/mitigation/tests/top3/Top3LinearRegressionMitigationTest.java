package dev.abunai.confidentiality.mitigation.tests.top3;

import dev.abunai.confidentiality.mitigation.ranking.RankerType;
import dev.abunai.confidentiality.mitigation.ranking.RankingAggregationMethod;
import dev.abunai.confidentiality.mitigation.tests.MitigationModelTestBase;

public class Top3LinearRegressionMitigationTest extends MitigationModelTestBase {

	protected RankerType getRankerType() {
		return RankerType.LINEAR_REGRESSION;
	}

	protected RankingAggregationMethod getAggregationMethod() {
		return RankingAggregationMethod.TOP_3;
	}
}