package dev.abunai.confidentiality.mitigation.tests;

import java.util.List;

import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.pcm.PCMUncertainFlowGraphCollection;
import dev.abunai.confidentiality.analysis.tests.PCMTestBase;

import static dev.abunai.confidentiality.mitigation.TrainDataGeneration.violationDataToCSV;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class CoronaWarnAppTest extends PCMTestBase {

		@Override
		protected String getFolderName() {
			return "CoronaWarnApp";
		}

		@Override
		protected String getFilesName() {
			return "default";
		}

		@Override
		protected String getBaseFolder() {
			return "casestudies";
		}

		@Test
		void testCWA() {
			PCMUncertainFlowGraphCollection flowGraphs = (PCMUncertainFlowGraphCollection) analysis.findFlowGraph();
			PCMUncertainFlowGraphCollection uncertainFlowGraphs = flowGraphs.createUncertainFlows();
			assertTrue(flowGraphs.getTransposeFlowGraphs().size() < uncertainFlowGraphs.getTransposeFlowGraphs().size());

			uncertainFlowGraphs.evaluate();

			var allUncertainties = analysis.getUncertaintySources();

			// Constraint 1
			List<UncertainConstraintViolation> illegalLocations = analysis.queryUncertainDataFlow(uncertainFlowGraphs,
					it -> {
						return it.getVertexCharacteristicNames("Location").contains("IllegalLocation");
					});
			violationDataToCSV(illegalLocations, allUncertainties, "cwa_illegalLocations.csv");
			assertTrue(illegalLocations.size() > 0);

			// Constraint 2
			List<UncertainConstraintViolation> leaks = analysis.queryUncertainDataFlow(uncertainFlowGraphs, it -> {
				return it.getDataCharacteristicNamesMap("Status").values().stream().flatMap(List::stream)
						.anyMatch(cv -> cv.equals("Leaked"));
			});
			violationDataToCSV(leaks, allUncertainties, "cwa_leaks.csv");
			assertTrue(leaks.size() > 0);
		}

}
