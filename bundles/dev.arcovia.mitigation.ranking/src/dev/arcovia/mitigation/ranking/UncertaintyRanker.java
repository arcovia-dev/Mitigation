package dev.arcovia.mitigation.ranking;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class UncertaintyRanker {
    /**
     * Executes the Python script for ranking uncertainties based on training data and returns the ranked results. This
     * method interacts with the Python script by invoking it with the given parameters and processes the output to return a
     * list of ranked uncertainties.
     * @param pythonPath Path to the Python interpreter to execute the script.
     * @param scriptPath Path to the Python script that performs the ranking.
     * @param pathToTrainDataFolder Path to the folder containing the training data used for ranking.
     * @param rankingLength The desired number of top-ranked uncertainties to output.
     * @param rankerType The type of ranking method or model to be used for ranking (e.g., PCA, LDA).
     * @param aggregationMethod The method used to aggregate rankings (e.g., SUM, EXPONENTIAL_RANKS).
     * @param mitigationStrategy The strategy for mitigating uncertainty during the ranking process (e.g., CLUSTER).
     * @return A list of ranked uncertainties as strings, or null if an error occurs during execution.
     */
    public static List<String> rankUncertaintiesBasedOnTrainData(String pythonPath, String scriptPath, String pathToTrainDataFolder,
            int rankingLength, RankerType rankerType, RankingAggregationMethod aggregationMethod, MitigationStrategy mitigationStrategy) {
        // Command to run the Python script
        String[] command = {pythonPath, scriptPath, pathToTrainDataFolder, Integer.toString(rankingLength), getRankerTypeCommandParameter(rankerType),
                getAggregationMethodCommandParamter(aggregationMethod), getBatchSizeOptimizationParameter(mitigationStrategy)};
        try {
            // Execute the command
            Process process = Runtime.getRuntime()
                    .exec(command);

            // Read the output from the process
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line)
                        .append(System.lineSeparator());
            }

            BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder errorOutput = new StringBuilder();
            while ((line = stdErr.readLine()) != null) {
                errorOutput.append(line)
                        .append(System.lineSeparator());
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                Logger logger = Logger.getLogger(UncertaintyRanker.class);
                logger.error("Python script failed with exit code " + exitCode);
                logger.error("Error output:\n" + errorOutput.toString());
                throw new RuntimeException("Python script execution failed.");
            }

            return Arrays.asList(output.toString()
                    .split(System.lineSeparator()));

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to execute Python script.", e);
        }
    }

    private static String getBatchSizeOptimizationParameter(MitigationStrategy mitigationStrategy) {
        return mitigationStrategy.equals(MitigationStrategy.CLUSTER) ? "Y" : "N";
    }

    private static String getRankerTypeCommandParameter(RankerType rankerType) {
        if (rankerType.equals(RankerType.FAMD)) {
            return "F";
        } else if (rankerType.equals(RankerType.LDA)) {
            return "LDA";
        } else if (rankerType.equals(RankerType.RANDOM_FOREST)) {
            return "RF";
        } else if (rankerType.equals(RankerType.LINEAR_REGRESSION)) {
            return "LR";
        } else if (rankerType.equals(RankerType.LOGISTIC_REGRESSION)) {
            return "LGR";
        } else {
            return "P";
        }
    }

    private static String getAggregationMethodCommandParamter(RankingAggregationMethod aggregationMethod) {
        if (aggregationMethod.equals(RankingAggregationMethod.SUM)) {
            return "L";
        } else if (aggregationMethod.equals(RankingAggregationMethod.EXPONENTIAL_RANKS)) {
            return "E";
        } else {
            return "T";
        }
    }

}
