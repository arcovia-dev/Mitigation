package dev.abunai.confidentiality.mitigation;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.DataFlowDiagramConverter;

import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyAwareConfidentialityAnalysis;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyAwareConfidentialityAnalysisBuilder;
import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDBehaviorUncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDBehaviorUncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDComponentUncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDComponentUncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDExternalUncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDExternalUncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDInterfaceUncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDInterfaceUncertaintySource;
import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainFlowGraphCollection;
import dev.abunai.confidentiality.analysis.testmodels.Activator;

public class MitigationModelCalculator {

	public static List<String> findMitigatingModel(
			DataFlowDiagramAndDictionary diagramAndDict,
			List<UncertaintySource> uncertaintySources,
			List<UncertaintySource> relevantUncertainties,
			List<Predicate<? super AbstractVertex<?>>> constraintFunctions,
			String uncertaintyFilePath,
			String mitigationsPath,
			String mitigationsPathFromProject,
			String mitigationUncertaintyPath) 
	{
		
		List<DataFlowDiagramAndDictionary> mitigationCandidates = new ArrayList<>();
		createMitigationCandidates(0, relevantUncertainties, diagramAndDict, mitigationCandidates);

		List<UncertaintySource> irrelevantUncertainties = uncertaintySources.stream()
				.filter(s -> !(relevantUncertainties.contains(s))).toList();
		storeMitigationCandidates(mitigationCandidates, uncertaintyFilePath, irrelevantUncertainties, mitigationsPath, mitigationUncertaintyPath);
		
		return getValidMitigationModels(mitigationsPathFromProject,mitigationCandidates.size(), constraintFunctions);
	}
	
	private static List<String> getValidMitigationModels(String outputPath, int modelsAmount, List<Predicate<? super AbstractVertex<?>>> constraintFunctions) {
		List<String> result = new ArrayList<>();
		for (int i = 0; i < modelsAmount; i++) {
			final var dataFlowDiagramPath = Paths
					.get(outputPath, "mitigation" + Integer.toString(i) + ".dataflowdiagram").toString();
			final var dataDictionaryPath = Paths.get(outputPath, "mitigation" + Integer.toString(i) + ".datadictionary")
					.toString();
			final var uncertaintyPath = Paths.get(outputPath, "mitigation.uncertainty")
					.toString();
			var builder = new DFDUncertaintyAwareConfidentialityAnalysisBuilder().standalone()
					.modelProjectName("dev.abunai.confidentiality.analysis.testmodels")
					.usePluginActivator(Activator.class).useDataDictionary(dataDictionaryPath)
					.useDataFlowDiagram(dataFlowDiagramPath).useUncertaintyModel(uncertaintyPath);

			DFDUncertaintyAwareConfidentialityAnalysis analysis = builder.build();
			analysis.initializeAnalysis();

			DFDUncertainFlowGraphCollection flowGraphs = (DFDUncertainFlowGraphCollection) analysis.findFlowGraph();
			DFDUncertainFlowGraphCollection uncertainFlowGraphs = flowGraphs.createUncertainFlows();
			uncertainFlowGraphs.evaluate();
			
			boolean noConstraintViolated = true;
			for(var constraint: constraintFunctions) {
				List<UncertainConstraintViolation> violations = analysis.queryUncertainDataFlow(uncertainFlowGraphs,constraint);
				if(violations.size() > 0) {
					noConstraintViolated = false;
					break;
				}
			}
			
			if(noConstraintViolated) {
				result.add(outputPath+"/mitigation" + Integer.toString(i));
			}
		}

		return result;
	}

	

