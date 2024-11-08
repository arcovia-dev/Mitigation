package dev.abunai.confidentiality.mitigation.tests.upscaling;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.mitigation.tests.MitigationTestBase;
import dev.abunai.confidentiality.mitigation.ranking.MitigationStrategy;
import dev.abunai.confidentiality.mitigation.ranking.RankerType;
import dev.abunai.confidentiality.mitigation.ranking.RankingAggregationMethod;

public class OnlineBanking27MitigationTest extends MitigationTestBase {

	protected String getFolderName() {
		return "OBM_27";
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
	public void executeMitigation() {
		deleteOldMeassurement();
		for (int i = 0; i < MITIGATION_RUNS; i++) {
			var startTime = System.currentTimeMillis();
			mitigationStrategy = MitigationStrategy.BATCH_SIZE_OPTIMAL;
			createTrainData();
			createMitigationCandidatesAutomatically();
			var duration = System.currentTimeMillis() - startTime;
			storeMeassurement(duration);
		}
		printMetricies();
        storeMeassurementResult(seeAverageRuntime(),"OBM27_Best");
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
        storeMeassurementResult(seeAverageRuntime(),"OBM27_Brute_Force");
	}
}
