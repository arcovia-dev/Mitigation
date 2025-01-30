package dev.arcovia.mitigation.ranking.tests.scalability;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.ranking.MitigationStrategy;
import dev.arcovia.mitigation.ranking.RankerType;
import dev.arcovia.mitigation.ranking.RankingAggregationMethod;
import dev.arcovia.mitigation.ranking.tests.MitigationTestBase;

public class Jferrater7aMitigationTest extends MitigationTestBase{
	
	protected String getFolderName() {
		return "jferrater7a";
	}

	protected String getFilesName() {
		return "jferrater";
	}
	
	protected List<Predicate<? super AbstractVertex<?>>> getConstraints() {
		List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();
		constraints.add(it -> {
			return this.retrieveNodeLabels(it).contains("internal")
					&& !this.retrieveAllDataLabels(it).contains("authenticated_request");
		});
		constraints.add(it -> {
			return this.retrieveNodeLabels(it).contains("authorization_server")
					&& !this.retrieveNodeLabels(it).contains("login_attempts_regulation");
		});
		constraints.add(it -> {
			return this.retrieveDataLabels(it).contains("entrypoint")
					&& !this.retrieveAllDataLabels(it).contains("encrypted_connection");
		});
		/*constraints.add(it -> {
			return this.retrieveNodeLabels(it).contains("internal")
					&& !this.retrieveAllDataLabels(it).contains("encrypted_connection");
		});*/
		constraints.add(it -> {
			 return this.retrieveNodeLabels(it).contains("internal") &&
					!this.retrieveNodeLabels(it).contains("local_logging");
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
    
	private void executeMitigationStrategy(MitigationStrategy strategy) {
        deleteOldMeassurement();
        for (int i = 0; i < MITIGATION_RUNS; i++) {
            var startTime = System.currentTimeMillis();
            mitigationStrategy = strategy;

            if (!strategy.equals(MitigationStrategy.BRUTE_FORCE)) {
                createTrainData();
            }

            createMitigationCandidatesAutomatically();
            var duration = System.currentTimeMillis() - startTime;
            storeMeassurement(duration);
        }
        storeMeassurementResult(seeAverageRuntime(), getFolderName() + "_" + strategy.name());
    }

    @Test
    public void executeHalf() {
        executeMitigationStrategy(MitigationStrategy.HALF);
    }

    @Test
    public void executeQuarter() {
        executeMitigationStrategy(MitigationStrategy.QUATER);
    }

    @Test
    public void executeIncreasing() {
        executeMitigationStrategy(MitigationStrategy.INCREASING);
    }

    @Test
    public void executeCluster() {
        executeMitigationStrategy(MitigationStrategy.CLUSTER);
    }

    @Test
    public void executeFastStart() {
        executeMitigationStrategy(MitigationStrategy.FAST_START);
    }

    @Test
    public void executeBruteForce() {
        executeMitigationStrategy(MitigationStrategy.BRUTE_FORCE);
    }
}
