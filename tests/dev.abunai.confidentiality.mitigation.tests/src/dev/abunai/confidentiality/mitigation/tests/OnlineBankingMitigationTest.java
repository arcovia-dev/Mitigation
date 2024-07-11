package dev.abunai.confidentiality.mitigation.tests;

import java.nio.file.Paths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.dfd.core.DFDCharacteristicValue;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainFlowGraphCollection;
import dev.abunai.confidentiality.mitigation.MitigationModelCalculator;
import dev.abunai.confidentiality.mitigation.TrainDataGeneration;
import dev.abunai.confidentiality.mitigation.TrainDataGenerationBinary;
import dev.abunai.confidentiality.mitigation.TrainDataGenerationMinimal;
import dev.abunai.confidentiality.mitigation.UncertaintyRanker;
import dev.abunai.confidentiality.mitigation.testBases.MitigationTestBase;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

@TestMethodOrder(OrderAnnotation.class)
public class OnlineBankingMitigationTest extends MitigationTestBase {

	protected String getFolderName() {
		return "OnlineBankingModel";
	}

	protected String getFilesName() {
		return "online_banking_model";
	}

	private final int uncertaintysToModify = 4;


	private List<Predicate<? super AbstractVertex<?>>> getConstraints() {
		List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();
		constraints.add(it -> {
			System.out.println(this.retrieveNodeLabels(it));
			System.out.println(this.retrieveDataLabels(it));
			boolean res =  this.retrieveNodeLabels(it).contains("nonEU")
					&& this.retrieveDataLabels(it).contains("Personal");
			if(res) {
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

		// Generate train data for each constraint
		for (var constraint : constraints) {
			DFDUncertainFlowGraphCollection flowGraphs = (DFDUncertainFlowGraphCollection) analysis.findFlowGraph();
			DFDUncertainFlowGraphCollection uncertainFlowGraphs = flowGraphs.createUncertainFlows();
			uncertainFlowGraphs.evaluate();

			List<UncertainConstraintViolation> violations = analysis.queryUncertainDataFlow(uncertainFlowGraphs,
					constraint);
			TrainDataGenerationMinimal.violationDataToCSV(violations, uncertaintySources,
					trainDataDirectory + "\\violations_" + Integer.toString(count) + ".csv");
			count++;
		}

		// Rank the uncertainties specified in the given model and store the result in
		// the specified file
		var relevantUncertaintyIds = UncertaintyRanker.rankUncertaintiesBasedOnTrainData(pathToUncertaintyRankingScript,
				trainDataDirectory, uncertaintysToModify);
		Path filePath = Paths.get(pathToRelevantUncertainties);
		var content = String.join("\n", relevantUncertaintyIds);
		System.out.println(content);
		try {
			Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	@Order(2)
	public void createMitigationCandidatesAutomatically() {

		// Load uncertainties that should be modified for the mitigation
		Path filePath = Paths.get(pathToRelevantUncertainties);
		final List<String> lines;
		var relevantUncertainties = uncertaintySources;
		try {
			lines = Files.readAllLines(filePath);
			relevantUncertainties = uncertaintySources.stream().filter(u -> lines.contains(u.getEntityName())).toList();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Specify data that is needed for mitigation
		List<Predicate<? super AbstractVertex<?>>> constraints = getConstraints();
		var pathToDfdTestModels = "platform:/plugin/dev.abunai.confidentiality.analysis.testmodels/models/dfd";
		var pathFromTestModelsToMitigationFolder = "models/dfd/mitigation";
		var pathToModelsUncertainty = pathToDfdTestModels
				+ String.format("/%s/%s.uncertainty", getFolderName(), getFilesName());
		var pathToMitigationModel = "C:\\Users\\Jonas\\Desktop\\Masterarbeit_Paper\\UncertaintyAwareConfidentialityAnalysis\\tests\\dev.abunai.confidentiality.analysis.testmodels\\models\\dfd\\mitigation";
		var pathToMitigationModelUncertainty = pathToDfdTestModels + "/mitigation/mitigation.uncertainty";

		// Execute mitigation
		var result = MitigationModelCalculator.findMitigatingModel(new DataFlowDiagramAndDictionary(this.dfd, this.dd),
				uncertaintySources, relevantUncertainties, constraints, pathToModelsUncertainty, pathToMitigationModel,
				pathFromTestModelsToMitigationFolder, pathToMitigationModelUncertainty);
		System.out.println(result);
	}

}
