package dev.abunai.confidentiality.mitigation;

import java.util.Arrays;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class UncertaintyRanker {

	public static List<String> rankUncertaintiesBasedOnTrainData(String scriptPath,String pathToTrainDataFolder,int rankingLength){
        // Command to run the Python script
        String command = "python " + scriptPath + " " +pathToTrainDataFolder+" "+Integer.toString(rankingLength);
System.out.println(command);
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

            // Wait for the process to complete and get the exit code
            int exitCode = process.waitFor();
            System.out.println("Python script exited with code " + exitCode);

            // Get the output as a String
            String result = output.toString();
            System.out.println("Output from Python script:");
            System.out.println(result);
            result = result.substring(0, result.length() - 1);
            result = result.replaceAll("[\n\r]", "");
            return Arrays.asList(result.split(","));

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
		return null;
	}

}
