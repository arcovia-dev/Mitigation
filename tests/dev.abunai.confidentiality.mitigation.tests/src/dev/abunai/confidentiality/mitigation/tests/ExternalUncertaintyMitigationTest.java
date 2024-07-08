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
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainFlowGraphCollection;
import dev.abunai.confidentiality.mitigation.MitigationModelCalculator;
import dev.abunai.confidentiality.mitigation.TrainDataGeneration;
import dev.abunai.confidentiality.mitigation.UncertaintyRanker;
import dev.abunai.confidentiality.mitigation.testBases.MitigationTestBase;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

@TestMethodOrder(OrderAnnotation.class)
public class ExternalUncertaintyMitigationTest extends MitigationTestBase {

	protected String getFolderName() {
		return "DFDExternalUncertainty";
	}

	protected String getFilesName() {
		return "default";
	}

	private final String scriptDirectory = "C:\\Users\\Jonas\\Desktop\\Masterarbeit_Paper\\Mitigation\\bundles\\dev.abunai.confidentiality.mitigation\\scripts\\uncertaintyRanking";
	private final String trainDataDirectory = scriptDirectory + "\\train_data_files";
	private final String pathToUncertaintyRankingScript = scriptDirectory + "\\uncertainty_ranking.py";
	private final String pathToRelevantUncertainties = "C:/Users/Jonas/Desktop/Masterarbeit_Paper/Mitigation/bundles/dev.abunai.confidentiality.mitigation/relevantUncertainties.txt";

	private List<Predicate<? super AbstractVertex<?>>> getConstraints(){
		List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();
		constraints.add(it -> {
			System.out.println(this.retrieveNodeLabels(it));
			System.out.println(this.retrieveDataLabels(it));
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
		
		// Generate train data for each constraint
		for (var constraint : constraints) {
			DFDUncertainFlowGraphCollection flowGraphs = (DFDUncertainFlowGraphCollection) analysis.findFlowGraph();
			DFDUncertainFlowGraphCollection uncertainFlowGraphs = flowGraphs.createUncertainFlows();
			uncertainFlowGraphs.evaluate();

			List<UncertainConstraintViolation> violations = analysis.queryUncertainDataFlow(uncertainFlowGraphs,
					constraint);
			TrainDataGeneration.violationDataToCSV(violations, uncertaintySources,
					trainDataDirectory + "\\violations_" + Integer.toString(count) + ".csv");
			count++;
		}
		
		// Rank the uncertainties specified in the given model and store the result in the specified file
		var relevantUncertaintyIds = UncertaintyRanker.rankUncertaintiesBasedOnTrainData(pathToUncertaintyRankingScript,
				trainDataDirectory, 1);
		Path filePath = Paths.get(pathToRelevantUncertainties);
		var content = String.join(System.lineSeparator(), relevantUncertaintyIds);
		try {
			Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	@Order(2)
	public void mitigateAutomatically() {
		
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
		var pathToModelsUncertainty = pathToDfdTestModels + "/DFDExternalUncertainty/default.uncertainty";
		var pathToMitigationModel = "C:\\Users\\Jonas\\Desktop\\Masterarbeit_Paper\\UncertaintyAwareConfidentialityAnalysis\\tests\\dev.abunai.confidentiality.analysis.testmodels\\models\\dfd\\mitigation";
		var pathToMitigationModelUncertainty = pathToDfdTestModels + "/mitigation/mitigation.uncertainty";

		// Execute mitigation
		var result = MitigationModelCalculator.findMitigatingModel(new DataFlowDiagramAndDictionary(this.dfd, this.dd),
				uncertaintySources, relevantUncertainties, constraints, pathToModelsUncertainty, pathToMitigationModel,
				pathFromTestModelsToMitigationFolder, pathToMitigationModelUncertainty);
		System.out.println(result);
	}
	
}
