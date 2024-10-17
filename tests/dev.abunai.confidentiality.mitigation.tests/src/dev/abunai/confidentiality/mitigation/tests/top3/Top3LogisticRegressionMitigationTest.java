package dev.abunai.confidentiality.mitigation.tests.top3;

import dev.abunai.confidentiality.mitigation.ranking.RankerType;
import dev.abunai.confidentiality.mitigation.ranking.RankingAggregationMethod;
import dev.abunai.confidentiality.mitigation.tests.MitigationModelTestBase;

public class Top3LogisticRegressionMitigationTest extends MitigationModelTestBase {
	
	protected RankerType getRankerType() {
		return RankerType.LOGISTIC_REGRESSION;
	}

	protected RankingAggregationMethod getAggregationMethod() {
		return RankingAggregationMethod.TOP_3;
	}
}