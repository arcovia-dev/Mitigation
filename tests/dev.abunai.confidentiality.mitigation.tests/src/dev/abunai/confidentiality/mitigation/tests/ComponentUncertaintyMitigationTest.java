package dev.abunai.confidentiality.mitigation.tests;

import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyResourceProvider;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDComponentUncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDComponentUncertaintySource;
import dev.abunai.confidentiality.analysis.tests.DFDTestBase;
import dev.abunai.confidentiality.mitigation.UncertaintySourceMitigationUtils;
import dev.abunai.confidentiality.mitigation.testBases.MitigationTestBase;

public class ComponentUncertaintyMitigationTest extends MitigationTestBase{
	protected String getFolderName() {
		return "DFDComponentUncertainty";
	}

	protected String getFilesName() {
		return "default";
	}

	
	@Test
	public void mitigate() {
		
		// Apply mitigating scenario to dd and dfd
		var compUn = (DFDComponentUncertaintySource)this.uncertaintySources.get(0);
		var scenarios = UncertaintyUtils.getUncertaintyScenarios(compUn);
		var result = UncertaintySourceMitigationUtils.chooseComponentScenario(this.dfd,this.dd,compUn,(DFDComponentUncertaintyScenario)scenarios.get(0));
		
		// Store result
		new DataFlowDiagramConverter().storeDFD(result , "component");
		
	}
	
}
