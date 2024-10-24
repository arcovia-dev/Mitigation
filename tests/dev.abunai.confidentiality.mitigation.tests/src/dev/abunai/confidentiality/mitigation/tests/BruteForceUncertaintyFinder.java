package dev.abunai.confidentiality.mitigation.tests;

import java.util.*;

import dev.abunai.confidentiality.analysis.UncertaintyAwareConfidentialityAnalysis;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainFlowGraphCollection;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainTransposeFlowGraph;

/*
 * Class finding a ordered list 
 * which will be used to determine the order of the brute force mitigation.
 * The order gets determined by the positions of the uncertainties in the TFGs.
 * */
public class BruteForceUncertaintyFinder {
	
	public static List<String> getBruteForceUncertaintyEntityNames(UncertaintyAwareConfidentialityAnalysis analysis){
		List<String> result = new ArrayList<>();
		DFDUncertainFlowGraphCollection flowGraphs = (DFDUncertainFlowGraphCollection) analysis.findFlowGraph();
		DFDUncertainFlowGraphCollection uncertainFlowGraphs = flowGraphs.createUncertainFlows();
		uncertainFlowGraphs.evaluate();
		List<DFDUncertainTransposeFlowGraph> allTFGs = uncertainFlowGraphs.getTransposeFlowGraphs().stream()
				.map(DFDUncertainTransposeFlowGraph.class::cast).toList();
		
		for(var tfg : allTFGs) {
			var newEntityNames = tfg.getRelevantUncertaintySources().stream()
					.map(u -> u.getEntityName())
					.toList();
			result.addAll(newEntityNames);
		}
		
		return result.stream().distinct().toList();
	}

}
