package dev.arcovia.mitigation.ranking;

import java.util.Arrays;
import java.util.List;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class UncertaintyRanker {

    public static List<String> rankUncertaintiesBasedOnTrainData(String pythonPath, String scriptPath, String pathToTrainDataFolder,
            int rankingLength, RankerType rankerType, RankingAggregationMethod aggregationMethod, MitigationStrategy mitigationStrategy) {
        // Command to run the Python script
        String[] command = {pythonPath, scriptPath, pathToTrainDataFolder, Integer.toString(rankingLength), getRankerTypeCommandParameter(rankerType),
                getAggregationMethodCommandParamter(aggregationMethod), getBatchSizeOptimizationParameter(mitigationStrategy)};
        try {
            // Execute the command
            var startTime = System.currentTimeMillis();
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

            System.out.println(System.currentTimeMillis() - startTime);

            // Get the output as a String
            String result = output.toString();

            /*
             * reader = new BufferedReader(new InputStreamReader(process.getErrorStream())); output = new StringBuilder(); while
             * ((line = reader.readLine()) != null) { output.append(line).append(System.lineSeparator()); }
             * System.out.println(output.toString());
             */

            return Arrays.asList(result.split(System.lineSeparator()));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