	private static void storeMitigationCandidates(List<DataFlowDiagramAndDictionary> candidates, String uncertaintyFilePath,
			List<UncertaintySource> uncertaintiesToKeep, String outputPath, String mitigationUncertaintyPath) {
		System.out.println(mitigationUncertaintyPath);
		// Store uncertainties
		ResourceSet resSet = new ResourceSetImpl();
		Resource oldUncertaintyRes = resSet.getResource(URI.createURI(uncertaintyFilePath), true);
		Resource newUncertaintyRes = resSet.createResource(URI.createURI(mitigationUncertaintyPath));
		List<String> idsToKeep = uncertaintiesToKeep.stream().map(u -> u.getId()).toList();

		var sourceCollection = oldUncertaintyRes.getContents().get(0);
		newUncertaintyRes.getContents().add(sourceCollection);

		for (EObject eObject : sourceCollection.eContents()) {
			var id = EcoreUtil.getID(eObject);
			if (!idsToKeep.contains(id)) {
				EcoreUtil.delete(eObject);
			}
		}
		
		try {
			newUncertaintyRes.save(Collections.EMPTY_MAP);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Store dataflowdigrams and datadictionarys
		var conv = new DataFlowDiagramConverter();
		for (int i = 0; i < candidates.size(); i++) {
			conv.storeDFD(candidates.get(i), outputPath + "/mitigation" + Integer.toString(i));
		}

	}

	private static void createMitigationCandidates(int index, List<UncertaintySource> relevantUncertainties,
			DataFlowDiagramAndDictionary diagramAndDict, List<DataFlowDiagramAndDictionary> candidates) {
		// Store possible mitigation in mitigations list when all uncertainties got considered
		if (index == relevantUncertainties.size()) {
			candidates.add(diagramAndDict);
			return;
		}
		// Consider all options for uncertainty source actSource
		else {
			var actSource = relevantUncertainties.get(index);

			// chose default scenario
			createMitigationCandidates(index + 1, relevantUncertainties, diagramAndDict, candidates);

			for (var scenario : UncertaintyUtils.getUncertaintyScenarios(actSource)) {
				if (UncertaintyUtils.isDefaultScenario(actSource, scenario)) {
					break;
				} else {
					// choose alternative scenario
					if (scenario instanceof DFDExternalUncertaintyScenario castedScenario) {
						var newDiagramAndDict = UncertaintySourceMitigationUtils.chooseExternalScenario(
								diagramAndDict.dataFlowDiagram(), diagramAndDict.dataDictionary(),
								(DFDExternalUncertaintySource) actSource, castedScenario);
						createMitigationCandidates(index + 1, relevantUncertainties, newDiagramAndDict, candidates);
					} else if (scenario instanceof DFDBehaviorUncertaintyScenario castedScenario) {
						var newDiagramAndDict = UncertaintySourceMitigationUtils.chooseBehaviorScenario(
								diagramAndDict.dataFlowDiagram(), diagramAndDict.dataDictionary(),
								(DFDBehaviorUncertaintySource) actSource, castedScenario);
						createMitigationCandidates(index + 1, relevantUncertainties, newDiagramAndDict, candidates);
					} else if (scenario instanceof DFDInterfaceUncertaintyScenario castedScenario) {
						var newDiagramAndDict = UncertaintySourceMitigationUtils.chooseInterfaceScenario(
								diagramAndDict.dataFlowDiagram(), diagramAndDict.dataDictionary(),
								(DFDInterfaceUncertaintySource) actSource, castedScenario);
						createMitigationCandidates(index + 1, relevantUncertainties, newDiagramAndDict, candidates);
					} else if (scenario instanceof DFDComponentUncertaintyScenario castedScenario) {
						var newDiagramAndDict = UncertaintySourceMitigationUtils.chooseComponentScenario(
								diagramAndDict.dataFlowDiagram(), diagramAndDict.dataDictionary(),
								(DFDComponentUncertaintySource) actSource, castedScenario);
						createMitigationCandidates(index + 1, relevantUncertainties, newDiagramAndDict, candidates);
					} else {
						throw new IllegalArgumentException("Unexpected DFD uncertainty scenario: %s"
								.formatted(UncertaintyUtils.getUncertaintyScenarioName(scenario)));
					}
				}
			}
		}
	}
	
}
