package dev.abunai.confidentiality.mitigation.tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainFlowGraphCollection;
import dev.abunai.confidentiality.mitigation.MitigationModelCalculator;
import dev.abunai.confidentiality.mitigation.UncertaintyRanker;
import dev.abunai.confidentiality.mitigation.testBases.MitigationTestBase;
import dev.abunai.confidentiality.mitigation.trainDataGeneration.TrainDataGenerationMinimal;
import dev.abunai.confidentiality.mitigation.trainDataGeneration.ITrainDataGeneration;

public class OnlineBankingMitigationTest extends MitigationTestBase {

	protected String getFolderName() {
		return "OnlineBankingModel";
	}

	protected String getFilesName() {
		return "online_banking_model";
	}

	private final ITrainDataGeneration trainDataGeneration = new TrainDataGenerationMinimal();
	private final String pathToDfdTestModels = "platform:/plugin/dev.abunai.confidentiality.analysis.testmodels/models/dfd";
	private final String pathFromTestModelsToMitigationFolder = "models/dfd/mitigation";
	private final String pathToModelsUncertainty = pathToDfdTestModels
			+ String.format("/%s/%s.uncertainty", getFolderName(), getFilesName());
	private final String pathToMitigationModel = "C:\\Users\\Jonas\\Desktop\\Masterarbeit_Paper\\UncertaintyAwareConfidentialityAnalysis\\tests\\dev.abunai.confidentiality.analysis.testmodels\\models\\dfd\\mitigation";
	private final String pathToMitigationModelUncertainty = pathToDfdTestModels + "/mitigation/mitigation.uncertainty";

	private List<Predicate<? super AbstractVertex<?>>> getConstraints() {
		List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();
		constraints.add(it -> {
			System.out.println(this.retrieveNodeLabels(it));
			System.out.println(this.retrieveDataLabels(it));
			boolean res = this.retrieveNodeLabels(it).contains("Processable") &&
					 this.retrieveDataLabels(it).contains("Encrypted");
			if (res) {
				System.out.println("violation occured here:");
				System.out.println(it.toString());
			}
			return res;
		});
		constraints.add(it -> {
			System.out.println(this.retrieveNodeLabels(it));
			System.out.println(this.retrieveDataLabels(it));
			boolean res = this.retrieveNodeLabels(it).contains("nonEU") &&
					 this.retrieveDataLabels(it).contains("Personal");
			if (res) {
				System.out.println("violation occured here:");
				System.out.println(it.toString());
			}
			return res;
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
		if(!success) {
			System.out.println("mitigation failed");
		}
	}

	private boolean mitigateWithIncreasingAmountOfUncertainties(List<String> rankedUncertaintyEntityName) {
		// Increase amount of uncertainties used if the current amount is not enough
		for (int i = 1; i <= rankedUncertaintyEntityName.size(); i++) {
			
			// Extract relevant uncertainties
			var relevantUncertaintyEntityName = rankedUncertaintyEntityName.stream().limit(i).toList();
			var relevantUncertainties = uncertaintySources.stream()
					.filter(u -> relevantUncertaintyEntityName.contains(u.getEntityName())).toList();
			
			// Run mitigation with i+1 uncertainties
			var result = MitigationModelCalculator.findMitigatingModel(
					new DataFlowDiagramAndDictionary(this.dfd, this.dd), uncertaintySources, relevantUncertainties,
					getConstraints(), pathToModelsUncertainty, pathToMitigationModel, pathFromTestModelsToMitigationFolder,
					pathToMitigationModelUncertainty);
			
			// Print working mitigation if one was found
			if (result.size() > 0) {
				System.out.println(result);
				System.out.println(i);
				return true;
			}
		}
		// Return false if no mitigation was found
		return false;
	}

	private boolean mitigateWithFixAmountOfUncertainties(List<String> rankedUncertaintyEntityName, int n) {
		
		// Extract relevant uncertainties
		var relevantEntityNames = rankedUncertaintyEntityName.stream().limit(n).toList();
		var relevantUncertainties = uncertaintySources.stream().filter(u -> relevantEntityNames.contains(u.getEntityName())).toList();
		
		// Execute mitigation
		var result = MitigationModelCalculator.findMitigatingModel(new DataFlowDiagramAndDictionary(this.dfd, this.dd),
				uncertaintySources, relevantUncertainties, getConstraints(), pathToModelsUncertainty,
				pathToMitigationModel, pathFromTestModelsToMitigationFolder, pathToMitigationModelUncertainty);
		
		// Return success of mitgation
		if (result.size() > 0) {
			System.out.println(result);
			return true;
		}
		return false;
	}

	private void storeRankingResult(List<String> relevantUncertaintyIds) {
		Path filePath = Paths.get(pathToRelevantUncertainties);
		var content = String.join("\n", relevantUncertaintyIds);
		try {
			Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private List<String> loadRanking() {
		Path filePath = Paths.get(pathToRelevantUncertainties);
		try {
			return Files.readAllLines(filePath);
		} catch (IOException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

}
