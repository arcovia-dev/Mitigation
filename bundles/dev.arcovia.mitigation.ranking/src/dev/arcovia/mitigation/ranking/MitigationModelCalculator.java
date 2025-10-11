package dev.arcovia.mitigation.ranking;

import java.io.IOException;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;

import dev.abunai.confidentiality.analysis.core.UncertaintyUtils;
import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.*;

public class MitigationModelCalculator {
    List<Predicate<? super AbstractVertex<?>>> constraintFunctions;
    boolean findFirstModel;
    DataFlowDiagramAndDictionary diagramAndDict;
    UncertaintySubset relevantUncertaintySubset;
    MitigationURIs mitigationURIs;
    List<MitigationModel> mitigationCandidates = new ArrayList<>();

    public MitigationModelCalculator(DataFlowDiagramAndDictionary diagramAndDict, UncertaintySubset relevantUncertaintySubset,
            MitigationURIs mitigationURIs, List<Predicate<? super AbstractVertex<?>>> constraintFunctions, boolean findFirstModel) {
        this.diagramAndDict = diagramAndDict;
        this.relevantUncertaintySubset = relevantUncertaintySubset;
        this.mitigationURIs = mitigationURIs;
        this.constraintFunctions = constraintFunctions;
        this.findFirstModel = findFirstModel;
    }

    /**
     * Finds and returns a list of mitigating models based on the given uncertainty sources, data flow diagrams, and
     * constraints. The method cleans the output path, generates mitigation candidates by considering uncertainty scenarios,
     * and finally stores and processes them while filtering irrelevant uncertainties.
     * @return A list of mitigating models that meet the constraints and have been successfully processed.
     */
    public List<MitigationModel> findMitigatingModel() {

        cleanOutputPath(mitigationURIs);

        createMitigationCandidates(0, relevantUncertaintySubset.getSubsetSources(), diagramAndDict, mitigationCandidates, new ArrayList<String>());

        List<UncertaintySource> irrelevantUncertainties = relevantUncertaintySubset.getNotInSubsetSources();
        return storeMitigationCandidates(mitigationCandidates, irrelevantUncertainties, mitigationURIs);
    }

    private void cleanOutputPath(MitigationURIs mitigationURIs) {
        var outputDirectoryPathString = getOutputPathFromURI(mitigationURIs.mitigationUncertaintyURI());
        var outputDirectoryPath = Paths.get(outputDirectoryPathString);
        var outputDirectory = new File(outputDirectoryPathString);
        if (!Files.exists(outputDirectoryPath)) {
            return;
        }
        for (var file : outputDirectory.listFiles()) {
            file.delete();
        }
    }

    /**
     * Generates mitigation candidates by exploring combinations of uncertainty scenarios and determining if they lead to
     * valid mitigation models.
     * @param index The current index of the uncertainty source being considered in the recursive process.
     * @param relevantUncertainties A list of uncertainty sources relevant to the mitigation computation.
     * @param diagramAndDict The current data flow diagram and its corresponding dictionary being analyzed.
     * @param candidates The list where discovered mitigation models are stored as candidates.
     * @param chosenScenarios A list of scenario identifiers representing the current combination of uncertainties being
     * processed.
     */
    private void createMitigationCandidates(int index, List<UncertaintySource> relevantUncertainties, DataFlowDiagramAndDictionary diagramAndDict,
            List<MitigationModel> candidates, List<String> chosenScenarios) {
        // Store possible mitigation in mitigations list when all uncertainties got
        // considered
        if (index == relevantUncertainties.size()) {
            var mitigationModelName = "mitigation" + candidates.size();
            if (!isViolatingDFD(diagramAndDict))
                candidates.add(new MitigationModel(diagramAndDict, mitigationModelName, chosenScenarios));

            return;
        }
        // Consider all options for uncertainty source actSource
        else {
            var actSource = relevantUncertainties.get(index);

            // chose default scenario
            List<String> newChosenScenariosD = new ArrayList<String>(chosenScenarios);
            newChosenScenariosD.add("D");
            createMitigationCandidates(index + 1, relevantUncertainties, diagramAndDict, candidates, newChosenScenariosD);

            for (var scenario : UncertaintyUtils.getUncertaintyScenarios(actSource)) {
                if (UncertaintyUtils.isDefaultScenario(actSource, scenario)) {
                    break;
                }

                DataFlowDiagramAndDictionary newDiagramAndDict = getDFD(scenario, diagramAndDict, actSource);

                var newChosenScenariosA = new ArrayList<String>(chosenScenarios);
                newChosenScenariosA.add("A" + scenario.getId());
                createMitigationCandidates(index + 1, relevantUncertainties, newDiagramAndDict, candidates, newChosenScenariosA);
            }

        }
    }

