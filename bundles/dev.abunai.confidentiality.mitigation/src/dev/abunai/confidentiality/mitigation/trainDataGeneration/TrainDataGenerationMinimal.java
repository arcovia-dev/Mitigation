package dev.abunai.confidentiality.mitigation.trainDataGeneration;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;

public class TrainDataGenerationMinimal implements ITrainDataGeneration{

	public void violationDataToCSV(List<UncertainConstraintViolation> violations,
			List<UncertaintySource> allUncertainties, String outputPath) {
		
		HashSet<String> fastLookUpTable = new HashSet<>();

		for (var violation : violations) {
			var relevantSources = violation.transposeFlowGraph().getRelevantUncertaintySources();
			var sourceToScenario = violation.uncertainState().getSourceToScenarioMapping();

			String lookUpString = "";

			for (int i = 0; i < allUncertainties.size(); i++) {
				var src = allUncertainties.get(i);
				var scenario = sourceToScenario.containsKey(src) ? sourceToScenario.get(src) : null;
				
				if (relevantSources.contains(src)) {
					if (scenario != null && !UncertaintyUtils.isDefaultScenario(src, scenario)) {
						lookUpString += "A";
					}
					else {
						lookUpString += "D";
					}
				} else {
					lookUpString += "I";
				}
			}

			fastLookUpTable.add(lookUpString);
		}
		generateTestDataFile(allUncertainties, fastLookUpTable, outputPath);
	}

	private void generateTestDataFile(List<UncertaintySource> allUncertainties, HashSet<String> fastLookUpTable,
			String outputPath) {

		String[] elements = { "A", "D","I" };
		int permutationSize = allUncertainties.size();
		int permutationChanceInverse = 4000;
		System.out.println(fastLookUpTable);
		List<String[]> permutationsList = generatePermutations(elements, permutationSize, fastLookUpTable,permutationChanceInverse);

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

	private void generateCSVFromData(String[][] dataRows, List<UncertaintySource> allUncertainties,
			String outputPath) {

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

	private List<String[]> generatePermutations(String[] elements, int size, HashSet<String> fastLookUpTable, int permutationChanceInverse) {
		List<String[]> permutationsList = new ArrayList<>();
		generatePermutationsHelper(elements, new String[size], 0, permutationsList, fastLookUpTable, permutationChanceInverse);
		return permutationsList;
	}

	private void generatePermutationsHelper(String[] elements, String[] current, int index,
			List<String[]> permutationsList, HashSet<String> fastLookUpTable, int permutationChanceInverse) {

		// Once last element is reached the array will be added to the permutations list
		if (index == current.length) {
			String[] permutation = new String[current.length];

			Random rand = new Random();
			for (int i = 0; i < current.length; i++) {
				permutation[i] = String.valueOf(current[i]);
			}
			if (fastLookUpTable.contains(Arrays
				    .stream(permutation)
				    .collect(Collectors.joining())) || rand.nextInt(permutationChanceInverse) == 1) {
				permutationsList.add(permutation);
			}
			return;
		}

		// Set all possible values and move on to the next index
		for (String element : elements) {
			current[index] = element;
			generatePermutationsHelper(elements, current, index + 1, permutationsList, fastLookUpTable,  permutationChanceInverse);
		}
	}
	
}
