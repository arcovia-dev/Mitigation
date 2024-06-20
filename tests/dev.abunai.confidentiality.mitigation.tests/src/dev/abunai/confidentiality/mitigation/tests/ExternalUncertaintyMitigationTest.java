package dev.abunai.confidentiality.mitigation.tests;

import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyResourceProvider;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDExternalUncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDExternalUncertaintySource;
import dev.abunai.confidentiality.analysis.tests.DFDTestBase;
import dev.abunai.confidentiality.mitigation.MitigationUtils;

public class ExternalUncertaintyMitigationTest extends DFDTestBase{
	protected String getFolderName() {
		return "DFDExternalUncertainty";
	}

	protected String getFilesName() {
		return "default";
	}
	
	@Test
	public void mitigate() {
		
		// Load datadictionary, dataflowdiagram and uncertainties
		var resourceProvider = (DFDUncertaintyResourceProvider)this.analysis.getResourceProvider();
		resourceProvider.loadRequiredResources();
		var dd = resourceProvider.getDataDictionary();
		var dfd = resourceProvider.getDataFlowDiagram();
		var uncertainties = resourceProvider.getUncertaintySourceCollection().getSources();
		
		// Apply mitigating scenario to dd and dfd
		var extUn = (DFDExternalUncertaintySource)uncertainties.get(0);
		var scenarios = UncertaintyUtils.getUncertaintyScenarios(extUn);
		var result = MitigationUtils.chooseExternalScenario(dfd,dd,extUn,(DFDExternalUncertaintyScenario)scenarios.get(0));
		
		// Store result
		new DataFlowDiagramConverter().storeDFD(result , "external");
		
	}
}
