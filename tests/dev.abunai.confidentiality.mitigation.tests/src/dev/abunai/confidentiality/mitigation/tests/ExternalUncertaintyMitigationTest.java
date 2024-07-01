package dev.abunai.confidentiality.mitigation.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainFlowGraphCollection;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDExternalUncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDExternalUncertaintySource;
import dev.abunai.confidentiality.mitigation.MitigationModelCalculator;
import dev.abunai.confidentiality.mitigation.TrainDataGeneration;
import dev.abunai.confidentiality.mitigation.UncertaintyRanker;
import dev.abunai.confidentiality.mitigation.UncertaintySourceMitigationUtils;
import dev.abunai.confidentiality.mitigation.testBases.MitigationTestBase;

public class ExternalUncertaintyMitigationTest extends MitigationTestBase{
	
	protected String getFolderName() {
		return "DFDExternalUncertainty";
	}

	protected String getFilesName() {
		return "default";
	}
	
	private final String scriptDirectory = "C:\\Users\\Jonas\\Desktop\\Masterarbeit_Paper\\Mitigation\\bundles\\dev.abunai.confidentiality.mitigation\\scripts\\uncertaintyRanking";
	private final String trainDataDirectory = scriptDirectory + "\\train_data_files";
	private final String pathToUncertaintyRankingScript = scriptDirectory+"\\uncertainty_ranking.py";
	
	@Test
	public void createTrainData() {
		List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();
		constraints.add(it -> {
			System.out.println(this.retrieveNodeLabels(it));
			System.out.println(this.retrieveDataLabels(it));
			return this.retrieveNodeLabels(it).contains("nonEU") && this.retrieveDataLabels(it).contains("Personal");
		});
		
		var count = 0;
		for (var constraint : constraints) {
			DFDUncertainFlowGraphCollection flowGraphs = (DFDUncertainFlowGraphCollection) analysis.findFlowGraph();
			DFDUncertainFlowGraphCollection uncertainFlowGraphs = flowGraphs.createUncertainFlows();
			uncertainFlowGraphs.evaluate();

			List<UncertainConstraintViolation> violations = analysis.queryUncertainDataFlow(uncertainFlowGraphs,
					constraint);
			TrainDataGeneration.violationDataToCSV(violations, uncertaintySources,
					trainDataDirectory+"\\violations_"+Integer.toString(count)+".csv");
			count++;
		}
		System.out.println(UncertaintyRanker.rankUncertaintiesBasedOnTrainData(
				pathToUncertaintyRankingScript,
				trainDataDirectory,
				1)
				);
	}
	
	@Test
	public void mitigateAutomatically() {
		List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();
		constraints.add(it -> {
			System.out.println(this.retrieveNodeLabels(it));
			System.out.println(this.retrieveDataLabels(it));
			return this.retrieveNodeLabels(it).contains("nonEU") && this.retrieveDataLabels(it).contains("Personal");
		});
		
		var pathToDfdTestModels = "platform:/plugin/dev.abunai.confidentiality.analysis.testmodels/models/dfd";
		var pathFromTestModelsToMitigationFolder = "models/dfd/mitigation";
		
		var pathToModelsUncertainty = pathToDfdTestModels + "/DFDExternalUncertainty/default.uncertainty";
		var pathToMitigationModel = "C:\\Users\\Jonas\\Desktop\\Masterarbeit_Paper\\UncertaintyAwareConfidentialityAnalysis\\tests\\dev.abunai.confidentiality.analysis.testmodels\\models\\dfd\\mitigation";
		var pathToMitigationModelUncertainty = pathToDfdTestModels +"/mitigation/mitigation.uncertainty";
		

		var result = MitigationModelCalculator.findMitigatingModel(
				new DataFlowDiagramAndDictionary(this.dfd,this.dd),
				uncertaintySources, 
				uncertaintySources,
				constraints,
				pathToModelsUncertainty,
				pathToMitigationModel,
				pathFromTestModelsToMitigationFolder,
				pathToMitigationModelUncertainty);
		System.out.println(result);
	}
	
	@Test
	public void mitigateManually() {
		// Apply mitigating scenario to dd and dfd
		var extUn = (DFDExternalUncertaintySource)this.uncertaintySources.get(0);
		var scenarios = UncertaintyUtils.getUncertaintyScenarios(extUn);
		var result = UncertaintySourceMitigationUtils.chooseExternalScenario(dfd,dd,extUn,(DFDExternalUncertaintyScenario)scenarios.get(0));
		
		// Store result
		new DataFlowDiagramConverter().storeDFD(result , "external");
		
	}
}
