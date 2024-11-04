package dev.abunai.confidentiality.mitigation.tests;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.utils.ResourceUtils;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.BeforeEach;

import dev.abunai.confidentiality.analysis.UncertaintyAwareConfidentialityAnalysis;
import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainFlowGraphCollection;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainTransposeFlowGraph;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyAwareConfidentialityAnalysisBuilder;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyResourceProvider;
import dev.abunai.confidentiality.mitigation.ranking.MitigationListSimplifier;
import dev.abunai.confidentiality.mitigation.ranking.MitigationModel;
import dev.abunai.confidentiality.mitigation.ranking.MitigationModelCalculator;
import dev.abunai.confidentiality.mitigation.ranking.TrainDataGeneration;
import dev.abunai.confidentiality.mitigation.ranking.UncertaintyRanker;
import dev.abunai.confidentiality.mitigation.ranking.UncertaintySubset;
import dev.abunai.confidentiality.mitigation.ranking.MitigationURIs;
import dev.abunai.confidentiality.mitigation.ranking.RankerType;
import dev.abunai.confidentiality.mitigation.ranking.RankingAggregationMethod;

public abstract class MitigationTestBase extends TestBase {

	// Abstract variables for concrete test classes
	protected abstract String getFolderName();

	protected abstract String getFilesName();

	protected abstract List<Predicate<? super AbstractVertex<?>>> getConstraints();

	protected abstract RankerType getRankerType();

	protected abstract RankingAggregationMethod getAggregationMethod();

	protected String customPythonPath() {
		return "python3";
	}

	// Mitigation ranking variables
	protected final TrainDataGeneration trainDataGeneration = new TrainDataGeneration();
	protected final String scriptDirectory = Paths.get("scripts", "uncertaintyRanking").toString();
	protected final String trainDataDirectory = Paths.get(scriptDirectory, "train_data_files").toString();
	protected final String pathToUncertaintyRankingScript = Paths.get(scriptDirectory, "uncertainty_ranking.py")
			.toString();

	// URIs for mitigation
	protected final URI modelUncertaintyURI = ResourceUtils.createRelativePluginURI(
			Paths.get("models", getFolderName(), getFilesName() + ".uncertainty").toString(), TEST_MODEL_PROJECT_NAME);
	protected final URI mitigationUncertaintyURI = ResourceUtils.createRelativePluginURI(
			Paths.get("mitigation", "mitigation.uncertainty").toString(), TEST_MODEL_PROJECT_NAME);

	// Evaluation variables
	protected final String pathToMeassurements = "meassurements.txt";
	protected final boolean evalMode = false;
	protected final String pathToRankingSolution = Paths
			.get("models", getFolderName(), getFilesName() + "_solution.txt").toString();

	// Mitigation execution variables
	protected final int MITIGATION_RUNS = 1; // Must be at least 3 for meassurments
	protected MitigationStrategy mitigationStrategy = MitigationStrategy.INCREASING;

