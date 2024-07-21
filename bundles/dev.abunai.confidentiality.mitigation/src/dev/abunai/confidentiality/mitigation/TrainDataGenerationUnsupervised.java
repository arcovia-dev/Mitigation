package dev.abunai.confidentiality.mitigation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;

public class TrainDataGenerationUnsupervised {
	
	public void violationDataToCSV(List<UncertainConstraintViolation> violations,
			List<UncertaintySource> allUncertainties, String outputPath) {

		HashSet<List<String>> fastLookUpTable = new HashSet<>();
		HashMap<String, HashSet<String>> sourceIdToScenarioIds = new HashMap<>();

		for (var violation : violations) {
			var relevantSources = violation.transposeFlowGraph().getRelevantUncertaintySources();
			var sourceToScenario = violation.uncertainState().getSourceToScenarioMapping();

			List<String> lookUpList = new ArrayList<>();

			for (int i = 0; i < allUncertainties.size(); i++) {
				var src = allUncertainties.get(i);
				var scenario = sourceToScenario.containsKey(src) ? sourceToScenario.get(src) : null;
				var scenarioId = scenario == null ? "" : scenario.getId().replace('_','a');

				if (relevantSources.contains(src)) {
					if (scenario != null && !UncertaintyUtils.isDefaultScenario(src, scenario)) {
						lookUpList.add("A" + scenarioId);
						if (sourceIdToScenarioIds.containsKey(src.getId())) {
							sourceIdToScenarioIds.get(src.getId()).add(scenarioId);
						} else {
							var newScenarioIdList = new HashSet<String>();
							newScenarioIdList.add(scenarioId);
							sourceIdToScenarioIds.put(src.getId(), newScenarioIdList);
						}
					} else {
						lookUpList.add("D");
					}
				} else {
					lookUpList.add("I");
				}
			}

			fastLookUpTable.add(lookUpList);
		}
		generateTestDataFile(allUncertainties, fastLookUpTable, outputPath, sourceIdToScenarioIds);
	}

	private void generateTestDataFile(List<UncertaintySource> allUncertainties, HashSet<List<String>> fastLookUpTable,
			String outputPath, HashMap<String, HashSet<String>> sourceIdToScenarioIds) {

		int columnsCount = allUncertainties.size();
		int rowsCount = fastLookUpTable.size();
		var it = fastLookUpTable.iterator();
		// Fill in target values
		String[][] trainDataArray = new String[rowsCount][columnsCount+1];
		for (int i = 0; i < rowsCount; i++) {
			var row = it.next();
			for(int j = 0; j < columnsCount;j++) {
				trainDataArray[i][j] = row.get(j);
			}
			trainDataArray[i][columnsCount] = "True";
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

}
