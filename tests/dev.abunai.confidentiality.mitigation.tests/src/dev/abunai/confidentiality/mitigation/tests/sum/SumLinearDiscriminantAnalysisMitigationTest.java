package dev.abunai.confidentiality.mitigation.tests.sum;

import dev.abunai.confidentiality.mitigation.ranking.RankerType;
import dev.abunai.confidentiality.mitigation.ranking.RankingAggregationMethod;
import dev.abunai.confidentiality.mitigation.tests.MitigationModelTestBase;

public class SumLinearDiscriminantAnalysisMitigationTest extends MitigationModelTestBase {

	
	protected RankerType getRankerType() {
		return RankerType.LDA;
	}

	protected RankingAggregationMethod getAggregationMethod() {
		return RankingAggregationMethod.LINEAR_RANKS;
	}
}