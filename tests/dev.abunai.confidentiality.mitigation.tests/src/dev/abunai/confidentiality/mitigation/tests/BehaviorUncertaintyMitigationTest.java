package dev.abunai.confidentiality.mitigation.tests;


import org.dataflowanalysis.converter.*;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
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
	public void mitigate() {
		// Apply mitigating scenario to dd and dfd
		var behUn = (DFDBehaviorUncertaintySource)this.uncertaintySources.get(0);
		var scenarios = UncertaintyUtils.getUncertaintyScenarios(behUn);
		var result = UncertaintySourceMitigationUtils.chooseBehaviorScenario(this.dfd,this.dd,behUn,(DFDBehaviorUncertaintyScenario)scenarios.get(0));
		
		// Store result
		new DataFlowDiagramConverter().storeDFD(result , "behaviour");
		
	}
	
}
