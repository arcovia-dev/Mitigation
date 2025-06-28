package dev.arcovia.mitigation.ranking.tests.exponential;

import dev.arcovia.mitigation.ranking.RankerType;
import dev.arcovia.mitigation.ranking.RankingAggregationMethod;
import dev.arcovia.mitigation.ranking.tests.MitigationModelTestBase;

public class ExponentialRandomForestMitigationTest extends MitigationModelTestBase {

    protected RankerType getRankerType() {
        return RankerType.RANDOM_FOREST;
    }

    protected RankingAggregationMethod getAggregationMethod() {
        return RankingAggregationMethod.EXPONENTIAL_RANKS;
    }
}
