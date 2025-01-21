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

public class OnlineBanking9MitigationTest extends MitigationTestBase {

	protected String getFolderName() {
		return "OBM";
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

    @Test
    public void executeHalf() {
        deleteOldMeassurement();
        for (int i = 0; i < MITIGATION_RUNS; i++) {
            var startTime = System.currentTimeMillis();
            mitigationStrategy = MitigationStrategy.HALF;
            createTrainData();
            createMitigationCandidatesAutomatically();
            var duration = System.currentTimeMillis() - startTime;
            storeMeassurement(duration);
        }
        storeMeassurementResult(seeAverageRuntime(),"OBM9_Half");
    }
    
    @Test
    public void executeQuarter() {
        deleteOldMeassurement();
        for (int i = 0; i < MITIGATION_RUNS; i++) {
            var startTime = System.currentTimeMillis();
            mitigationStrategy = MitigationStrategy.QUATER;
            createTrainData();
            createMitigationCandidatesAutomatically();
            var duration = System.currentTimeMillis() - startTime;
            storeMeassurement(duration);
        }
        storeMeassurementResult(seeAverageRuntime(),"OBM9_Quarter");
    }
    
    @Test
    public void executeIncreasing() {
        deleteOldMeassurement();
        for (int i = 0; i < MITIGATION_RUNS; i++) {
            var startTime = System.currentTimeMillis();
            mitigationStrategy = MitigationStrategy.INCREASING;
            createTrainData();
            createMitigationCandidatesAutomatically();
            var duration = System.currentTimeMillis() - startTime;
            storeMeassurement(duration);
        }
        storeMeassurementResult(seeAverageRuntime(),"OBM9_Increasing");
    }
    
    @Test
    public void executeCluster() {
        deleteOldMeassurement();
        for (int i = 0; i < MITIGATION_RUNS; i++) {
            var startTime = System.currentTimeMillis();
            mitigationStrategy = MitigationStrategy.BATCH_SIZE_OPTIMAL;
            createTrainData();
            createMitigationCandidatesAutomatically();
            var duration = System.currentTimeMillis() - startTime;
            storeMeassurement(duration);
        }
        storeMeassurementResult(seeAverageRuntime(),"OBM9_Cluster");
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
        storeMeassurementResult(seeAverageRuntime(),"OBM9_Brute_Force");
    }
}