    private boolean isViolatingDFD(DataFlowDiagramAndDictionary dfd) {
        var resourceProvider = new DFDModelResourceProvider(dfd.dataDictionary(), dfd.dataFlowDiagram());
        var analysis = new DFDDataFlowAnalysisBuilder().standalone()
                .useCustomResourceProvider(resourceProvider)
                .build();

        analysis.initializeAnalysis();

        DFDFlowGraphCollection flowGraph = analysis.findFlowGraphs();

        flowGraph.evaluate();

        for (var constraint : constraintFunctions) {
            for (var tfg : flowGraph.getTransposeFlowGraphs()) {
                var violations = analysis.queryDataFlow(tfg, constraint);
                if (violations.size() > 0) {
                    return true;
                }
            }

        }
        return false;
    }

    private static DataFlowDiagramAndDictionary getDFD(UncertaintyScenario scenario, DataFlowDiagramAndDictionary diagramAndDict,
            UncertaintySource actSource) {
        if (scenario instanceof DFDExternalUncertaintyScenario castedScenario) {
            return UncertaintySourceMitigationUtils.chooseExternalScenario(diagramAndDict.dataFlowDiagram(), diagramAndDict.dataDictionary(),
                    (DFDExternalUncertaintySource) actSource, castedScenario);

        } else if (scenario instanceof DFDBehaviorUncertaintyScenario castedScenario) {
            return UncertaintySourceMitigationUtils.chooseBehaviorScenario(diagramAndDict.dataFlowDiagram(), diagramAndDict.dataDictionary(),
                    (DFDBehaviorUncertaintySource) actSource, castedScenario);

        } else if (scenario instanceof DFDInterfaceUncertaintyScenario castedScenario) {
            return UncertaintySourceMitigationUtils.chooseInterfaceScenario(diagramAndDict.dataFlowDiagram(), diagramAndDict.dataDictionary(),
                    (DFDInterfaceUncertaintySource) actSource, castedScenario);

        } else if (scenario instanceof DFDComponentUncertaintyScenario castedScenario) {
            return UncertaintySourceMitigationUtils.chooseComponentScenario(diagramAndDict.dataFlowDiagram(), diagramAndDict.dataDictionary(),
                    (DFDComponentUncertaintySource) actSource, castedScenario);

        } else if (scenario instanceof DFDConnectorUncertaintyScenario castedScenario) {
            return UncertaintySourceMitigationUtils.chooseConnectorScenario(diagramAndDict.dataFlowDiagram(), diagramAndDict.dataDictionary(),
                    (DFDConnectorUncertaintySource) actSource, castedScenario);

        } else {
            throw new IllegalArgumentException(
                    "Unexpected DFD uncertainty scenario: %s".formatted(UncertaintyUtils.getUncertaintyScenarioName(scenario)));
        }
    }

    /**
     * Stores and processes mitigation candidates by filtering uncertainty sources and saving the relevant mitigation
     * models.
     * @param candidates A list of mitigation models to be stored after filtering relevant uncertainties.
     * @param uncertaintiesToKeep A list of uncertainty sources that should be retained during filtering.
     * @param mitigationURIs URIs representing file paths or memory locations used for storing uncertainty models and their
     * mitigated versions.
     * @return A list of mitigation models that have been successfully stored and processed.
     */
    private List<MitigationModel> storeMitigationCandidates(List<MitigationModel> candidates, List<UncertaintySource> uncertaintiesToKeep,
            MitigationURIs mitigationURIs) {

        ResourceSet resSet = new ResourceSetImpl();
        Resource oldUncertaintyRes = resSet.getResource(mitigationURIs.modelUncertaintyURI(), true);
        Resource newUncertaintyRes = resSet.createResource(mitigationURIs.mitigationUncertaintyURI());
        List<String> idsToKeep = uncertaintiesToKeep.stream()
                .map(u -> u.getId())
                .toList();

        var sourceCollection = oldUncertaintyRes.getContents()
                .get(0);
        newUncertaintyRes.getContents()
                .add(sourceCollection);

        Stack<EObject> objectsToRemove = new Stack<>();
        for (EObject eObject : sourceCollection.eContents()) {
            var id = EcoreUtil.getID(eObject);
            if (!idsToKeep.contains(id)) {
                objectsToRemove.push(eObject);
            }
        }

        while (!objectsToRemove.isEmpty()) {
            var object = objectsToRemove.pop();
            EcoreUtil.delete(object);
        }

        try {
            newUncertaintyRes.save(Collections.EMPTY_MAP);
        } catch (IOException e) {
            e.printStackTrace();
        }

        var result = new ArrayList<MitigationModel>();
        String outputPath = getOutputPathFromURI(mitigationURIs.mitigationUncertaintyURI());

        for (int i = 0; i < candidates.size(); i++) {
            // Store dataflowdigrams and datadictionaries
            var dfd = candidates.get(i)
                    .model();
            dfd.save("", Paths.get(outputPath, "mitigation" + Integer.toString(i))
                    .toString());
            result.add(candidates.get(i));

            if (findFirstModel) {
                return result;
            }
        }
        return result;
    }

    private static String getOutputPathFromURI(URI mitigationUncertaintyURI) {
        var segments = mitigationUncertaintyURI.segmentsList();
        return segments.get(segments.size() - 2);
    }

}
