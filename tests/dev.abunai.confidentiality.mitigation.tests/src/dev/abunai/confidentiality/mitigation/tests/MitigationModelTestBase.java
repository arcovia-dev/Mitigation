package dev.abunai.confidentiality.mitigation.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.mitigation.ranking.RankerType;
import dev.abunai.confidentiality.mitigation.ranking.RankingAggregationMethod;

/*
 * Abstract class for executing each mitigation strategy
 * */
public abstract class MitigationModelTestBase extends MitigationTestBase {

	protected String getFolderName() {
		return "OBM";
	}

	protected String getFilesName() {
		return "OBM";
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
		List<Float> meassurements = new ArrayList<>();
		List<Float> train_data_meassurements = new ArrayList<>();
		deleteOldMeassurement();
		for (int i = 0; i < MITIGATION_RUNS; i++) {
			var startTime = System.currentTimeMillis();
			mitigationStrategy = MitigationStrategy.INCREASING;
			createTrainData();
			train_data_meassurements.add((float) (System.currentTimeMillis() - startTime));
			createMitigationCandidatesAutomatically();
			var duration = System.currentTimeMillis() - startTime;
			storeMeassurement(duration);
		}
		meassurements.add(seeAverageRuntime());
		for (int i = 0; i < MITIGATION_RUNS; i++) {
			var startTime = System.currentTimeMillis();
			mitigationStrategy = MitigationStrategy.QUATER;
			createTrainData();
			train_data_meassurements.add((float) (System.currentTimeMillis() - startTime));
			createMitigationCandidatesAutomatically();
			var duration = System.currentTimeMillis() - startTime;
			storeMeassurement(duration);
		}
		meassurements.add(seeAverageRuntime());

		for (int i = 0; i < MITIGATION_RUNS; i++) {
			var startTime = System.currentTimeMillis();
			mitigationStrategy = MitigationStrategy.HALF;
			createTrainData();
			train_data_meassurements.add((float) (System.currentTimeMillis() - startTime));
			createMitigationCandidatesAutomatically();
			var duration = System.currentTimeMillis() - startTime;
			storeMeassurement(duration);
		}
		meassurements.add(seeAverageRuntime());
		printMetricies();
		System.out.println(meassurements);
		storeMeassurementResults(meassurements, getRankerType().toString(), getAggregationMethod().toString());
		storeTrainingDataResults(train_data_meassurements, getRankerType().toString(),
				getAggregationMethod().toString());

	}
}
