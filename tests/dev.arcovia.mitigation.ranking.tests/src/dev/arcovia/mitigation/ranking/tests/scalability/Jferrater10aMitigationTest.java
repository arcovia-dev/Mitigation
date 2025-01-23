package dev.arcovia.mitigation.ranking.tests.scalability;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.ranking.MitigationStrategy;
import dev.arcovia.mitigation.ranking.RankerType;
import dev.arcovia.mitigation.ranking.RankingAggregationMethod;
import dev.arcovia.mitigation.ranking.tests.MitigationTestBase;

public class Jferrater10aMitigationTest extends MitigationTestBase{
	
	protected String getFolderName() {
		return "jferrater10a";
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

	@Disabled
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
        storeMeassurementResult(seeAverageRuntime(),"jf10a_Half");
    }
    
	@Disabled
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
        storeMeassurementResult(seeAverageRuntime(),"jf10a_Quarter");
    }
    
	@Disabled
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
        storeMeassurementResult(seeAverageRuntime(),"jf10a_Increasing");
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
        storeMeassurementResult(seeAverageRuntime(),"jf10a_Cluster");
    }
    
	@Disabled
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
        storeMeassurementResult(seeAverageRuntime(),"jf10a_Brute_Force");
    }
}
