package dev.abunai.confidentiality.mitigation.tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.utils.ResourceUtils;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.BeforeEach;

import dev.abunai.confidentiality.analysis.UncertaintyAwareConfidentialityAnalysis;
import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyAwareConfidentialityAnalysisBuilder;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyResourceProvider;
import dev.abunai.confidentiality.mitigation.ranking.MitigationListSimplifier;
import dev.abunai.confidentiality.mitigation.ranking.MitigationModel;
import dev.abunai.confidentiality.mitigation.ranking.MitigationModelCalculator;
import dev.abunai.confidentiality.mitigation.ranking.TrainDataGeneration;
import dev.abunai.confidentiality.mitigation.ranking.UncertaintySubset;
import dev.abunai.confidentiality.mitigation.ranking.MitigationURIs;

public abstract class MitigationTestBase extends TestBase {

	// Abstract variables for concrete test classes
	protected abstract String getFolderName();

	protected abstract String getFilesName();

	protected abstract List<Predicate<? super AbstractVertex<?>>> getConstraints();

	// Mitigation ranking variables
	protected final TrainDataGeneration trainDataGeneration = new TrainDataGeneration();
	protected final String scriptDirectory = Paths.get("scripts", "uncertaintyRanking").toString();
	protected final String trainDataDirectory = Paths.get(scriptDirectory, "train_data_files").toString();
	protected final String pathToUncertaintyRankingScript = Paths.get(scriptDirectory, "uncertainty_ranking.py")
			.toString();
	protected final String pathToRelevantUncertainties = "relevantUncertainties.txt";

	// URIs for mitigation
	protected final URI modelUncertaintyURI = ResourceUtils.createRelativePluginURI(
			Paths.get("models", getFolderName(), getFilesName() + ".uncertainty").toString(), TEST_MODEL_PROJECT_NAME);
	protected final URI mitigationUncertaintyURI = ResourceUtils.createRelativePluginURI(
			Paths.get("models", "mitigation", "mitigation.uncertainty").toString(), TEST_MODEL_PROJECT_NAME);

	// Evaluation variables
	protected final String pathToMeassurements = "meassurements.txt";
	protected final boolean evalMode = false;
	
	// Mitigation execution variables
	protected final int MITIGATION_RUNS = 30;
	protected final MitigationStrategy mitigationStrategy = MitigationStrategy.INCREASING;

	@BeforeEach
	public void before() {
		final var dataFlowDiagramPath = Paths.get(getBaseFolder(), getFolderName(), getFilesName() + ".dataflowdiagram")
				.toString();
		final var dataDictionaryPath = Paths.get(getBaseFolder(), getFolderName(), getFilesName() + ".datadictionary")
				.toString();
		final var uncertaintyPath = Paths.get(getBaseFolder(), getFolderName(), getFilesName() + ".uncertainty")
				.toString();

		var builder = new DFDUncertaintyAwareConfidentialityAnalysisBuilder().standalone()
				.modelProjectName(TEST_MODEL_PROJECT_NAME).usePluginActivator(Activator.class)
				.useDataDictionary(dataDictionaryPath).useDataFlowDiagram(dataFlowDiagramPath)
				.useUncertaintyModel(uncertaintyPath);

		UncertaintyAwareConfidentialityAnalysis analysis = builder.build();
		analysis.initializeAnalysis();

		// Load datadictonary, dataflowdiagram and uncertainties
		var resourceProvider = (DFDUncertaintyResourceProvider) analysis.getResourceProvider();
		resourceProvider.loadRequiredResources();
		var dd = resourceProvider.getDataDictionary();
		var dfd = resourceProvider.getDataFlowDiagram();

		DataFlowDiagramConverter conv = new DataFlowDiagramConverter();
		var web = conv.dfdToWeb(new DataFlowDiagramAndDictionary(dfd, dd));
		conv.storeWeb(web, "test.json");
	}

