package dev.arcovia.mitigation.smt.preprocess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dfd.DFDConfidentialityAnalysis;
import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.core.DFDTransposeFlowGraph;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.result.DSLResult;
import org.dataflowanalysis.analysis.dsl.selectors.CharacteristicsSelectorData;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelReference;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;

import dev.arcovia.mitigation.smt.FlowInstance;
import dev.arcovia.mitigation.smt.operations.LabelOperation;
import dev.arcovia.mitigation.smt.operations.LabelTypeOperation;
import dev.arcovia.mitigation.smt.utils.ParsingUtils;

/**
 * Given an input DFD and constraints, performs preprocessing
 */
public class Preprocess {

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Analyzes incoming constraints. If any reference labels that are not currently in the Datadictionary, they are added
     * @param dfd Incoming DFD
     * @param constraints Incoming Constraints
     * @return DFD that possesses the required labels
     */
    private DataFlowDiagramAndDictionary addMissingLabels(DataFlowDiagramAndDictionary dfd, List<AnalysisConstraint> constraints) {
        Set<CharacteristicsSelectorData> characteristicsSelectorData = ParsingUtils.getAnalysisCharacteristics(constraints);

        DataDictionary dd = dfd.dataDictionary();

        for (CharacteristicsSelectorData data : characteristicsSelectorData) {
            if (!data.characteristicType()
                    .isConstant()
                    || !data.characteristicValue()
                            .isConstant()) {
                logger.error("Variable detected in Constraints. Currently not supported.");
                throw new UnsupportedOperationException("Variable detected in constraints. Currently not supported.");
            }
            // First add label type if it does not exist
            String type = data.characteristicType()
                    .toString();
            if (!ParsingUtils.containsLabelType(dd, type)) {
                LabelTypeOperation modifyLabelType = new LabelTypeOperation(type);
                modifyLabelType.doOperation(dfd);
            }
            // Now add label
            String value = data.characteristicValue()
                    .toString();
            LabelType parentType = ParsingUtils.getLabelTypeByName(dd, type);
            if (!ParsingUtils.containsLabel(parentType, value)) {
                LabelOperation modifyLabel = new LabelOperation(type, value);
                modifyLabel.doOperation(dfd);
            }
        }
        return dfd;
    }

