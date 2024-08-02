package dev.abunai.confidentiality.mitigation.ranking;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;

public class TrainDataGeneration {

	public void violationDataToCSV(List<UncertainConstraintViolation> violations,
			List<UncertaintySource> allUncertainties, String outputPath) {

		HashSet<String> fastLookUpTable = new HashSet<>();
		HashMap<String, HashSet<String>> sourceIdToScenarioIds = new HashMap<>();

		for (var violation : violations) {
			var relevantSources = violation.transposeFlowGraph().getRelevantUncertaintySources();
			var sourceToScenario = violation.uncertainState().getSourceToScenarioMapping();

			String lookUpString = "";

			for (int i = 0; i < allUncertainties.size(); i++) {
				var src = allUncertainties.get(i);
				var scenario = sourceToScenario.containsKey(src) ? sourceToScenario.get(src) : null;
				var scenarioId = scenario == null ? "" : scenario.getId().replace('_','a');

				if (relevantSources.contains(src)) {
					if (scenario != null && !UncertaintyUtils.isDefaultScenario(src, scenario)) {
						lookUpString += "A" + scenarioId;
						if (sourceIdToScenarioIds.containsKey(src.getId())) {
							sourceIdToScenarioIds.get(src.getId()).add(scenarioId);
						} else {
							var newScenarioIdList = new HashSet<String>();
							newScenarioIdList.add(scenarioId);
							sourceIdToScenarioIds.put(src.getId(), newScenarioIdList);
						}
					} else {
						lookUpString += "D";
					}
				} else {
					lookUpString += "I";
				}
			}

			fastLookUpTable.add(lookUpString);
		}
		generateTestDataFile(allUncertainties, fastLookUpTable, outputPath, sourceIdToScenarioIds);
	}

	private void generateTestDataFile(List<UncertaintySource> allUncertainties, HashSet<String> fastLookUpTable,
			String outputPath, HashMap<String, HashSet<String>> sourceIdToScenarioIds) {

		String[] elements = { "D", "I" };
		int permutationSize = allUncertainties.size();
		System.out.println(fastLookUpTable);
		List<String[]> permutationsList = generatePermutations(elements, permutationSize, fastLookUpTable,
				allUncertainties, sourceIdToScenarioIds);

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

	private void generateCSVFromData(String[][] dataRows, List<UncertaintySource> allUncertainties, String outputPath) {

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

	private List<String[]> generatePermutations(String[] elements, int size, HashSet<String> fastLookUpTable, List<UncertaintySource> allUncertainties, HashMap<String, HashSet<String>> sourceIdToScenarioIds) {
		List<String[]> permutationsList = new ArrayList<>();
		generatePermutationsHelper(elements, new String[size], 0, permutationsList, fastLookUpTable, allUncertainties, sourceIdToScenarioIds);
		return permutationsList;
	}

	private void generatePermutationsHelper(String[] elements, String[] current, int index,
			List<String[]> permutationsList, HashSet<String> fastLookUpTable, List<UncertaintySource> allUncertainties, HashMap<String, HashSet<String>> sourceIdToScenarioIds) {

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
			generatePermutationsHelper(elements, current, index + 1, permutationsList, fastLookUpTable, allUncertainties, sourceIdToScenarioIds);
		}
		
		var currentUncertaintyId = allUncertainties.get(index).getId();
		if(sourceIdToScenarioIds.containsKey(currentUncertaintyId)){
			for (var scenarioId: sourceIdToScenarioIds.get(currentUncertaintyId)) {
				current[index] = "A"+scenarioId;
				generatePermutationsHelper(elements, current, index + 1, permutationsList, fastLookUpTable, allUncertainties, sourceIdToScenarioIds);
			}
		}
	}

}
