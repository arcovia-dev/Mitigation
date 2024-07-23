package dev.abunai.confidentiality.mitigation;

import java.util.Arrays;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class UncertaintyRanker {

	private final static RankerType rankerType = RankerType.PCA;

	public static List<String> rankUncertaintiesBasedOnTrainData(String scriptPath, String pathToTrainDataFolder,
			int rankingLength) {
		// Command to run the Python script
		String command = "python3 " + scriptPath + " " + pathToTrainDataFolder + " " + Integer.toString(rankingLength)
				+ " " + getRankerTypeCommandParameter();
		try {
			// Execute the command
			Process process = Runtime.getRuntime().exec(command);

			// Read the output from the process
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder output = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				output.append(line).append(System.lineSeparator());
			}

			// Get the output as a String
			String result = output.toString();

			return Arrays.asList(result.split(System.lineSeparator()));

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String getRankerTypeCommandParameter() {
		if (rankerType.equals(RankerType.FAMD)) {
			return "F";
		}
		if (rankerType.equals(RankerType.LDA)) {
			return "L";
		} else {
			return "P";
		}
	}

}
