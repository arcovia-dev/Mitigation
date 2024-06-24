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

	public static List<String> findMitigatingModel(DataFlowDiagramAndDictionary diagramAndDict,
			List<UncertaintySource> uncertaintySources, List<UncertaintySource> relevantUncertainties,
			List<Predicate<? super AbstractVertex<?>>> constraintFunctions, String uncertaintyFilePath,
			String outputPath) {

		List<DataFlowDiagramAndDictionary> mitigations = new ArrayList<>();
		createNewMitigation(0, relevantUncertainties, diagramAndDict, mitigations);

		List<UncertaintySource> irrelevantUncertainties = uncertaintySources.stream()
				.filter(s -> !(relevantUncertainties.contains(s))).toList();
		storeNewMitigations(mitigations, uncertaintyFilePath, irrelevantUncertainties, outputPath);
		
		return getValidMitigationModels(outputPath,mitigations.size(), constraintFunctions);
	}
	
	private static List<String> getValidMitigationModels(String outputPath, int modelsAmount, List<Predicate<? super AbstractVertex<?>>> constraintFunctions) {
		List<String> result = new ArrayList<>();
		for (int i = 0; i < modelsAmount; i++) {
			final var dataFlowDiagramPath = Paths
					.get(outputPath, "mitigation" + Integer.toString(i) + ".dataflowdiagram").toString();
			final var dataDictionaryPath = Paths.get(outputPath, "mitigation" + Integer.toString(i) + ".datadictionary")
					.toString();
			final var uncertaintyPath = Paths.get(outputPath, "mitigation" + Integer.toString(i) + ".uncertainty")
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
				result.add(outputPath+"\\mitigation" + Integer.toString(i));
			}
		}

		return result;
	}

	

	private static void storeNewMitigations(List<DataFlowDiagramAndDictionary> mitigations, String uncertaintyFilePath,
			List<UncertaintySource> uncertaintiesToKeep, String outputPath) {

		// Store uncertainties
		ResourceSet resSet = new ResourceSetImpl();
		Resource oldUncertaintyRes = resSet.getResource(URI.createURI(uncertaintyFilePath), true);
		Resource newUncertaintyRes = resSet.createResource(URI.createURI(outputPath + "/mitigation.uncertainty"));
		List<String> idsToKeep = uncertaintiesToKeep.stream().map(u -> u.getId()).toList();

		for (EObject eObject : oldUncertaintyRes.getContents()) {
			var id = EcoreUtil.getID(eObject);
			if (idsToKeep.contains(id)) {
				newUncertaintyRes.getContents().add(eObject);
			}
		}
		try {
			newUncertaintyRes.save(Collections.EMPTY_MAP);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Store dataflowdigrams and datadictionarys
		var conv = new DataFlowDiagramConverter();
		for (int i = 0; i < mitigations.size(); i++) {
			conv.storeDFD(mitigations.get(i), outputPath + "/mitigation" + Integer.toString(i));
		}

	}

	private static void createNewMitigation(int index, List<UncertaintySource> relevantUncertainties,
			DataFlowDiagramAndDictionary diagramAndDict, List<DataFlowDiagramAndDictionary> mitigations) {
		// Store possible mitigation in mitigations list when all uncertainties got
		// considered
		if (index == relevantUncertainties.size()) {
			mitigations.add(diagramAndDict);
			return;
		}
		// Consider all options for uncertainty source actSource
		else {
			var actSource = relevantUncertainties.get(index);

			// chose default scenario
			createNewMitigation(index + 1, relevantUncertainties, diagramAndDict, mitigations);

			for (var scenario : UncertaintyUtils.getUncertaintyScenarios(actSource)) {
				if (UncertaintyUtils.isDefaultScenario(actSource, scenario)) {
					break;
				} else {
					// choose alternative scenario
					if (scenario instanceof DFDExternalUncertaintyScenario castedScenario) {
						var newDiagramAndDict = UncertaintySourceMitigationUtils.chooseExternalScenario(
								diagramAndDict.dataFlowDiagram(), diagramAndDict.dataDictionary(),
								(DFDExternalUncertaintySource) actSource, castedScenario);
						createNewMitigation(index + 1, relevantUncertainties, newDiagramAndDict, mitigations);
					} else if (scenario instanceof DFDBehaviorUncertaintyScenario castedScenario) {
						var newDiagramAndDict = UncertaintySourceMitigationUtils.chooseBehaviorScenario(
								diagramAndDict.dataFlowDiagram(), diagramAndDict.dataDictionary(),
								(DFDBehaviorUncertaintySource) actSource, castedScenario);
						createNewMitigation(index + 1, relevantUncertainties, newDiagramAndDict, mitigations);
					} else if (scenario instanceof DFDInterfaceUncertaintyScenario castedScenario) {
						var newDiagramAndDict = UncertaintySourceMitigationUtils.chooseInterfaceScenario(
								diagramAndDict.dataFlowDiagram(), diagramAndDict.dataDictionary(),
								(DFDInterfaceUncertaintySource) actSource, castedScenario);
						createNewMitigation(index + 1, relevantUncertainties, newDiagramAndDict, mitigations);
					} else if (scenario instanceof DFDComponentUncertaintyScenario castedScenario) {
						var newDiagramAndDict = UncertaintySourceMitigationUtils.chooseComponentScenario(
								diagramAndDict.dataFlowDiagram(), diagramAndDict.dataDictionary(),
								(DFDComponentUncertaintySource) actSource, castedScenario);
						createNewMitigation(index + 1, relevantUncertainties, newDiagramAndDict, mitigations);
					} else {
						throw new IllegalArgumentException("Unexpected DFD uncertainty scenario: %s"
								.formatted(UncertaintyUtils.getUncertaintyScenarioName(scenario)));
					}
				}
			}
		}
	}

}