	protected List<String> relevantUncertaintyEntityNames;

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
	}

	public void storeMeassurementResults(List<Float> meassurements, String rankerType, String aggregationMethod) {
		Path filePath = Paths.get("meassurement_results.txt");
		try {
			var content = Files.exists(filePath) ? Files.readString(filePath) : "";
			content += rankerType + " " + aggregationMethod + System.lineSeparator();
			if (rankerType.equals("BRUTE FORCE")) {
				content += "Runtime: " + Float.toString(meassurements.get(0)) + System.lineSeparator();
			} else {
				content += "Increasing: " + Float.toString(meassurements.get(0)) + System.lineSeparator();
				content += "Quater: " + Float.toString(meassurements.get(1)) + System.lineSeparator();
				content += "Half: " + Float.toString(meassurements.get(2)) + System.lineSeparator();
				Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void storeMeassurementResult(float meassurement, String tag) {
		Path filePath = Paths.get("meassurement_results.txt");
		try {
			var content = Files.exists(filePath) ? Files.readString(filePath) : "";
			content += tag + ": " + Float.toString(meassurement) + System.lineSeparator();
			Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void storeTrainingDataResults(List<Float> meassurements, String rankerType, String aggregationMethod) {
		Path filePath = Paths.get("meassurement_results.txt");
		try {
			var content = Files.exists(filePath) ? Files.readString(filePath) : "";
			content += "TRAIN_DATA " + rankerType + " " + aggregationMethod + System.lineSeparator();

			float increasing_training_duration = 0;
			float quarter_training_duration = 0;
			float half_training_duration = 0;

			var skip_amount = MITIGATION_RUNS / 3;
			var average_amount = 2 * skip_amount;

			for (int i = skip_amount - 1; i < meassurements.size(); i++) {
				if (i == MITIGATION_RUNS || i == 2 * MITIGATION_RUNS) {
					i += skip_amount - 1;
					continue;
				}
				if (i < MITIGATION_RUNS) {
					increasing_training_duration += meassurements.get(i);
				} else if (i < 2 * MITIGATION_RUNS) {
					quarter_training_duration += meassurements.get(i);
				} else {
					half_training_duration += meassurements.get(i);
				}
			}

			increasing_training_duration /= average_amount;
			quarter_training_duration /= average_amount;
			half_training_duration /= average_amount;

			content += "Increasing: " + Float.toString(increasing_training_duration) + System.lineSeparator();
			content += "Quater: " + Float.toString(quarter_training_duration) + System.lineSeparator();
			content += "Half: " + Float.toString(half_training_duration) + System.lineSeparator();
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
			var content = Files.exists(filePath) ? Files.readString(filePath) : "";
			content += Long.toString(meassurement) + System.lineSeparator();
			Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<String> loadSolutionRanking() {
		Path filePath = Paths.get(pathToRankingSolution);
		try {
			if (!Files.isRegularFile(filePath)) {
				System.out.println("Metric Calcuation Solution does not exist");
				return new ArrayList<>();
			}
			return Files.readAllLines(filePath);
		} catch (IOException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	public float seeAverageRuntime() {
		Path filePath = Paths.get(pathToMeassurements);
		if (!Files.isRegularFile(filePath)) {
			System.out.println("run mitigation first !!!");

		}
		try {
			var contentLines = Files.readAllLines(filePath);
			int sum = 0;
			var warmupEnd = MITIGATION_RUNS / 3;
			for (int i = contentLines.size() - 2 * warmupEnd; i < contentLines.size() && i >= 0; i++) {
				sum += Integer.parseInt(contentLines.get(i));
			}
			System.out.println((float) sum / ((float) 2 * warmupEnd));
			return (float) sum / ((float) 2 * warmupEnd);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0.0f;
	}

	public void printMetricies() {
		var solutionRanking = loadSolutionRanking();
		var programRanking = relevantUncertaintyEntityNames;
		var k = solutionRanking.size();
		var r = MetricCalculator.determineR(solutionRanking, programRanking);
		System.out.println("P@K");
		System.out.println(MetricCalculator.calculatePAtK(k, solutionRanking, programRanking));
		System.out.println("MAP@K");
		System.out.println(MetricCalculator.calculateMAPAtK(k, solutionRanking, programRanking));
		System.out.println("P@R");
		System.out.println(MetricCalculator.calculatePAtK(r, solutionRanking, programRanking));
		System.out.println("MAP@R");
		System.out.println(MetricCalculator.calculateMAPAtK(r, solutionRanking, programRanking));
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
							result.stream().map(m -> m.chosenScenarios()).toList(), analysis.getUncertaintySources()
									.stream().map(u -> UncertaintyUtils.getUncertaintyScenarios(u).size()).toList());
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
		var relevantUncertainties = analysis.getUncertaintySources().stream()
				.filter(u -> relevantEntityNames.contains(u.getEntityName())).toList();

		// Execute mitigation
		result = MitigationModelCalculator.findMitigatingModel(dfdAnddd,
				new UncertaintySubset(analysis.getUncertaintySources(), relevantUncertainties),
				new MitigationURIs(modelUncertaintyURI, mitigationUncertaintyURI), getConstraints(), evalMode,
				Activator.class);

		if (result.size() > 0 && !evalMode) {
			var resultMinimal = MitigationListSimplifier.simplifyMitigationList(
					result.stream().map(m -> m.chosenScenarios()).toList(), analysis.getUncertaintySources().stream()
							.map(u -> UncertaintyUtils.getUncertaintyScenarios(u).size()).toList());
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

	public void createTrainData() {
		var trainDir = new File(trainDataDirectory);
		for (File file : trainDir.listFiles()) {
			file.delete();
		}
		var analysis = this.getAnalysis();
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
		relevantUncertaintyEntityNames = UncertaintyRanker.rankUncertaintiesBasedOnTrainData(customPythonPath(),
				pathToUncertaintyRankingScript, trainDataDirectory, analysis.getUncertaintySources().size(),
				getRankerType(), getAggregationMethod());

	}

	public void createMitigationCandidatesAutomatically() {
		var analysis = getAnalysis();
		var rankedUncertaintyEntityName = mitigationStrategy.equals(MitigationStrategy.BRUTE_FORCE)
				? BruteForceUncertaintyFinder.getBruteForceUncertaintyEntityNames(getAnalysis())
				: relevantUncertaintyEntityNames;
		var ddAndDfd = getDDAndDfd(analysis);
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
		} else if (mitigationStrategy.equals(MitigationStrategy.HALF)) {
			result = mitigateWithFixAmountOfUncertainties(rankedUncertaintyEntityName,
					rankedUncertaintyEntityName.size() / 2, analysis, ddAndDfd);
			if (result.size() == 0) {
				result = mitigateWithFixAmountOfUncertainties(rankedUncertaintyEntityName,
						rankedUncertaintyEntityName.size(), analysis, ddAndDfd);
			}
		} else {
			result = mitigateWithFixAmountOfUncertainties(rankedUncertaintyEntityName,
					rankedUncertaintyEntityName.size(), analysis, ddAndDfd);
		}
		if (result.size() == 0) {
			System.out.println("mitigation failed");
		}
	}

	public DataFlowDiagramAndDictionary getDDAndDfd(UncertaintyAwareConfidentialityAnalysis analysis) {
		var resourceProvider = (DFDUncertaintyResourceProvider) analysis.getResourceProvider();
		resourceProvider.loadRequiredResources();
		var dd = resourceProvider.getDataDictionary();
		var dfd = resourceProvider.getDataFlowDiagram();
		return new DataFlowDiagramAndDictionary(dfd, dd);
	}

}
