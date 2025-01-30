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

public class OnlineBanking14MitigationTest extends MitigationTestBase {

	protected String getFolderName() {
		return "OBM_14";
	}

	protected String getFilesName() {
		return "OBM";
	}
		
	@Override
	protected RankerType getRankerType() {
		return RankerType.LOGISTIC_REGRESSION;
	}

	@Override
	protected RankingAggregationMethod getAggregationMethod() {
		return RankingAggregationMethod.EXPONENTIAL_RANKS;
	}

	protected List<Predicate<? super AbstractVertex<?>>> getConstraints() {
		List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();
		constraints.add(it -> {
			boolean vio = this.retrieveNodeLabels(it).contains("Develop")
					&& this.retrieveDataLabels(it).contains("Personal");
			return vio;
		});
		constraints.add(it -> {
			boolean vio = this.retrieveNodeLabels(it).contains("Processable")
					&& this.retrieveDataLabels(it).contains("Encrypted");
			return vio;
		});
		constraints.add(it -> {
			boolean vio = this.retrieveNodeLabels(it).contains("nonEU")
					&& this.retrieveDataLabels(it).contains("Personal");
			return vio;
		});
		return constraints;
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
