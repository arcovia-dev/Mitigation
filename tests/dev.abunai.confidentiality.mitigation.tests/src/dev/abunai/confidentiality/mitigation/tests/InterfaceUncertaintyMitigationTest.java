package dev.abunai.confidentiality.mitigation.tests;

import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDInterfaceUncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDInterfaceUncertaintySource;
import dev.abunai.confidentiality.mitigation.UncertaintySourceMitigationUtils;
import dev.abunai.confidentiality.mitigation.testBases.MitigationTestBase;

public class InterfaceUncertaintyMitigationTest extends MitigationTestBase{
	
	protected String getFolderName() {
		return "DFDInterfaceUncertainty";
	}

	protected String getFilesName() {
		return "default";
	}
	
	@Test
	public void mitigate() {
		
		// Apply mitigating scenario to dd and dfd
		var intUn = (DFDInterfaceUncertaintySource)this.uncertaintySources.get(0);
		var scenarios = UncertaintyUtils.getUncertaintyScenarios(intUn);
		var result = UncertaintySourceMitigationUtils.chooseInterfaceScenario(this.dfd,this.dd,intUn,(DFDInterfaceUncertaintyScenario)scenarios.get(0));
		
		// Store result
		new DataFlowDiagramConverter().storeDFD(result , "interface");
		
	}
}
