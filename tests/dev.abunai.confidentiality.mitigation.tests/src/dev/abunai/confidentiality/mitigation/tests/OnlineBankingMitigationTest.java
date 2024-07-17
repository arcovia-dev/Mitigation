package dev.abunai.confidentiality.mitigation.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainFlowGraphCollection;
import dev.abunai.confidentiality.mitigation.UncertaintyRanker;
import dev.abunai.confidentiality.mitigation.testBases.MitigationTestBase;

public class OnlineBankingMitigationTest extends MitigationTestBase {

	protected String getFolderName() {
		return "DFDInterfaceUncertaintyMitigation";
	}

	protected String getFilesName() {
		return "int";
	}

	protected List<Predicate<? super AbstractVertex<?>>> getConstraints() {
		List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();
		constraints.add(it -> {
			return this.retrieveNodeLabels(it).contains("Develop")
					&& this.retrieveDataLabels(it).contains("Personal");
		});
		constraints.add(it -> {
			return this.retrieveNodeLabels(it).contains("nonEU") && this.retrieveDataLabels(it).contains("Personal");
		});
		return constraints;
	}

	@Test
	@Order(1)
	public void createTrainData() {
		// Get constraints and define count variable for constraint file differentiation
		List<Predicate<? super AbstractVertex<?>>> constraints = getConstraints();
		var count = 0;
		DFDUncertainFlowGraphCollection flowGraphs = (DFDUncertainFlowGraphCollection) analysis.findFlowGraph();
		DFDUncertainFlowGraphCollection uncertainFlowGraphs = flowGraphs.createUncertainFlows();
		uncertainFlowGraphs.evaluate();
		// Generate train data for each constraint
		for (var constraint : constraints) {
			List<UncertainConstraintViolation> violations = analysis.queryUncertainDataFlow(uncertainFlowGraphs,
					constraint);

			// If no violation occured no training data needs to be created
			if (violations.size() == 0) {
				continue;
			}

			trainDataGeneration.violationDataToCSV(violations, uncertaintySources,
					trainDataDirectory + "\\violations_" + Integer.toString(count) + ".csv");
			count++;
		}

		// Rank the uncertainties specified in the given model and store the result in
		// the specified file
		var relevantUncertaintyIds = UncertaintyRanker.rankUncertaintiesBasedOnTrainData(pathToUncertaintyRankingScript,
				trainDataDirectory, uncertaintySources.size());

		// Store the result of the Ranking in a file
		storeRankingResult(relevantUncertaintyIds);
	}

	@Test
	@Order(2)
	public void createMitigationCandidatesAutomatically() {
		var rankedUncertaintyEntityName = loadRanking();
		var success = mitigateWithIncreasingAmountOfUncertainties(rankedUncertaintyEntityName);
		if (!success) {
			System.out.println("mitigation failed");
		}
	}

	// @Test
	// @Order(2)
	public void createMitigationCandidatesAutomatically2() {
		var rankedUncertaintyEntityName = loadRanking();
		var success = mitigateWithFixAmountOfUncertainties(rankedUncertaintyEntityName,
				rankedUncertaintyEntityName.size() / 2);
		if (!success) {
			var success2 = mitigateWithFixAmountOfUncertainties(rankedUncertaintyEntityName,
					rankedUncertaintyEntityName.size());
			if (!success2) {
				System.out.println("mitigation failed");
			}
		}
	}

	// @Test
	// @Order(2)
	public void createMitigationCandidatesAutomatically3() {
		var rankedUncertaintyEntityName = loadRanking();
		boolean success = false;
		for (int i = 1; i <= 4 && !success; i++) {
			success = mitigateWithFixAmountOfUncertainties(rankedUncertaintyEntityName,
					i * (rankedUncertaintyEntityName.size() / 4));
		}
		if (!success) {
			System.out.println("mitigation failed");
		}
	}


}
