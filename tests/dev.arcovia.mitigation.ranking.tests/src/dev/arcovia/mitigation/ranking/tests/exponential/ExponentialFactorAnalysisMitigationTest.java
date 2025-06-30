package dev.arcovia.mitigation.ranking.tests.exponential;

import dev.arcovia.mitigation.ranking.RankerType;
import dev.arcovia.mitigation.ranking.RankingAggregationMethod;
import dev.arcovia.mitigation.ranking.tests.MitigationModelTestBase;

public class ExponentialFactorAnalysisMitigationTest extends MitigationModelTestBase {

    protected RankerType getRankerType() {
        return RankerType.FAMD;
    }

    protected RankingAggregationMethod getAggregationMethod() {
        return RankingAggregationMethod.EXPONENTIAL_RANKS;
    }
}