package dev.abunai.confidentiality.mitigation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;

public class TrainDataGenerationBinary {
	
	public static void violationDataToCSV(List<UncertainConstraintViolation> violations,
			List<UncertaintySource> allUncertainties, String outputPath) {

		HashSet<String> fastLookUpTable = new HashSet<>();

		for (var violation : violations) {
			var relevantSources = violation.transposeFlowGraph().getRelevantUncertaintySources();
			
			String lookUpString = "";

			for (int i = 0; i < allUncertainties.size(); i++) {
				
				if(relevantSources.contains(allUncertainties.get(i))) {
					lookUpString += "U";
				}
				else {
					lookUpString += "I";
				}
			}

			fastLookUpTable.add(lookUpString);
		}
		generateTestDataFile(allUncertainties, fastLookUpTable, outputPath);
	}

	private static void generateTestDataFile(List<UncertaintySource> allUncertainties, 
			HashSet<String> fastLookUpTable, String outputPath) {

		String[] elements = { "U", "I" }; 
		int permutationSize = allUncertainties.size();
		List<String[]> permutationsList = generatePermutations(elements, permutationSize);

		// Generate all random permutations
		String[][] permutationsArray = new String[permutationsList.size()][permutationSize];
		permutationsArray = permutationsList.toArray(permutationsArray);

		// Fill in target values
		String[][] trainDataArray = new String[permutationsList.size()][permutationSize + 1];
		for (int i = 0; i < permutationsArray.length; i++) {
			String lookUpString = "";
			for (int j = 0; j < permutationsArray[i].length; j++) {
				trainDataArray[i][j] = permutationsArray[i][j];
				lookUpString += permutationsArray[i][j];
			}
			trainDataArray[i][permutationSize] = fastLookUpTable.contains(lookUpString) ? "True" : "False";
		}
		generateCSVFromData(trainDataArray, allUncertainties, outputPath);
	}

	private static void generateCSVFromData(String[][] dataRows, List<UncertaintySource> allUncertainties, String outputPath) {
		
		// Entity names of uncertainties as header
		String[] header = new String[allUncertainties.size() + 1];
		for (int i = 0; i < allUncertainties.size(); i++) {
			header[i] = allUncertainties.get(i).getEntityName();
		}
		header[allUncertainties.size()] = "Constraint violated";

		// Write file content
		try (FileWriter writer = new FileWriter(outputPath)) {
			writeLine(writer, header);
			for (String[] row : dataRows) {
				writeLine(writer, row);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void writeLine(FileWriter writer, String[] values) throws IOException {
		// ; for german excel
		String line = String.join(";", values);
		writer.write(line);
		writer.write("\n");
	}

	private static List<String[]> generatePermutations(String[] elements, int size) {
		List<String[]> permutationsList = new ArrayList<>();
		generatePermutationsHelper(elements, new String[size], 0, permutationsList);
		return permutationsList;
	}

	private static void generatePermutationsHelper(String[] elements, String[] current, int index,
			List<String[]> permutationsList) {
		
		// Once last element is reached the array will be added to the permutations list
		if (index == current.length) {
			String[] permutation = new String[current.length];
			for (int i = 0; i < current.length; i++) {
				permutation[i] = String.valueOf(current[i]);
			}
			permutationsList.add(permutation);
			return;
		}

		// Set all possible values and move on to the next index
		for (String element : elements) {
			current[index] = element;
			generatePermutationsHelper(elements, current, index + 1, permutationsList);
		}
	}
}
