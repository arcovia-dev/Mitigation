package dev.arcovia.mitigation.ranking.tests.top3;

import dev.arcovia.mitigation.ranking.RankerType;
import dev.arcovia.mitigation.ranking.RankingAggregationMethod;
import dev.arcovia.mitigation.ranking.tests.MitigationModelTestBase;

public class Top3LinearDiscriminantAnalysisMitigationTest extends MitigationModelTestBase {

    protected RankerType getRankerType() {
        return RankerType.LDA;
    }

    protected RankingAggregationMethod getAggregationMethod() {
        return RankingAggregationMethod.TOP_3;
    }
}