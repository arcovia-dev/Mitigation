package dev.abunai.confidentiality.mitigation.tests.ranking;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyResourceProvider;
import dev.abunai.confidentiality.mitigation.ranking.RankerType;
import dev.abunai.confidentiality.mitigation.ranking.RankingAggregationMethod;
import dev.abunai.confidentiality.mitigation.tests.MitigationTestBase;

public class MitigationCalculationTest extends MitigationTestBase{

	@Override
	protected String getFolderName() {
		return "mitigation_example";
	}

	@Override
	protected String getFilesName() {
		return "mitigation_example";
	}
	
	@Override
	protected RankerType getRankerType() {
		return RankerType.RANDOM_FOREST;
	}

	@Override
	protected RankingAggregationMethod getAggregationMethod() {
		return RankingAggregationMethod.EXPONENTIAL_RANKS;
	}

	@Override
	protected List<Predicate<? super AbstractVertex<?>>> getConstraints() {
		List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();
		constraints.add(it -> {
			return this.retrieveNodeLabels(it).contains("nonEU") && this.retrieveDataLabels(it).contains("Personal");
		});
		return constraints;
	}
	
	@Test
	public void testMitigationModel() {
		var analysis = getAnalysis();
		var resourceProvider = (DFDUncertaintyResourceProvider) analysis.getResourceProvider();
		resourceProvider.loadRequiredResources();
		var dd = resourceProvider.getDataDictionary();
		var dfd = resourceProvider.getDataFlowDiagram();
		var ddAndDfd = new DataFlowDiagramAndDictionary(dfd, dd);
		var allEntityNames = analysis.getUncertaintySources().stream().map(u -> u.getEntityName()).toList();
		var mitigationModels =  mitigateWithIncreasingAmountOfUncertainties(allEntityNames, analysis, ddAndDfd);
		
		assertTrue(mitigationModels.size() == 1);
		var bcFlow = mitigationModels.get(0).model().dataFlowDiagram().getFlows().stream()
		.filter(f -> f.getSourceNode().getEntityName().equals("b") 
				&& f.getDestinationNode().getEntityName().equals("c")).findAny();
		var ceFlow = mitigationModels.get(0).model().dataFlowDiagram().getFlows().stream()
				.filter(f -> f.getSourceNode().getEntityName().equals("c") 
						&& f.getDestinationNode().getEntityName().equals("e")).findAny();
		var iNode = mitigationModels.get(0).model().dataFlowDiagram().getNodes().stream()
				.filter(n -> n.getEntityName().equals("i")).findAny();
		var kProp =  mitigationModels.get(0).model().dataFlowDiagram().getNodes().stream()
				.filter(n -> n.getEntityName().equals("k")).findAny().get().getProperties().get(0);
		
		assertTrue(bcFlow.isPresent());
		assertTrue(ceFlow.isPresent());
		assertFalse(iNode.isPresent());
		assertTrue(kProp.getEntityName().equals("EU"));
	}

}
