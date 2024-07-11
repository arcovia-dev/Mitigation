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
	
	protected final String scriptDirectory = "C:\\Users\\Jonas\\Desktop\\Masterarbeit_Paper\\Mitigation\\bundles\\dev.abunai.confidentiality.mitigation\\scripts\\uncertaintyRanking";
	protected final String trainDataDirectory = scriptDirectory + "\\train_data_files";
	protected final String pathToUncertaintyRankingScript = scriptDirectory + "\\uncertainty_ranking.py";
	protected final String pathToRelevantUncertainties = "C:/Users/Jonas/Desktop/Masterarbeit_Paper/Mitigation/bundles/dev.abunai.confidentiality.mitigation/relevantUncertainties.txt";


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
