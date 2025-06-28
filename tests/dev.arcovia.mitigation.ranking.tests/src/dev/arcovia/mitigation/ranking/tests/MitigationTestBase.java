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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.dfd.DFDConfidentialityAnalysis;
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
    private final Logger logger = Logger.getLogger(DFDConfidentialityAnalysis.class);

    // Abstract variables for concrete test classes
    protected abstract String getFolderName();

    protected abstract String getFilesName();

    protected abstract List<Predicate<? super AbstractVertex<?>>> getConstraints();

    protected abstract RankerType getRankerType();

    protected abstract RankingAggregationMethod getAggregationMethod();

    protected String customPythonPath() {
        return "D:/entwicklungsumgebungen/Conda/envs/MitigationRanking/python";
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
    protected final int MITIGATION_RUNS = 12; // Must be at least 3 for meassurments
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
        // preserve individual meassurements for statistical tests
        storeMeassurementResult(meassurement, "RAW");
    }

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
            logger.info((float) sum / ((float) 2 * warmupEnd));
            return (float) sum / ((float) 2 * warmupEnd);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0.0f;
    }

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

    public List<MitigationModel> mitigateWithIncreasingAmountOfUncertainties(List<String> rankedUncertaintyEntityName,
            UncertaintyAwareConfidentialityAnalysis analysis, DataFlowDiagramAndDictionary dfdAnddd) {
        List<MitigationModel> result = new ArrayList<MitigationModel>();
        // Increase amount of uncertainties used if the current amount is not enough
        for (int i = 1; i <= rankedUncertaintyEntityName.size(); i++) {

            result = mitigateWithFixAmountOfUncertainties(rankedUncertaintyEntityName, i, analysis, dfdAnddd);

            if (result.size() > 0) {
                break;
            }
        }
        return result;
    }

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

            // If no violation occurred no mitigation needs to be executed
            if (violations.size() == 0) {
                logger.error("No violations found - terminating mitigation");
                System.exit(0);
            }

            trainDataGeneration.violationDataToCSV(violations, allTFGs, analysis.getUncertaintySources(),
                    Paths.get(trainDataDirectory, "violations_" + Integer.toString(count) + ".csv")
                            .toString());
            count++;

        }

        // Rank the uncertainties specified in the given model and store the result in
        // the specified file
        rankedUncertaintyEntityNames = UncertaintyRanker.rankUncertaintiesBasedOnTrainData(customPythonPath(), pathToUncertaintyRankingScript,
                trainDataDirectory, analysis.getUncertaintySources()
                        .size(),
                getRankerType(), getAggregationMethod(), mitigationStrategy);

    }

    public void createMitigationCandidatesAutomatically() {
        var analysis = getAnalysis();
        var uncertaintyEntityNames = mitigationStrategy.equals(MitigationStrategy.BRUTE_FORCE)
                ? BruteForceUncertaintyFinder.getBruteForceUncertaintyEntityNames(getAnalysis())
                : rankedUncertaintyEntityNames;
        var ddAndDfd = getDDAndDfd(analysis);
        List<MitigationModel> result = new ArrayList<>();

        switch (mitigationStrategy) {
            case INCREASING -> {
                for (int i = 1; i <= uncertaintyEntityNames.size(); i++) {
                    result = mitigateWithIncreasingAmountOfUncertainties(uncertaintyEntityNames, analysis, ddAndDfd);
                }
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
