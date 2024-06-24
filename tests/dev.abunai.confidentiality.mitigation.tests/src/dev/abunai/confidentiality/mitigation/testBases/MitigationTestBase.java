package dev.abunai.confidentiality.mitigation.testBases;

import java.util.List;

import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyResourceProvider;
import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.abunai.confidentiality.analysis.tests.DFDTestBase;

public abstract class MitigationTestBase extends DFDTestBase {

	protected abstract String getFolderName();

	protected abstract String getFilesName();
	
	public DataDictionary dd;
	
	public DataFlowDiagram dfd;
	
	public List<UncertaintySource> uncertaintySources;

	@BeforeEach
	public void before() {
		// Load datadictonary, dataflowdiagram and uncertainties
		var resourceProvider = (DFDUncertaintyResourceProvider) this.analysis.getResourceProvider();
		resourceProvider.loadRequiredResources();
		dd = resourceProvider.getDataDictionary();
		dfd = resourceProvider.getDataFlowDiagram();
		uncertaintySources = resourceProvider.getUncertaintySourceCollection().getSources();
	}

}
