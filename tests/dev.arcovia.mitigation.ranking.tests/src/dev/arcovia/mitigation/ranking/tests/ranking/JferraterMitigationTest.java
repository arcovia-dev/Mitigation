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

public class JferraterMitigationTest extends MitigationTestBase {

    protected String getFolderName() {
        return "jferrater";
    }

    protected String getFilesName() {
        return "jferrater";
    }

    protected List<Predicate<? super AbstractVertex<?>>> getConstraints() {
        List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();
        constraints.add(it -> {
            return this.retrieveNodeLabels(it)
                    .contains("internal")
                    && !this.retrieveAllDataLabels(it)
                            .contains("authenticated_request");
        });
        constraints.add(it -> {
            return this.retrieveNodeLabels(it)
                    .contains("authorization_server")
                    && !this.retrieveNodeLabels(it)
                            .contains("login_attempts_regulation");
        });
        constraints.add(it -> {
            return this.retrieveDataLabels(it)
                    .contains("entrypoint")
                    && !this.retrieveAllDataLabels(it)
                            .contains("encrypted_connection");
        });
        /*
         * constraints.add(it -> { return this.retrieveNodeLabels(it).contains("internal") &&
         * !this.retrieveAllDataLabels(it).contains("encrypted_connection"); });
         */
        constraints.add(it -> {
            return this.retrieveNodeLabels(it)
                    .contains("internal")
                    && !this.retrieveNodeLabels(it)
                            .contains("local_logging");
        });
        return constraints;
    }

    @Override
    protected RankerType getRankerType() {
        return RankerType.LOGISTIC_REGRESSION;
    }

    @Override
    protected RankingAggregationMethod getAggregationMethod() {
        return RankingAggregationMethod.EXPONENTIAL_RANKS;
    }

    @Test
    public void executeMitigation() {
        deleteOldMeassurement();
        for (int i = 0; i < MITIGATION_RUNS; i++) {
            var startTime = System.currentTimeMillis();
            mitigationStrategy = MitigationStrategy.HALF;
            createUncertaintyRanking();
            printMetricies();
            createMitigationCandidatesAutomatically();
            var duration = System.currentTimeMillis() - startTime;
            storeMeassurement(duration);
        }
        printMetricies();
        seeAverageRuntime();
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
        seeAverageRuntime();
    }
}
