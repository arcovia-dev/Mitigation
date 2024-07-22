package dev.abunai.confidentiality.mitigation.testBases;

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
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.BeforeEach;

import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyResourceProvider;
import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.abunai.confidentiality.analysis.tests.DFDTestBase;
import dev.abunai.confidentiality.mitigation.MitigationModelCalculator;
import dev.abunai.confidentiality.mitigation.TrainDataGenerationUnsupervised;

public abstract class MitigationTestBase extends DFDTestBase {

	protected abstract String getFolderName();
	protected abstract String getFilesName();
	protected abstract List<Predicate<? super AbstractVertex<?>>> getConstraints();
	
	private final static String ABUNAI_TEST_MODELS = "dev.abunai.confidentiality.analysis.testmodels";

	public DataDictionary dd;
	public DataFlowDiagram dfd;
	public List<UncertaintySource> uncertaintySources;

	protected final String scriptDirectory = Paths.get("..","dev.abunai.confidentiality.mitigation","scripts","uncertaintyRanking").toString();
	protected final String trainDataDirectory = Paths.get(scriptDirectory,"train_data_files").toString();
	protected final String pathToUncertaintyRankingScript = Paths.get(scriptDirectory,"uncertainty_ranking.py").toString();
	
	protected final String pathToRelevantUncertainties = "relevantUncertainties.txt";
	protected final String pathToMeassurements = "meassurements.txt";
	
	protected final URI testModels = ResourceUtils.createRelativePluginURI(Paths.get("models","dfd").toString(), ABUNAI_TEST_MODELS);
	protected final String pathFromTestModelsToMitigationFolder = "models/dfd/mitigation";
	protected final String pathToModelsUncertainty = testModels
			+ String.format("/%s/%s.uncertainty", getFolderName(), getFilesName());
	//use relative plugin path
	protected final String pathToMitigationModel = "C:\\Users\\Jonas\\Desktop\\Masterarbeit_Paper\\UncertaintyAwareConfidentialityAnalysis\\tests\\dev.abunai.confidentiality.analysis.testmodels\\models\\dfd\\mitigation";
	protected final String pathToMitigationModelUncertainty = testModels
			+ "/mitigation/mitigation.uncertainty";
	protected final TrainDataGenerationUnsupervised trainDataGeneration = new TrainDataGenerationUnsupervised();


	@BeforeEach
	public void before() {
		// Load datadictonary, dataflowdiagram and uncertainties
		var resourceProvider = (DFDUncertaintyResourceProvider) this.analysis.getResourceProvider();
		resourceProvider.loadRequiredResources();
		dd = resourceProvider.getDataDictionary();
		dfd = resourceProvider.getDataFlowDiagram();
		uncertaintySources = resourceProvider.getUncertaintySourceCollection().getSources();

		DataFlowDiagramConverter conv = new DataFlowDiagramConverter();
		var web = conv.dfdToWeb(new DataFlowDiagramAndDictionary(dfd, dd));
		conv.storeWeb(web, "testWeb.json");
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
			return Files.readAllLines(filePath);
		} catch (IOException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	public boolean mitigateWithIncreasingAmountOfUncertainties(List<String> rankedUncertaintyEntityName) {
		// Increase amount of uncertainties used if the current amount is not enough
		for (int i = 1; i <= rankedUncertaintyEntityName.size(); i++) {

			// Extract relevant uncertainties
			var relevantUncertaintyEntityName = rankedUncertaintyEntityName.stream().limit(i).toList();
			var relevantUncertainties = uncertaintySources.stream()
					.filter(u -> relevantUncertaintyEntityName.contains(u.getEntityName())).toList();

			// Run mitigation with i+1 uncertainties
			var result = MitigationModelCalculator.findMitigatingModel(
					new DataFlowDiagramAndDictionary(this.dfd, this.dd), uncertaintySources, relevantUncertainties,
					getConstraints(), pathToModelsUncertainty, pathToMitigationModel,
					pathFromTestModelsToMitigationFolder, pathToMitigationModelUncertainty);

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

	public boolean mitigateWithFixAmountOfUncertainties(List<String> rankedUncertaintyEntityName, int n) {

		// Extract relevant uncertainties
		var relevantEntityNames = rankedUncertaintyEntityName.stream().limit(n).toList();
		var relevantUncertainties = uncertaintySources.stream()
				.filter(u -> relevantEntityNames.contains(u.getEntityName())).toList();

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

}
