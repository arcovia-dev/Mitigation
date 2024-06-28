package dev.abunai.confidentiality.mitigation.tests;


import java.util.*;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.converter.*;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainFlowGraphCollection;
import dev.abunai.confidentiality.mitigation.MitigationModelCalculator;
import dev.abunai.confidentiality.mitigation.TrainDataGeneration;
import dev.abunai.confidentiality.mitigation.UncertaintySourceMitigationUtils;
import dev.abunai.confidentiality.mitigation.testBases.MitigationTestBase;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDBehaviorUncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDBehaviorUncertaintySource;


public class BehaviorUncertaintyMitigationTest extends MitigationTestBase{

	
	protected String getFolderName() {
		return "DFDBehaviorUncertainty";
	}

	protected String getFilesName() {
		return "default";
	}
	
	@Test
	public void mitigateAutomatically() {
		var pathToDfdTestModels = "platform:/plugin/dev.abunai.confidentiality.analysis.testmodels/models/dfd";
		var pathFromTestModelsToMitigationFolder = "models/dfd/mitigation";
		
		var pathToModelsUncertainty = pathToDfdTestModels + "/DFDBehaviorUncertainty/default.uncertainty";
		var pathToMitigationModel = "C:\\Users\\Jonas\\Desktop\\Masterarbeit_Paper\\UncertaintyAwareConfidentialityAnalysis\\tests\\dev.abunai.confidentiality.analysis.testmodels\\models\\dfd\\mitigation";
		var pathToMitigationModelUncertainty = pathToDfdTestModels +"/mitigation/mitigation.uncertainty";
		
		List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();
		constraints.add(it -> {
			System.out.println(this.retrieveNodeLabels(it));
			System.out.println(this.retrieveDataLabels(it));
			return this.retrieveNodeLabels(it).contains("nonEU") && this.retrieveDataLabels(it).contains("Unencrypted");
		});
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
	public void mitigateWithScenario() {
		// Apply mitigating scenario to dd and dfd
		var behUn = (DFDBehaviorUncertaintySource)this.uncertaintySources.get(0);
		var scenarios = UncertaintyUtils.getUncertaintyScenarios(behUn);
		var result = UncertaintySourceMitigationUtils.chooseBehaviorScenario(this.dfd,this.dd,behUn,(DFDBehaviorUncertaintyScenario)scenarios.get(0));
		
		// Store result
		new DataFlowDiagramConverter().storeDFD(result , "behaviour");
		
	}
	
}