    /**
     * Performs preprocessing on the incoming DFD. Adds missing labels. Extracts Flow Instances and DFDVertices from TFGs. Finds
     * relevant node labels and types, as well as data labels based on constraints
     * @param dfdIn Input DFD
     * @param analysisConstraints Input constraints
     * @return Record Type that has all relevant preprocessing information
     */
    public PreprocessingResult preprocess(DataFlowDiagramAndDictionary dfdIn, List<AnalysisConstraint> analysisConstraints,
            boolean onlyViolatingTFGs) {
        dfdIn = addMissingLabels(dfdIn, analysisConstraints);

        DataFlowDiagram dfd = dfdIn.dataFlowDiagram();
        DataDictionary dd = dfdIn.dataDictionary();
        Map<Pin, List<AbstractAssignment>> outPinToAss = ParsingUtils.outPinToAssignments(dfd.getNodes());

        // Determine relevant labels based on confidentiality constraints.
        // Labels that appear in negated Vertex Selectors. Adding them could repair violations.
        Set<Label> relevantNodeLabelsAdd = ParsingUtils.getRelevantNodeLabelsAdd(dd, analysisConstraints);
        // Labels that appear in non-negated Vertex Selectors. Removing them could repair violations.
        Set<Label> relevantNodeLabelsRemove = ParsingUtils.getRelevantNodeLabelsRemove(dd, analysisConstraints);
        // Labels that appear in negated Data Selectors. Adding them could repair violations.
        Set<Label> relevantDataLabelsAdd = ParsingUtils.getRelevantDataLabelsAdd(dd, analysisConstraints);
        // Labels that appear in non-negated Data Selectors. Removing them could repair violations.
        Set<Label> relevantDataLabelsRemove = ParsingUtils.getRelevantDataLabelsRemove(dd, analysisConstraints);

        // Data Labels that are not constraint-relevant also need to be considered if they
        // can modify relevant labels
        // by being referenced in assign statements that modify these labels
        // Transitively, any labels that could modify such non-constraint-relevant
        // labels, which could in turn
        // influence constraint-relevant labels also need to be considered.
        // Therefore we repeat this process until now new labels get added
        List<Assignment> assignStatements = outPinToAss.values()
                .stream()
                .flatMap(x -> x.stream())
                .filter(Assignment.class::isInstance)
                .map(Assignment.class::cast)
                .toList();
        boolean changed;
        do {
            changed = false;
            for (Assignment assignment : assignStatements) {
                /// only consider assignments that could add or remove relevant labels
                if (Collections.disjoint(assignment.getOutputLabels(), relevantDataLabelsAdd)
                        && Collections.disjoint(assignment.getOutputLabels(), relevantDataLabelsRemove)) {
                    continue;
                }
                Set<LabelReference> labelReferences = ParsingUtils.reduceToLabelReferences(assignment.getTerm());
                // Add the labels to the relevant data labels
                for (LabelReference labelRef : labelReferences) {
                    Label label = labelRef.getLabel();
                    if (!relevantDataLabelsAdd.contains(label)) {
                        relevantDataLabelsAdd.add(label);
                        changed = true;
                    }
                    if (!relevantDataLabelsRemove.contains(label)) {
                        relevantDataLabelsRemove.add(label);
                        changed = true;
                    }
                }
            }
        } while (changed);

        DFDModelResourceProvider dfdModelResourceProvider = new DFDModelResourceProvider(dd, dfd);
        DFDConfidentialityAnalysis dfdConfidentialityAnalysis = new DFDDataFlowAnalysisBuilder().standalone()
                .useCustomResourceProvider(dfdModelResourceProvider)
                .build();
        long before = System.currentTimeMillis();
        DFDFlowGraphCollection flowGraphs = dfdConfidentialityAnalysis.findFlowGraphs();
        long after = System.currentTimeMillis();
        long findTFGsTime = after - before;

        // There are cases where inspecting only violating TFGs can lead to not finding minimal repairs.
        // We suspect that these cases are exactly those, where relevantNodeLabelsAdd and relevantNodeLabelsRemove are not
        // disjoint and
        // the addition and removal of node labels is allowed OR relevantDataLabelsAdd and relevantDataLabelsRemove are not
        // disjoint and
        // the addition and removal of data labels is allowed
        Set<DFDTransposeFlowGraph> tfgs;
        if (onlyViolatingTFGs) {
            logger.warn("Encoding only violating TFGs may lead to non-minimal repairs");
            // Label propagation needs to be done for constraint evaluation
            flowGraphs.evaluate();
            Set<DFDTransposeFlowGraph> violatingTFGs = new HashSet<>();
            for (AnalysisConstraint analysisConstraint : analysisConstraints) {
                List<DSLResult> violations = analysisConstraint.findViolations(flowGraphs);
                // Add all violating tfgs
                for (int j = 0; j < violations.size(); j++) {
                    violatingTFGs.add((DFDTransposeFlowGraph) violations.get(j)
                            .getTransposeFlowGraph());
                }
            }
            tfgs = violatingTFGs;
        } else {
            // If all tfgs are considered the TFGs do not have to be evaluated as we model the label propagation ourselves
            tfgs = flowGraphs.getTransposeFlowGraphs()
                    .stream()
                    .filter(DFDTransposeFlowGraph.class::isInstance)
                    .map(DFDTransposeFlowGraph.class::cast)
                    .collect(Collectors.toSet());
        }

        Set<DFDVertex> allVertices = new HashSet<>();
        Set<FlowInstance> allFlows = new HashSet<>();
        Map<DFDVertex, List<FlowInstance>> allFlowInstancesToVertex = new HashMap<>();
        // Create flow instances for each tfg
        for (DFDTransposeFlowGraph tfg : tfgs) {
            // Find all vertices for this tfg
            List<DFDVertex> vertices = tfg.getVertices()
                    .stream()
                    .filter(DFDVertex.class::isInstance)
                    .map(DFDVertex.class::cast)
                    .toList();
            // All flow instances for this tfg.
            List<FlowInstance> allFlowInstancesCurrTFG = new ArrayList<>();
            // Iterate over vertices backwards. This is crucial because the vertex API offers better support
            // for interfacing with preceeding vertices than with suceeding ones. It also eases the creation of correct
            // forwarding and assign relationships between flows.
            for (int j = vertices.size() - 1; j >= 0; j--) {
                DFDVertex vertex = vertices.get(j);
                // Pins that lead to preceeding vertices
                Map<Pin, DFDVertex> previousVerticesMap = vertex.getPinDFDVertexMap();
                // Input pins of this vertex and their flow
                Map<Pin, Flow> pinFlowMap = vertex.getPinFlowMap();
                // Create a flow for each incoming pin that is connected to a previous vertex
                for (Entry<Pin, Flow> pinFlow : pinFlowMap.entrySet()) {
                    // Find DFD flow that this flow instance should model
                    Flow flow = pinFlow.getValue();
                    // Create a new Flow instance that represents the occurence of said flow for this TFG
                    FlowInstance flowInstance = new FlowInstance(flow.getSourcePin(), previousVerticesMap.get(flow.getDestinationPin()), flow.getDestinationPin(),
                            vertex, flow);
                    allFlowInstancesCurrTFG.add(flowInstance);
                    // Also keep track of vertices that flows flow to as this is needed for later constraint encoding
                    List<FlowInstance> allFlowInstancesToThisVertex = allFlowInstancesToVertex.getOrDefault(vertex, new ArrayList<FlowInstance>());
                    // For this vertex
                    allFlowInstancesToThisVertex.add(flowInstance);
                    // For all vertices
                    allFlowInstancesToVertex.put(vertex, allFlowInstancesToThisVertex);
                }
                // Create mappings, so we can later know which flow instances are forwarded by which
                // and on which flows assign statements have to be evaluated.
                // Find all flows that leave this vertex because they may forward or Assign.
                // They exist because they were already created when handling the succeeding vertex
                List<FlowInstance> thisVertexOutgoingFlows = allFlowInstancesCurrTFG.stream()
                        .filter(x -> x.getSourceVertex()
                                .equals(vertex))
                        .toList();
                // Also find all incoming flows to connect them to the outgoing ones
                List<FlowInstance> allFlowInstancesToThisVertex = allFlowInstancesToVertex.getOrDefault(vertex, new ArrayList<FlowInstance>());
                // For every outgoing flow
                for (FlowInstance flowInstance : thisVertexOutgoingFlows) {
                    // For every assignment of its source pin
                    List<AbstractAssignment> thisPinAssigns = outPinToAss.get(flowInstance.getSourcePin());
                    for (AbstractAssignment assign : thisPinAssigns) {
                        // A forward or Assign assignment forwards exactly those flows that flow to a input pin that is referenced
                        // by the assignment within the same tfg
                        if (assign instanceof ForwardingAssignment cast) {
                            List<FlowInstance> thisFlowForwards = allFlowInstancesToThisVertex.stream()
                                    .filter(x -> cast.getInputPins()
                                            .contains(x.getDestinationPin()))
                                    .toList();
                            flowInstance.getThisFlowForwards()
                                    .put(cast, thisFlowForwards);
                        } else if (assign instanceof Assignment cast) {
                            List<FlowInstance> thisFlowEvaluatesOn = allFlowInstancesToThisVertex.stream()
                                    .filter(x -> cast.getInputPins()
                                            .contains(x.getDestinationPin()))
                                    .toList();
                            flowInstance.getThisFlowEvaluatesOn()
                                    .put(cast, thisFlowEvaluatesOn);
                        }
                    }
                }
                allVertices.add(vertex);
            }
            allFlows.addAll(allFlowInstancesCurrTFG);
        }

        PreprocessingResult result = new PreprocessingResult(dfdIn, allFlows, allVertices, relevantNodeLabelsAdd, relevantNodeLabelsRemove,
                relevantDataLabelsAdd, relevantDataLabelsRemove, allFlowInstancesToVertex, findTFGsTime);

        return result;
    }
}
