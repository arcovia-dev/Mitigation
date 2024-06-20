package dev.abunai.confidentiality.mitigation.tests;


import org.dataflowanalysis.converter.*;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyResourceProvider;
import dev.abunai.confidentiality.mitigation.MitigationUtils;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDBehaviorUncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDBehaviorUncertaintySource;
import dev.abunai.confidentiality.analysis.tests.DFDTestBase;


public class BehaviorUncertaintyMitigationTest extends DFDTestBase{

	
	protected String getFolderName() {
		return "DFDBehaviorUncertainty";
	}

	protected String getFilesName() {
		return "default";
	}

	
	@Test
	public void mitigate() {
		
		// Load datadictonary, dataflowdiagram and uncertainties
		var resourceProvider = (DFDUncertaintyResourceProvider)this.analysis.getResourceProvider();
		resourceProvider.loadRequiredResources();
		var dd = resourceProvider.getDataDictionary();
		var dfd = resourceProvider.getDataFlowDiagram();
		var uncertainties = resourceProvider.getUncertaintySourceCollection().getSources();
		
		// Apply mitigating scenario to dd and dfd
		var behUn = (DFDBehaviorUncertaintySource)uncertainties.get(0);
		var scenarios = UncertaintyUtils.getUncertaintyScenarios(behUn);
		var result = MitigationUtils.chooseBehaviorScenario(dfd,dd,behUn,(DFDBehaviorUncertaintyScenario)scenarios.get(0));
		
		// Store result
		new DataFlowDiagramConverter().storeDFD(result , "behaviour");
		
	}
	
}
