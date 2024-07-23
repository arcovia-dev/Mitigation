package dev.abunai.confidentiality.mitigation.testBases;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.File;  
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.utils.ResourceUtils;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.BeforeEach;

import dev.abunai.confidentiality.analysis.UncertaintyAwareConfidentialityAnalysis;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyAwareConfidentialityAnalysisBuilder;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyResourceProvider;
import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.abunai.confidentiality.mitigation.MitigationModelCalculator;
import dev.abunai.confidentiality.mitigation.TrainDataGenerationUnsupervised;
import dev.abunai.confidentiality.mitigation.tests.Activator;

public abstract class MitigationTestBase extends TestBase {

	// Abstract variables for concrete test classes
	protected abstract String getFolderName();
	protected abstract String getFilesName();
	protected abstract List<Predicate<? super AbstractVertex<?>>> getConstraints();
	
	// Mitigation preparation variables
	protected final TrainDataGenerationUnsupervised trainDataGeneration = new TrainDataGenerationUnsupervised();
	protected final String scriptDirectory = Paths.get(PROJECT_ROOT_PATH, "scripts", "uncertaintyRanking").toString();
	protected final String trainDataDirectory = Paths.get(scriptDirectory, "train_data_files").toString();
	protected final String pathToUncertaintyRankingScript = Paths.get(scriptDirectory, "uncertainty_ranking.py")
			.toString();
	protected final String pathToRelevantUncertainties = Paths.get(PROJECT_ROOT_PATH, "relevantUncertainties.txt")
			.toString();

	// Paths and URIs for mitigation
	protected final String pathToMitigationModel = Paths.get(PROJECT_ROOT_PATH, "models", "mitigation").toString();
	protected final URI modelUncertaintyURI = ResourceUtils.createRelativePluginURI(
			Paths.get("models", getFolderName(), getFilesName() + ".uncertainty").toString(), TEST_MODEL_PROJECT_NAME);
	protected final URI mitigationUncertaintyURI = ResourceUtils.createRelativePluginURI(
			Paths.get("models", "mitigation", "mitigation.uncertainty").toString(), TEST_MODEL_PROJECT_NAME);

	// Evaluation variables
	protected final String pathToMeassurements = Paths.get(PROJECT_ROOT_PATH, "meassurements.txt").toString();
	
	@BeforeEach
	public void before() {
		System.out.println(Paths.get(Paths.get("").toString(), "meassurements.txt").toString());
		System.out.println(Paths.get("meassurements.txt").toAbsolutePath().toString());

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
		dd = resourceProvider.getDataDictionary();
		dfd = resourceProvider.getDataFlowDiagram();
		this.analysis = analysis;
	}

	public void storeRankingResult(List<String> relevantUncertaintyIds) {
		Path filePath = Paths.get(pathToRelevantUncertainties);
		var content = String.join("\n", relevantUncertaintyIds);
		try {
			Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void storeMeassurement(long meassurement) {
		Path filePath = Paths.get(pathToMeassurements);
		try {
			if (!Files.isRegularFile(filePath)) {
				var file = new File(filePath.toString());
				file.createNewFile();
			}
			var content = Files.readString(filePath);
			content += Long.toString(meassurement) + "\n";
			Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<String> loadRanking() {
		Path filePath = Paths.get(pathToRelevantUncertainties);
		try {
			if (!Files.isRegularFile(filePath)) {
				System.out.println("ranking did not exist");
				return new ArrayList<>();
			}
			return Files.readAllLines(filePath);
		} catch (IOException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	public boolean mitigateWithIncreasingAmountOfUncertainties(List<String> rankedUncertaintyEntityName,
			List<UncertaintySource> sources) {
		// Increase amount of uncertainties used if the current amount is not enough
		for (int i = 1; i <= rankedUncertaintyEntityName.size(); i++) {

			// Extract relevant uncertainties
			var relevantUncertaintyEntityName = rankedUncertaintyEntityName.stream().limit(i).toList();
			var relevantUncertainties = sources.stream()
					.filter(u -> relevantUncertaintyEntityName.contains(u.getEntityName())).toList();

			// Run mitigation with i+1 uncertainties
			var result = MitigationModelCalculator.findMitigatingModel(
					new DataFlowDiagramAndDictionary(this.dfd, this.dd), sources, relevantUncertainties,
					pathToMitigationModel, TEST_MODEL_PROJECT_NAME, modelUncertaintyURI, mitigationUncertaintyURI,
					getConstraints(), Activator.class);

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

	public boolean mitigateWithFixAmountOfUncertainties(List<String> rankedUncertaintyEntityName, int n,
			List<UncertaintySource> sources) {

		// Extract relevant uncertainties
		var relevantEntityNames = rankedUncertaintyEntityName.stream().limit(n).toList();
		var relevantUncertainties = sources.stream().filter(u -> relevantEntityNames.contains(u.getEntityName()))
				.toList();

		// Execute mitigation
		var result = MitigationModelCalculator.findMitigatingModel(new DataFlowDiagramAndDictionary(this.dfd, this.dd),
				sources, relevantUncertainties, pathToMitigationModel, TEST_MODEL_PROJECT_NAME, modelUncertaintyURI,
				mitigationUncertaintyURI, getConstraints(), Activator.class);

		// Return success of mitgation
		if (result.size() > 0) {
			System.out.println(result);
			return true;
		}
		
		return false;
	}

}
