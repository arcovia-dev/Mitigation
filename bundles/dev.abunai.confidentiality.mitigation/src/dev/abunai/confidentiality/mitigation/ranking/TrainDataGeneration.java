package dev.abunai.confidentiality.mitigation.ranking;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainTransposeFlowGraph;
import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;

public class TrainDataGeneration {

	public void violationDataToCSV(List<UncertainConstraintViolation> violations,
			List<DFDUncertainTransposeFlowGraph> tfgs, List<UncertaintySource> allUncertainties, String outputPath) {
		var violationsLookUpTable = getLookUpTableForTGFs(violations.stream().map(v -> (DFDUncertainTransposeFlowGraph)v.transposeFlowGraph()).toList(),allUncertainties);
		var nonViolationsLookUpTable = getLookUpTableForTGFs(tfgs, allUncertainties);
		generateTestDataFile(allUncertainties, violationsLookUpTable, nonViolationsLookUpTable, outputPath);
	}

	private HashSet<List<String>> getLookUpTableForTGFs(List<DFDUncertainTransposeFlowGraph> tfgs,
			List<UncertaintySource> allUncertainties) {
		HashSet<List<String>> fastLookUpTable = new HashSet<>();
		HashMap<String, HashSet<String>> sourceIdToScenarioIds = new HashMap<>();

		for (var tfg : tfgs) {
			var relevantSources = tfg.getRelevantUncertaintySources();
			var sourceToScenario = tfg.getUncertainState().getSourceToScenarioMapping();

			List<String> lookUpList = new ArrayList<>();

			for (int i = 0; i < allUncertainties.size(); i++) {
				var src = allUncertainties.get(i);
				var scenario = sourceToScenario.containsKey(src) ? sourceToScenario.get(src) : null;
				var scenarioId = scenario == null ? "" : scenario.getId().replace('_', 'a');

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

		return fastLookUpTable;
	}

	private void generateTestDataFile(List<UncertaintySource> allUncertainties, HashSet<List<String>> violationsLookUpTable, 
			HashSet<List<String>> nonViolatonsLookUpTable,
			String outputPath) {
		nonViolatonsLookUpTable.removeAll(violationsLookUpTable);
		int columnsCount = allUncertainties.size();
		int rowsCount = violationsLookUpTable.size() + nonViolatonsLookUpTable.size();
		var it = violationsLookUpTable.iterator();
		// Fill in target values
		String[][] trainDataArray = new String[rowsCount][columnsCount + 1];
		for (int i = 0; i < violationsLookUpTable.size(); i++) {
			var row = it.next();
			for (int j = 0; j < columnsCount; j++) {
				trainDataArray[i][j] = row.get(j);
			}
			trainDataArray[i][columnsCount] = "True";
		}
		it = nonViolatonsLookUpTable.iterator();
		for (int i = violationsLookUpTable.size(); i < rowsCount; i++) {
			var row = it.next();
			for (int j = 0; j < columnsCount; j++) {
				trainDataArray[i][j] = row.get(j);
			}
			trainDataArray[i][columnsCount] = "False";
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
