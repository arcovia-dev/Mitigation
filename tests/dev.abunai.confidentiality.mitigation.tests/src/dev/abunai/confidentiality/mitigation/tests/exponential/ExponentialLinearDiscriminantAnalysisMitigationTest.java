package dev.abunai.confidentiality.mitigation.tests.exponential;

import dev.abunai.confidentiality.mitigation.ranking.RankerType;
import dev.abunai.confidentiality.mitigation.ranking.RankingAggregationMethod;
import dev.abunai.confidentiality.mitigation.tests.MitigationModelTestBase;

public class ExponentialLinearDiscriminantAnalysisMitigationTest extends MitigationModelTestBase {

	protected RankerType getRankerType() {
		return RankerType.LDA;
	}

	protected RankingAggregationMethod getAggregationMethod() {
		return RankingAggregationMethod.EXPONENTIAL_RANKS;
	}
}