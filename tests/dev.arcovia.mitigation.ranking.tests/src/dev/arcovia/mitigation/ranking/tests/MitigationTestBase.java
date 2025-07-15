package dev.arcovia.mitigation.ranking.tests;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.utils.ResourceUtils;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.BeforeEach;

import dev.abunai.confidentiality.analysis.UncertaintyAwareConfidentialityAnalysis;
import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainFlowGraphCollection;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainTransposeFlowGraph;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyAwareConfidentialityAnalysisBuilder;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyResourceProvider;
import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.arcovia.mitigation.ranking.MitigationListSimplifier;
import dev.arcovia.mitigation.ranking.MitigationModel;
import dev.arcovia.mitigation.ranking.MitigationModelCalculator;
import dev.arcovia.mitigation.ranking.MitigationStrategy;
import dev.arcovia.mitigation.ranking.MitigationURIs;
import dev.arcovia.mitigation.ranking.RankerType;
import dev.arcovia.mitigation.ranking.RankingAggregationMethod;
import dev.arcovia.mitigation.ranking.TrainDataGeneration;
import dev.arcovia.mitigation.ranking.UncertaintyRanker;
import dev.arcovia.mitigation.ranking.UncertaintySubset;

public abstract class MitigationTestBase extends TestBase {
    private final Logger logger = Logger.getLogger(MitigationTestBase.class);

    // Abstract variables for concrete test classes
    protected abstract String getFolderName();

    protected abstract String getFilesName();

    protected abstract List<Predicate<? super AbstractVertex<?>>> getConstraints();

    protected abstract RankerType getRankerType();

    protected abstract RankingAggregationMethod getAggregationMethod();

    protected String customPythonPath() {
        return "/Users/nniehues/miniconda3/bin/python";
    }

    // Mitigation ranking variables
    protected final TrainDataGeneration trainDataGeneration = new TrainDataGeneration();
    protected final String scriptDirectory = Paths.get("scripts", "uncertaintyRanking")
            .toString();
    protected final String trainDataDirectory = Paths.get(scriptDirectory, "train_data_files")
            .toString();
    protected final String pathToUncertaintyRankingScript = Paths.get(scriptDirectory, "uncertainty_ranking.py")
            .toString();

    // URIs for mitigation
    protected final URI modelUncertaintyURI = ResourceUtils
            .createRelativePluginURI(Paths.get("models", getFolderName(), getFilesName() + ".uncertainty")
                    .toString(), TEST_MODEL_PROJECT_NAME);
    protected final URI mitigationUncertaintyURI = ResourceUtils.createRelativePluginURI(Paths.get("mitigation", "mitigation.uncertainty")
            .toString(), TEST_MODEL_PROJECT_NAME);

    // Evaluation variables
    protected final String pathToMeassurements = "meassurements.txt";
    protected final boolean evalMode = true;
    protected final String pathToRankingSolution = Paths.get("models", getFolderName(), getFilesName() + "_solution.txt")
            .toString();

    // Mitigation execution variables
    protected final int MITIGATION_RUNS = 12; // Must be at least 3 for measurements
    protected MitigationStrategy mitigationStrategy = MitigationStrategy.INCREASING;

    protected List<String> rankedUncertaintyEntityNames;

    @BeforeEach
    public void before() {
        var trainDir = new File(trainDataDirectory);
        if (!trainDir.exists()) {
            trainDir.mkdirs();
        }
        for (File file : trainDir.listFiles()) {
            file.delete();
        }
    }

    /**
     * Stores the measurement results by appending them to a file named "meassurement_results.txt".
     * Depending on the ranker type, the method writes runtime results or detailed increasing, quarter,
     * and half-measurements to the file.
     *
     * @param meassurements a list of float values representing measurement results
     * @param rankerType a string indicating the type of ranker (e.g., "BRUTE FORCE")
     * @param aggregationMethod a string indicating the aggregation method applied
     */
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

    /**
     * Stores a measurement result by appending it to a file named "meassurement_results.txt".
     * The result is written in the format: "{tag}: {meassurement}".
     * If the file does not exist, it is created. If it exists, the new measurement is appended.
     *
     * @param meassurement the measurement value to be stored
     * @param tag a string label associated with the measurement
     */
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

