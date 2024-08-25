package dev.abunai.confidentiality.mitigation.tests.ranking;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;


import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainFlowGraphCollection;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainTransposeFlowGraph;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyResourceProvider;
import dev.abunai.confidentiality.mitigation.ranking.MitigationModel;
import dev.abunai.confidentiality.mitigation.ranking.UncertaintyRanker;
import dev.abunai.confidentiality.mitigation.tests.MitigationStrategy;
import dev.abunai.confidentiality.mitigation.tests.MitigationTestBase;

public class BehaviorUncertaintyMitigationTest extends MitigationTestBase {

	protected String getFolderName() {
		return "DFDBehaviorUncertaintyMitigation";
	}

	protected String getFilesName() {
		return "beh";
	}

	protected List<Predicate<? super AbstractVertex<?>>> getConstraints() {
		List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();

		constraints.add(it -> {
			boolean vio = this.retrieveNodeLabels(it).contains("Develop")
					&& this.retrieveDataLabels(it).contains("Personal");
			if (vio) {
				System.out.println("develop");
			}
			return vio;
		});
		constraints.add(it -> {
			boolean vio =  this.retrieveNodeLabels(it).contains("nonEU") && this.retrieveDataLabels(it).contains("Personal");
			System.out.println(it);
			System.out.println(this.retrieveDataLabels(it));
			System.out.println(this.retrieveNodeLabels(it));
			return vio;
		});
		return constraints;
	}

	@Test
	public void executeMitigation() {
		// For meassuring at least 30 runs are required
		deleteOldMeassurement();
		for (int i = 0; i < MITIGATION_RUNS; i++) {
			var startTime = System.currentTimeMillis();
			createTrainData();
			createMitigationCandidatesAutomatically();
			var duration = System.currentTimeMillis() - startTime;
			storeMeassurement(duration);
		}
	}
	
	@Test
	public void executeBruteForce() throws Exception {
		// For meassuring at least 30 runs are required
		deleteOldMeassurement();
		for (int i = 0; i < MITIGATION_RUNS; i++) {
			var startTime = System.currentTimeMillis();
			if (!mitigationStrategy.equals(MitigationStrategy.BRUTE_FORCE)) {
				throw new Exception();
			}
			createMitigationCandidatesAutomatically();
			var duration = System.currentTimeMillis() - startTime;
			storeMeassurement(duration);
		}
	}

	public void createTrainData() {
		var trainDir = new File(trainDataDirectory);
		for (File file : trainDir.listFiles()) {
			file.delete();
		}
		var analysis = getAnalysis();
		// Get constraints and define count variable for constraint file differentiation
		List<Predicate<? super AbstractVertex<?>>> constraints = getConstraints();
		var count = 0;
		DFDUncertainFlowGraphCollection flowGraphs = (DFDUncertainFlowGraphCollection) analysis.findFlowGraph();
		DFDUncertainFlowGraphCollection uncertainFlowGraphs = flowGraphs.createUncertainFlows();

		uncertainFlowGraphs.evaluate();

		List<DFDUncertainTransposeFlowGraph> allTFGs = uncertainFlowGraphs.getTransposeFlowGraphs().stream()
				.map(DFDUncertainTransposeFlowGraph.class::cast).toList();
		// Generate train data for each constraint
		for (var constraint : constraints) {
			List<UncertainConstraintViolation> violations = analysis.queryUncertainDataFlow(uncertainFlowGraphs,
					constraint);

			// If no violation occured no training data needs to be created
			if (violations.size() == 0) {
				continue;
			}

			trainDataGeneration.violationDataToCSV(violations, allTFGs, analysis.getUncertaintySources(),
					Paths.get(trainDataDirectory, "violations_" + Integer.toString(count) + ".csv").toString());
			count++;
		}

		// Rank the uncertainties specified in the given model and store the result in
		// the specified file
		var relevantUncertaintyIds = UncertaintyRanker.rankUncertaintiesBasedOnTrainData(pathToUncertaintyRankingScript,
				trainDataDirectory, analysis.getUncertaintySources().size());

		// Store the result of the Ranking in a file
		storeRankingResult(relevantUncertaintyIds);
	}

	public void createMitigationCandidatesAutomatically() {
		var analysis = getAnalysis();
		var rankedUncertaintyEntityName = mitigationStrategy.equals(MitigationStrategy.BRUTE_FORCE) ?
				analysis.getUncertaintySources().stream().map(u -> u.getEntityName()).toList() : loadRanking();
		var resourceProvider = (DFDUncertaintyResourceProvider) analysis.getResourceProvider();
		resourceProvider.loadRequiredResources();
		var dd = resourceProvider.getDataDictionary();
		var dfd = resourceProvider.getDataFlowDiagram();
		var ddAndDfd = new DataFlowDiagramAndDictionary(dfd, dd);
		List<MitigationModel> result = new ArrayList<>();
		if (mitigationStrategy.equals(MitigationStrategy.INCREASING)) {
			result = mitigateWithIncreasingAmountOfUncertainties(rankedUncertaintyEntityName, analysis, ddAndDfd);
		} else if (mitigationStrategy.equals(MitigationStrategy.QUATER)) {
			for (int i = 1; i <= 4; i++) {
				result = mitigateWithFixAmountOfUncertainties(rankedUncertaintyEntityName,
						i * rankedUncertaintyEntityName.size() / 4, analysis, ddAndDfd);
				if (result.size() != 0) {
					break;
				}
			}
		} else if (mitigationStrategy.equals(MitigationStrategy.HALF)){
			result = mitigateWithFixAmountOfUncertainties(rankedUncertaintyEntityName,
					rankedUncertaintyEntityName.size() / 2, analysis, ddAndDfd);
			if (result.size() == 0) {
				result = mitigateWithFixAmountOfUncertainties(rankedUncertaintyEntityName,
						rankedUncertaintyEntityName.size(), analysis, ddAndDfd);
			}
		}
		else {
			result = mitigateWithFixAmountOfUncertainties(rankedUncertaintyEntityName,
					rankedUncertaintyEntityName.size(), analysis, ddAndDfd);
		}
		if (result.size() == 0) {
			System.out.println("mitigation failed");
		}
	}
}
