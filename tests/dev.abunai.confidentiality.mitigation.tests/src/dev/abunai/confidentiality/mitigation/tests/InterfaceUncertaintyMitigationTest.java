package dev.abunai.confidentiality.mitigation.tests;

import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyResourceProvider;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDInterfaceUncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDInterfaceUncertaintySource;
import dev.abunai.confidentiality.analysis.tests.DFDTestBase;
import dev.abunai.confidentiality.mitigation.MitigationUtils;

public class InterfaceUncertaintyMitigationTest extends DFDTestBase{
	
	protected String getFolderName() {
		return "DFDInterfaceUncertainty";
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
		var intUn = (DFDInterfaceUncertaintySource)uncertainties.get(0);
		var scenarios = UncertaintyUtils.getUncertaintyScenarios(intUn);
		var result = MitigationUtils.chooseInterfaceScenario(dfd,dd,intUn,(DFDInterfaceUncertaintyScenario)scenarios.get(0));
		
		// Store result
		DataFlowDiagramConverter conv = new DataFlowDiagramConverter();
		conv.storeDFD(result , "interface");
		
	}
}