    /**
     * Stores the training data results by appending them to a file named "meassurement_results.txt".
     * The results include the average duration for increasing, quarter, and half training phases
     * based on measured values while utilizing the specified ranker type and aggregation method.
     *
     * @param meassurements a list of float values representing the measurement results
     * @param rankerType a string indicating the ranker type used (e.g., "BRUTE FORCE")
     * @param aggregationMethod a string describing the aggregation method applied
     */
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
        // preserve individual meassurements for statistical tests
        storeMeassurementResult(meassurement, "RAW");
    }

    /**
     * Loads a solution ranking from a file specified by the path stored in the class field
     * 'pathToRankingSolution'. If the file does not exist or an error occurs during file reading,
     * an empty list is returned. Otherwise, the method returns the lines of the file as a list
     * of strings.
     *
     * @return a list of strings representing the lines in the solution ranking file, or an empty list
     *         if the file does not exist or an error occurs.
     */
    public List<String> loadSolutionRanking() {
        Path filePath = Paths.get(pathToRankingSolution);
        try {
            if (!Files.isRegularFile(filePath)) {
                logger.error("Metric Calcuation Solution does not exist");
                return new ArrayList<>();
            }
            return Files.readAllLines(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Computes and returns the average runtime measurement based on values stored in a file.
     * The method ignores initial "warmup" runs and calculates the average only on the relevant data.
     * Logs the computed average runtime for reference.
     *
     * If the measurement file does not exist or an error occurs during file reading,
     * a default value of 0.0f is returned.
     *
     * @return the average runtime as a float, or 0.0f if the file is invalid or an error occurs
     */
    public float seeAverageRuntime() {
        Path filePath = Paths.get(pathToMeassurements);
        if (!Files.isRegularFile(filePath)) {
            logger.error("Execute mitigation first");

        }
        try {
            var contentLines = Files.readAllLines(filePath);
            int sum = 0;
            var warmupEnd = MITIGATION_RUNS / 3;
            for (int i = contentLines.size() - 2 * warmupEnd; i < contentLines.size() && i >= 0; i++) {
                sum += Integer.parseInt(contentLines.get(i));
            }
            logger.info("Average Runtime: " + (float) sum / ((float) 2 * warmupEnd));
            return (float) sum / ((float) 2 * warmupEnd);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0.0f;
    }

    /**
     * Logs precision and mean average precision metrics for given rankings.
     * This method computes precision at K (P@K) and mean average precision at K (MAP@K)
     * for a set of solution rankings and program rankings. The values are logged using
     * the class logger. Additionally, the method calculates the metrics based on R,
     * which is determined by the rank of the last relevant element in the program ranking.
     *
     * The following calculations and logs are performed:
     * - P@K: Precision of the top K elements in the program ranking compared to the solution ranking.
     * - MAP@K: Mean average precision of the top K elements in the program ranking.
     * - P@R: Precision of the top R elements, where R is the rank of the last relevant element.
     * - MAP@R: Mean average precision of the top R elements.
     *
     * Preconditions:
     * - The solution ranking is loaded via the `loadSolutionRanking` method.
     * - The program ranking is derived from the class field `rankedUncertaintyEntityNames`.
     */
    public void printMetricies() {
        var solutionRanking = loadSolutionRanking();
        var programRanking = rankedUncertaintyEntityNames;
        var k = solutionRanking.size();
        var r = MetricCalculator.determineR(solutionRanking, programRanking);
        logger.info("P@K");
        logger.info(MetricCalculator.calculatePAtK(k, solutionRanking, programRanking));
        logger.info("MAP@K");
        logger.info(MetricCalculator.calculateMAPAtK(k, solutionRanking, programRanking));
        logger.info("P@R");
        logger.info(MetricCalculator.calculatePAtK(r, solutionRanking, programRanking));
        logger.info("MAP@R");
        logger.info(MetricCalculator.calculateMAPAtK(r, solutionRanking, programRanking));
    }

    /**
     * Executes a mitigation process by iteratively increasing the number of uncertainties
     * considered for mitigation until a valid mitigation is found or all uncertainties are reviewed.
     * The method begins with the top-ranked uncertainty and gradually includes more uncertainties
     * from the provided ranked list until mitigations are computed or no solutions are viable.
     *
     * @param rankedUncertaintyEntityName a list of entity names ranked by their uncertainty level or priority.
     * @param analysis an object that provides information about uncertainty sources and analyzes confidentiality.
     * @param dfdAnddd a data structure combining a data flow diagram (DFD) and its dictionary, used for mitigation.
     */
    public List<MitigationModel> mitigateWithIncreasingAmountOfUncertainties(List<String> rankedUncertaintyEntityName,
            UncertaintyAwareConfidentialityAnalysis analysis, DataFlowDiagramAndDictionary dfdAnddd) {
        // Increase amount of uncertainties used if the current amount is not enough
        for (int i = 1; i <= rankedUncertaintyEntityName.size(); i++) {

            List<MitigationModel> result = mitigateWithFixAmountOfUncertainties(rankedUncertaintyEntityName, i, analysis, dfdAnddd);

            if (result.size() > 0) {
                return result;
            }
        }
        return List.of();
    }

    /**
     * Executes a mitigation process on a fixed number of uncertainties ranked by their entity names,
     * and returns the corresponding mitigation models. The method identifies "n" top-ranked uncertainties,
     * calculates the mitigations based on the provided analysis and data flow diagram, and returns any
     * mitigation models that have been computed.
     *
     * @param rankedUncertaintyEntityName a list of entity names ranked by their significance or priority.
     * @param n the maximum number of top-ranked uncertainties to consider for mitigation.
     * @param analysis an object that provides information about uncertainty sources and supports analysis of confidentiality.
     * @param dfdAnddd a data structure that combines a data flow diagram and its associated dictionary, used for mitigation computation.
     * @return a list of MitigationModel objects representing the mitigation results for the chosen uncertainties.
     */
    public List<MitigationModel> mitigateWithFixAmountOfUncertainties(List<String> rankedUncertaintyEntityName, int n,
            UncertaintyAwareConfidentialityAnalysis analysis, DataFlowDiagramAndDictionary dfdAnddd) {
        List<MitigationModel> result = new ArrayList<MitigationModel>();

        // Get relevant (top n) uncertainties by entityName
        List<UncertaintySource> relevantUncertainties = analysis.getUncertaintySources()
                .stream()
                .filter(u -> rankedUncertaintyEntityName.stream()
                        .limit(n)
                        .collect(Collectors.toSet())
                        .contains(u.getEntityName()))
                .collect(Collectors.toList());

        // Execute mitigation
        result = new MitigationModelCalculator(dfdAnddd, new UncertaintySubset(analysis.getUncertaintySources(), relevantUncertainties),
                new MitigationURIs(modelUncertaintyURI, mitigationUncertaintyURI), getConstraints(), evalMode).findMitigatingModel();

        if (result.size() > 0 && !evalMode)
            printEval(result, analysis, relevantUncertainties);

        return result;
    }

    private void printEval(List<MitigationModel> result, UncertaintyAwareConfidentialityAnalysis analysis,
            List<UncertaintySource> relevantUncertainties) {
        var resultMinimal = MitigationListSimplifier.simplifyMitigationList(result.stream()
                .map(m -> m.chosenScenarios())
                .toList(),
                analysis.getUncertaintySources()
                        .stream()
                        .map(u -> UncertaintyUtils.getUncertaintyScenarios(u)
                                .size())
                        .toList());
        logger.info(result);
        logger.info(relevantUncertainties.stream()
                .map(u -> u.getEntityName())
                .toList());
        for (int k = 0; k < resultMinimal.size(); k++) {
            logger.info(resultMinimal.get(k));
        }
    }

    protected UncertaintyAwareConfidentialityAnalysis getAnalysis() {
        final var dataFlowDiagramPath = Paths.get(getBaseFolder(), getFolderName(), getFilesName() + ".dataflowdiagram")
                .toString();
        final var dataDictionaryPath = Paths.get(getBaseFolder(), getFolderName(), getFilesName() + ".datadictionary")
                .toString();
        final var uncertaintyPath = Paths.get(getBaseFolder(), getFolderName(), getFilesName() + ".uncertainty")
                .toString();
        var builder = new DFDUncertaintyAwareConfidentialityAnalysisBuilder().standalone()
                .modelProjectName(TEST_MODEL_PROJECT_NAME)
                .usePluginActivator(Activator.class)
                .useDataDictionary(dataDictionaryPath)
                .useDataFlowDiagram(dataFlowDiagramPath)
                .useUncertaintyModel(uncertaintyPath);

        UncertaintyAwareConfidentialityAnalysis analysis = builder.build();
        analysis.initializeAnalysis();
        return analysis;
    }

    /**
     * Generates a ranking of uncertainties within a model, based on violations of constraints applied
     * to uncertain data flows. This method performs the following steps:
     *
     * 1. Retrieves the analysis object and the list of constraints to be applied.
     * 2. Generates uncertain flow graphs from the existing flow graph contained in the analysis.
     * 3. Evaluates the generated uncertain flow graphs to determine their validity.
     * 4. Converts the uncertain flow graphs into a collection of uncertain transpose flow graphs.
     * 5. For each constraint:
     *    - Identifies violations by querying uncertain data flow using the constraint.
     *    - If constraint violations are found, generates training data and writes it to a CSV file
     *      specific to the constraint.
     * 6. Utilizes the generated training data to rank uncertainties in the model. The ranking is computed
     *    based on a specified ranker type, aggregation method, and mitigation strategy by the python script.
     *
     */
    public void createUncertaintyRanking() {
        var analysis = this.getAnalysis();
        // Get constraints and define count variable for constraint file differentiation
        List<Predicate<? super AbstractVertex<?>>> constraints = getConstraints();
        var count = 0;
        DFDUncertainFlowGraphCollection flowGraphs = (DFDUncertainFlowGraphCollection) analysis.findFlowGraph();
        DFDUncertainFlowGraphCollection uncertainFlowGraphs = flowGraphs.createUncertainFlows();

        uncertainFlowGraphs.evaluate();

        List<DFDUncertainTransposeFlowGraph> allTFGs = uncertainFlowGraphs.getTransposeFlowGraphs()
                .stream()
                .map(DFDUncertainTransposeFlowGraph.class::cast)
                .toList();

        // Generate train data for each constraint
        for (var constraint : constraints) {
            List<UncertainConstraintViolation> violations = analysis.queryUncertainDataFlow(uncertainFlowGraphs, constraint);

            // If no violation occurred for this constraint no traindata needs to be stored
            if (violations.size() > 0) {
                trainDataGeneration.violationDataToCSV(violations, allTFGs, analysis.getUncertaintySources(),
                        Paths.get(trainDataDirectory, "violations_" + Integer.toString(count) + ".csv")
                                .toString());
                count++;
            }

        }

        // Rank the uncertainties specified in the given model and store the result in
        // the specified file
        rankedUncertaintyEntityNames = UncertaintyRanker.rankUncertaintiesBasedOnTrainData(customPythonPath(), pathToUncertaintyRankingScript,
                trainDataDirectory, analysis.getUncertaintySources()
                        .size(),
                getRankerType(), getAggregationMethod(), mitigationStrategy);

    }

    /**
     * Automatically generates mitigation candidates for handling uncertainties within a given analysis based on a selected mitigation strategy.
     * This method determines the appropriate approach for mitigating uncertainties by evaluating entity names, uncertainty data,
     * and the chosen strategy, and then applies the corresponding mitigation logic to generate the most suitable mitigation candidates.
     *
     * The mitigation strategies supported include:
     * - INCREASING: Gradually mitigates an increasing amount of uncertainty sources.
     * - QUATER: Mitigates progressively with fixed fractions (quarters) of uncertainty sources.
     * - HALF: Starts with half the uncertainty sources, and expands if necessary.
     * - CLUSTER: Applies mitigation based on clustering analysis of uncertainty data.
     * - FAST_START: Optimizes mitigation by quickly identifying the smallest number of uncertainty sources to resolve the issue.
     * - BRUTE_FORCE (default): Mitigates all uncertainty sources in one attempt.
     *
     * The method ensures there is at least one valid mitigation result generated, as indicated by the final assertion.
     */
    public void createMitigationCandidatesAutomatically() {
        var analysis = getAnalysis();
        var uncertaintyEntityNames = mitigationStrategy.equals(MitigationStrategy.BRUTE_FORCE)
                ? BruteForceUncertaintyFinder.getBruteForceUncertaintyEntityNames(getAnalysis())
                : rankedUncertaintyEntityNames;
        var ddAndDfd = getDDAndDfd(analysis);
        List<MitigationModel> result = new ArrayList<>();

        switch (mitigationStrategy) {
            case INCREASING -> {
                result = mitigateWithIncreasingAmountOfUncertainties(uncertaintyEntityNames, analysis, ddAndDfd);
            }
            case QUATER -> {
                for (int i = 1; i <= 4; i++) {
                    result = mitigateWithFixAmountOfUncertainties(uncertaintyEntityNames, i * analysis.getUncertaintySources()
                            .size() / 4, analysis, ddAndDfd);
                    if (result.size() != 0) {
                        break;
                    }
                }
            }
            case HALF -> {
                result = mitigateWithFixAmountOfUncertainties(uncertaintyEntityNames, analysis.getUncertaintySources()
                        .size() / 2, analysis, ddAndDfd);
                if (result.size() == 0) {
                    result = mitigateWithFixAmountOfUncertainties(uncertaintyEntityNames, analysis.getUncertaintySources()
                            .size(), analysis, ddAndDfd);
                }
            }
            case CLUSTER -> {
                result = executeCluster(uncertaintyEntityNames, analysis, ddAndDfd);
            }
            case FAST_START -> {
                int threshold = analysis.getUncertaintySources()
                        .size() / 2;
                int n = analysis.getUncertaintySources()
                        .size();
                int i = 1;

                while (i <= n) {
                    result = mitigateWithFixAmountOfUncertainties(uncertaintyEntityNames, i, analysis, ddAndDfd);
                    if (result.size() != 0) {
                        break;
                    }

                    if (i * 2 > threshold) {
                        i++;
                    } else {
                        i *= 2;
                    }
                }
            }
            // BruteForce
            default -> {
                result = mitigateWithFixAmountOfUncertainties(uncertaintyEntityNames, analysis.getUncertaintySources()
                        .size(), analysis, ddAndDfd);
            }
        }
        assertTrue(result.size() > 0);
    }

    private List<MitigationModel> executeCluster(List<String> uncertaintyEntityNames, UncertaintyAwareConfidentialityAnalysis analysis,
            DataFlowDiagramAndDictionary ddAndDfd) {
        String separator = "_Cluster-Separator_";
        var prunedRankedEntityNames = uncertaintyEntityNames.stream()
                .filter(n -> !n.startsWith(separator))
                .toList();

        List<List<String>> clusters = new ArrayList<>();
        List<String> currentCluster = new ArrayList<>();
        for (var entityName : uncertaintyEntityNames) {
            if (entityName.startsWith(separator)) {
                if (!currentCluster.isEmpty()) {
                    clusters.add(currentCluster);
                }
                currentCluster = new ArrayList<>();
            } else {
                currentCluster.add(entityName);
            }
        }
        if (!currentCluster.isEmpty()) {
            clusters.add(currentCluster);
        }

        var clusterSizes = clusters.stream()
                .map(c -> c.size())
                .toList();
        List<Integer> summedClusterSizes = new ArrayList<>();

        int sum = 0;
        for (int num : clusterSizes) {
            sum += num;
            summedClusterSizes.add(sum);
        }
        List<MitigationModel> result = new ArrayList<>();
        for (int size : summedClusterSizes) {
            result = mitigateWithFixAmountOfUncertainties(prunedRankedEntityNames, size, analysis, ddAndDfd);
            if (result.size() != 0) {
                break;
            }
        }
        return result;

    }

    public DataFlowDiagramAndDictionary getDDAndDfd(UncertaintyAwareConfidentialityAnalysis analysis) {
        var resourceProvider = (DFDUncertaintyResourceProvider) analysis.getResourceProvider();
        resourceProvider.loadRequiredResources();
        var dd = resourceProvider.getDataDictionary();
        var dfd = resourceProvider.getDataFlowDiagram();
        return new DataFlowDiagramAndDictionary(dfd, dd);
    }

}
