package dev.arcovia.mitigation.ranking.tests.ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.ranking.MitigationStrategy;
import dev.arcovia.mitigation.ranking.RankerType;
import dev.arcovia.mitigation.ranking.RankingAggregationMethod;
import dev.arcovia.mitigation.ranking.tests.MitigationTestBase;

public class BehaviorUncertaintyMitigationTest extends MitigationTestBase {

    protected String getFolderName() {
        return "DFDBehaviorUncertaintyMitigation";
    }

    protected String getFilesName() {
        return "beh";
    }

    @Override
    protected RankerType getRankerType() {
        return RankerType.RANDOM_FOREST;
    }

    @Override
    protected RankingAggregationMethod getAggregationMethod() {
        return RankingAggregationMethod.EXPONENTIAL_RANKS;
    }

    protected List<Predicate<? super AbstractVertex<?>>> getConstraints() {
        List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();

        constraints.add(it -> {
            boolean vio = this.retrieveNodeLabels(it)
                    .contains("Develop")
                    && this.retrieveDataLabels(it)
                            .contains("Personal");
            if (vio) {
                System.out.println("develop");
            }
            return vio;
        });
        constraints.add(it -> {
            boolean vio = this.retrieveNodeLabels(it)
                    .contains("nonEU")
                    && this.retrieveDataLabels(it)
                            .contains("Personal");
            System.out.println(it);
            System.out.println(this.retrieveDataLabels(it));
            System.out.println(this.retrieveNodeLabels(it));
            return vio;
        });
        return constraints;
    }

    @Test
    public void executeMitigation() {
        deleteOldMeassurement();
        for (int i = 0; i < MITIGATION_RUNS; i++) {
            var startTime = System.currentTimeMillis();
            mitigationStrategy = MitigationStrategy.INCREASING;
            createUncertaintyRanking();
            createMitigationCandidatesAutomatically();
            var duration = System.currentTimeMillis() - startTime;
            storeMeassurement(duration);
        }
    }

    @Test
    public void executeBruteForce() throws Exception {
        deleteOldMeassurement();
        for (int i = 0; i < MITIGATION_RUNS; i++) {
            var startTime = System.currentTimeMillis();
            mitigationStrategy = MitigationStrategy.BRUTE_FORCE;
            createMitigationCandidatesAutomatically();
            var duration = System.currentTimeMillis() - startTime;
            storeMeassurement(duration);
        }
    }
}
