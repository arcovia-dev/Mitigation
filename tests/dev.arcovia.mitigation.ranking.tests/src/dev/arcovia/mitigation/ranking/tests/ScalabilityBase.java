package dev.arcovia.mitigation.ranking.tests;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.ranking.MitigationStrategy;
import dev.arcovia.mitigation.ranking.RankerType;
import dev.arcovia.mitigation.ranking.RankingAggregationMethod;

public abstract class ScalabilityBase extends MitigationTestBase {

    @Override
    protected RankerType getRankerType() {
        return RankerType.LOGISTIC_REGRESSION;
    }

    @Override
    protected RankingAggregationMethod getAggregationMethod() {
        return RankingAggregationMethod.EXPONENTIAL_RANKS;
    }

    private void executeMitigationStrategy(MitigationStrategy strategy) {
        deleteOldMeassurement();
        for (int i = 0; i < MITIGATION_RUNS; i++) {
            var startTime = System.currentTimeMillis();
            mitigationStrategy = strategy;

            if (!strategy.equals(MitigationStrategy.BRUTE_FORCE)) {
                createUncertaintyRanking();
            }

            createMitigationCandidatesAutomatically();
            var duration = System.currentTimeMillis() - startTime;
            storeMeassurement(duration);
        }
        storeMeassurementResult(seeAverageRuntime(), getFolderName() + "_" + strategy.name());
    }

    @Test
    @Disabled
    public void executeHalf() {
        executeMitigationStrategy(MitigationStrategy.HALF);
    }

    @Test
    @Disabled
    public void executeQuarter() {
        executeMitigationStrategy(MitigationStrategy.QUATER);
    }

    @Test
    @Disabled
    public void executeIncreasing() {
        executeMitigationStrategy(MitigationStrategy.INCREASING);
    }

    @Test
    @Disabled
    public void executeCluster() {
        executeMitigationStrategy(MitigationStrategy.CLUSTER);
    }

    @Test
    @Disabled
    public void executeFastStart() {
        executeMitigationStrategy(MitigationStrategy.FAST_START);
    }

    @Test
    @Disabled
    public void executeBruteForce() {
        executeMitigationStrategy(MitigationStrategy.BRUTE_FORCE);
    }
}