	public void storeRankingResult(List<String> relevantUncertaintyIds) {
		Path filePath = Paths.get(pathToRelevantUncertainties);
		var content = String.join(System.lineSeparator(), relevantUncertaintyIds);
		try {
			Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void deleteOldMeassurement() {
		Path filePath = Paths.get(pathToMeassurements);
		try {
			Files.write(filePath, "".getBytes(StandardCharsets.UTF_8));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void storeMeassurement(long meassurement) {
		Path filePath = Paths.get(pathToMeassurements);
		try {
			var content = Files.readString(filePath);
			content += Long.toString(meassurement) + System.lineSeparator();
			Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<String> loadRanking() {
		Path filePath = Paths.get(pathToRelevantUncertainties);
		try {
			if (!Files.isRegularFile(filePath)) {
				System.out.println("ranking does not exist");
				return new ArrayList<>();
			}
			return Files.readAllLines(filePath);
		} catch (IOException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	public List<MitigationModel> mitigateWithIncreasingAmountOfUncertainties(List<String> rankedUncertaintyEntityName,
			UncertaintyAwareConfidentialityAnalysis analysis, DataFlowDiagramAndDictionary dfdAnddd) {
		List<MitigationModel> result = new ArrayList<MitigationModel>();
		// Increase amount of uncertainties used if the current amount is not enough
		for (int i = 1; i <= rankedUncertaintyEntityName.size(); i++) {

			// Extract relevant uncertainties
			var relevantUncertaintyEntityNames = rankedUncertaintyEntityName.stream().limit(i).toList();
			var relevantUncertainties = analysis.getUncertaintySources().stream()
					.filter(u -> relevantUncertaintyEntityNames.contains(u.getEntityName())).toList();

			// Run mitigation with i+1 uncertainties
			result = MitigationModelCalculator.findMitigatingModel(dfdAnddd,
					new UncertaintySubset(analysis.getUncertaintySources(), relevantUncertainties),
					new MitigationURIs(modelUncertaintyURI, mitigationUncertaintyURI), getConstraints(), evalMode,
					Activator.class);

			if (result.size() > 0) {
				if (evalMode)
					break;
				else {
					var resultMinimal = MitigationListSimplifier.simplifyMitigationList(
							result.stream().map(m -> m.chosenScenarios()).toList(),
							analysis.getUncertaintySources().stream()
									.map(u -> UncertaintyUtils.getUncertaintyScenarios(u).size()).toList());
					System.out.println(i);
					System.out.println(result);
					System.out.println(relevantUncertaintyEntityNames);
					System.out.println(relevantUncertainties.stream().map(u -> u.getEntityName()).toList());
					for (int k = 0; k < resultMinimal.size(); k++) {
						System.out.println(resultMinimal.get(k));
					}
					break;
				}
			}
		}
		return result;
	}

	public List<MitigationModel> mitigateWithFixAmountOfUncertainties(List<String> rankedUncertaintyEntityName, int n,
			UncertaintyAwareConfidentialityAnalysis analysis, DataFlowDiagramAndDictionary dfdAnddd) {
		List<MitigationModel> result = new ArrayList<MitigationModel>();
		// Extract relevant uncertainties
		var relevantEntityNames = rankedUncertaintyEntityName.stream().limit(n).toList();
		var relevantUncertainties = analysis.getUncertaintySources().stream().filter(u -> relevantEntityNames.contains(u.getEntityName()))
				.toList();

		// Execute mitigation
		result = MitigationModelCalculator.findMitigatingModel(dfdAnddd,
				new UncertaintySubset(analysis.getUncertaintySources(), relevantUncertainties),
				new MitigationURIs(modelUncertaintyURI, mitigationUncertaintyURI), getConstraints(), evalMode,
				Activator.class);

		if (result.size() > 0 && !evalMode) {
			var resultMinimal = MitigationListSimplifier.simplifyMitigationList(
					result.stream().map(m -> m.chosenScenarios()).toList(), analysis.getUncertaintySources()
							.stream().map(u -> UncertaintyUtils.getUncertaintyScenarios(u).size()).toList());
			System.out.println(result);
			System.out.println(relevantUncertainties.stream().map(u -> u.getEntityName()).toList());
			for (int k = 0; k < resultMinimal.size(); k++) {
				System.out.println(resultMinimal.get(k));
			}

		}

		return result;
	}
	
	protected UncertaintyAwareConfidentialityAnalysis getAnalysis() {
		final var dataFlowDiagramPath = Paths.get(getBaseFolder(), getFolderName(), getFilesName() + ".dataflowdiagram")
				.toString();
		final var dataDictionaryPath = Paths.get(getBaseFolder(), getFolderName(), getFilesName() + ".datadictionary")
				.toString();
		final var uncertaintyPath = Paths.get(getBaseFolder(), getFolderName(), getFilesName() + ".uncertainty")
				.toString();
		var builder = new DFDUncertaintyAwareConfidentialityAnalysisBuilder().standalone()
				.modelProjectName(TEST_MODEL_PROJECT_NAME).usePluginActivator(Activator.class)
				.useDataDictionary(dataDictionaryPath).useDataFlowDiagram(dataFlowDiagramPath)
				.useUncertaintyModel(uncertaintyPath);

		UncertaintyAwareConfidentialityAnalysis analysis = builder.build();
		analysis.initializeAnalysis();
		return analysis;
	}

}
