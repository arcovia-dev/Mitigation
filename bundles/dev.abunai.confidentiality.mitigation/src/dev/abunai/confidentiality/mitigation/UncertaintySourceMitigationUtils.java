package dev.abunai.confidentiality.mitigation;

import java.util.*;

import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Behaviour;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.eclipse.emf.ecore.util.EcoreUtil;

import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDBehaviorUncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDBehaviorUncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDComponentUncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDComponentUncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDConnectorUncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDConnectorUncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDExternalUncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDExternalUncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDInterfaceUncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDInterfaceUncertaintySource;

public class UncertaintySourceMitigationUtils {

	public static DataFlowDiagramAndDictionary chooseBehaviorScenario(DataFlowDiagram dataFlowDiagram,
			DataDictionary dataDictionary, DFDBehaviorUncertaintySource source,
			DFDBehaviorUncertaintyScenario scenario) {

		// Extract assignments from default scenario and given scenario
		var newDD = (DataDictionary) EcoreUtil.copy(dataDictionary);
		var newDia = (DataFlowDiagram) EcoreUtil.copy(dataFlowDiagram);

		var targetBehaviorId = source.getTarget().getId();
		var oldAssignmentsIds = source.getTargetAssignments().stream().map(a -> a.getId()).toList();
		var newAssignmentsIds = scenario.getTargetAssignments().stream().map(a -> a.getId()).toList();

		var allAssignments = newDD.getBehaviour().stream().map(b -> b.getAssignment()).flatMap(List::stream).toList();
		var oldAssignments = allAssignments.stream().filter(a -> oldAssignmentsIds.contains(a.getId())).toList();
		var newAssignments = allAssignments.stream().filter(b -> newAssignmentsIds.contains(b.getId())).toList();

		correctNewAssignmentsPins(oldAssignments, newAssignments);

		// Add assignments from given scenario, remove assignments from default scenario
		var ddTargetBehaviors = newDD.getBehaviour().stream().filter(b -> b.getId() == targetBehaviorId).toList();

		for (var targetBehavior : ddTargetBehaviors) {
			targetBehavior.getAssignment().removeIf(a -> oldAssignmentsIds.contains(a.getId()));
			targetBehavior.getAssignment().addAll(newAssignments);
		}

		replaceOldDDReferencesWithTheOnesFromNewDD(newDD, newDia);

		return new DataFlowDiagramAndDictionary(newDia, newDD);
	}

	public static DataFlowDiagramAndDictionary chooseInterfaceScenario(DataFlowDiagram dataFlowDiagram,
			DataDictionary dataDictionary, DFDInterfaceUncertaintySource source,
			DFDInterfaceUncertaintyScenario scenario) {
		var newDia = (DataFlowDiagram) EcoreUtil.copy(dataFlowDiagram);

		// Extract destination node and pin from given scenario
		var targetFlow = source.getTargetFlow();
		var newDestinationNode = scenario.getTargetNode();
		var newDestinationPin = scenario.getTargetInPin();

		// Set destination node and pin to the ones in the given scenario
		var ddTargetFlows = newDia.getFlows().stream().filter(f -> f.getId() == targetFlow.getId()).toList();
		if (ddTargetFlows.size() == 0) {
			System.out.println("Flow " + targetFlow.getEntityName() + " not found");
			return new DataFlowDiagramAndDictionary(newDia, dataDictionary);
		}
		ddTargetFlows.get(0).setDestinationNode(newDestinationNode);
		ddTargetFlows.get(0).setDestinationPin(newDestinationPin);

		return new DataFlowDiagramAndDictionary(newDia, dataDictionary);
	}

	public static DataFlowDiagramAndDictionary chooseExternalScenario(DataFlowDiagram dataFlowDiagram,
			DataDictionary dataDictionary, DFDExternalUncertaintySource source,
			DFDExternalUncertaintyScenario scenario) {
		var newDia = (DataFlowDiagram) EcoreUtil.copy(dataFlowDiagram);

		// Extract Labels of default scenario (oldLabels) and of given scenario
		// (newLabels)
		var newNodeLabels = scenario.getTargetProperties();
		var oldNodeLabels = source.getTargetProperties();
		var targetNode = source.getTarget();

		// Remove Labels from default scenario and add Labels of given scenario
		var ddTargetNodes = newDia.getNodes().stream().filter(n -> n.getId() == targetNode.getId()).toList();
		if (ddTargetNodes.size() == 0) {
			System.out.println(targetNode.getEntityName());
			return new DataFlowDiagramAndDictionary(newDia, dataDictionary);
		}

		ddTargetNodes.get(0).getProperties().removeAll(oldNodeLabels);
		ddTargetNodes.get(0).getProperties().addAll(newNodeLabels);

		return new DataFlowDiagramAndDictionary(newDia, dataDictionary);
	}

	public static DataFlowDiagramAndDictionary chooseComponentScenario(DataFlowDiagram dataFlowDiagram,
			DataDictionary dataDictionary, DFDComponentUncertaintySource source,
			DFDComponentUncertaintyScenario scenario) {
		var newDia = (DataFlowDiagram) EcoreUtil.copy(dataFlowDiagram);
		var newDD = (DataDictionary) EcoreUtil.copy(dataDictionary);

		// Extract Node from default and given scenario
		var oldNode = newDia.getNodes().stream().filter(n -> n.getId() == source.getTarget().getId()).toList().get(0);
		var oldNodeBehaviorId = oldNode.getBehaviour().getId();
		var newNode = newDia.getNodes().stream().filter(n -> n.getId() == scenario.getTarget().getId()).toList().get(0);
		var newNodeBehaviorId = newNode.getBehaviour().getId();

		// Replace oldNode with newNode in all Flows
		for (var flow : newDia.getFlows()) {
			if (flow.getDestinationNode().getId() == oldNode.getId()) {
				flow.setDestinationNode(newNode);
			}
			if (flow.getSourceNode().getId() == oldNode.getId()) {
				flow.setSourceNode(newNode);
			}
		}
		// Remove old Node
		newDia.getNodes().removeIf(n -> n.getId() == oldNode.getId());

		Behaviour newNodeNewBehavior = newDD.getBehaviour().stream().filter(b -> b.getId().equals(newNodeBehaviorId))
				.toList().get(0);
		Behaviour oldNodeOldBehavior = newDD.getBehaviour().stream().filter(b -> b.getId().equals(oldNodeBehaviorId))
				.toList().get(0);
		newNodeNewBehavior.getInPin().addAll(oldNodeOldBehavior.getInPin());
		newNodeNewBehavior.getOutPin().addAll(oldNodeOldBehavior.getOutPin());
		newDD.getBehaviour().remove(oldNodeOldBehavior);
		newNode.setBehaviour(newNodeNewBehavior);

		// removeInPinsThatDontOccurInFlows(newDD, newDia);
		replaceOldDDReferencesWithTheOnesFromNewDD(newDD, newDia);

		return new DataFlowDiagramAndDictionary(newDia, newDD);
	}

	private static void replaceOldDDReferencesWithTheOnesFromNewDD(DataDictionary newDD, DataFlowDiagram newDia) {
		// Replace Behaviors and Properties
		for (var node : newDia.getNodes()) {
			var nodeBehaviorId = node.getBehaviour().getId();
			var nodeBehaviorInDD = newDD.getBehaviour().stream().filter(b -> b.getId().equals(nodeBehaviorId)).toList()
					.get(0);
			node.setBehaviour(nodeBehaviorInDD);
			var nodePropertyIds = node.getProperties().stream().map(p -> p.getId()).toList();
			node.getProperties().clear();
			for (var npid : nodePropertyIds) {
				var newProp = newDD.getLabelTypes().stream().map(l -> l.getLabel()).flatMap(List::stream)
						.filter(l -> l.getId().equals(npid)).toList().get(0);
				node.getProperties().add(newProp);
			}
		}
		// Replace pins
		List<Flow> flowsToRemove = new ArrayList<Flow>();
		for (var flow : newDia.getFlows()) {
			var srcPinId = flow.getSourcePin().getId();
			var dstPinId = flow.getDestinationPin().getId();
			var newSrcPin = newDD.getBehaviour().stream().map(b -> b.getOutPin()).flatMap(List::stream)
					.filter(p -> p.getId() != null).filter(p -> p.getId().equals(srcPinId)).findFirst();
			var newDstPin = newDD.getBehaviour().stream().map(b -> b.getInPin()).flatMap(List::stream)
					.filter(p -> p.getId() != null).filter(p -> p.getId().equals(dstPinId)).findFirst();
			if (newDstPin.isPresent() && newSrcPin.isPresent()) {
				flow.setSourcePin(newSrcPin.get());
				flow.setDestinationPin(newDstPin.get());
			}
			else {
				flowsToRemove.add(flow);
			}
		}
		newDia.getFlows().removeAll(flowsToRemove);
	}

	public static DataFlowDiagramAndDictionary chooseConnectorScenario(DataFlowDiagram dataFlowDiagram,
			DataDictionary dataDictionary, DFDConnectorUncertaintySource source,
			DFDConnectorUncertaintyScenario scenario) {
		var newDia = (DataFlowDiagram) EcoreUtil.copy(dataFlowDiagram);
		var newDD = (DataDictionary) EcoreUtil.copy(dataDictionary);

		// Extract fields for replacements
		var targetFlow = source.getTargetFlow();
		var newDestinationNode = scenario.getTargetNode();
		var newDestinationPin = scenario.getTargetPin();
		var oldAssignmentsIds = source.getTargetAssignments().stream().map(a -> a.getId()).toList();
		var newAssignmentsIds = scenario.getTargetAssignments().stream().map(a -> a.getId()).toList();
		var newAssignments = newDD.getBehaviour().stream().map(b -> b.getAssignment()).flatMap(List::stream)
				.filter(a -> newAssignmentsIds.contains(a.getId())).toList();

		// Set destination node and pin to the ones in the given scenario
		var ddTargetFlow = newDia.getFlows().stream().filter(f -> f.getId() == targetFlow.getId()).toList().get(0);
		ddTargetFlow.setDestinationNode(newDestinationNode);
		ddTargetFlow.setDestinationPin(newDestinationPin);

		replaceAssignments(newDD, oldAssignmentsIds, newAssignmentsIds, newAssignments);

		return new DataFlowDiagramAndDictionary(newDia, newDD);
	}

	private static void replaceAssignments(DataDictionary newDD, List<String> oldAssignmentsIds,
			List<String> newAssignmentsIds, List<AbstractAssignment> newAssignments) {

		for (var behavior : newDD.getBehaviour()) {
			var addNewAssignments = false;
			List<AbstractAssignment> assignmentsToRemove = new ArrayList<>();
			for (var assignment : behavior.getAssignment()) {
				if (oldAssignmentsIds.contains(assignment.getId())) {
					assignmentsToRemove.add(assignment);
					addNewAssignments = true;
				}
				behavior.getAssignment().removeAll(assignmentsToRemove);
				if (addNewAssignments) {
					behavior.getAssignment().addAll(newAssignments);
				}
			}
		}
	}

	/**
	 * Replaces pins of newAssignments with the ones in the old assigments (matching
	 * them by entity name)
	 **/
	private static void correctNewAssignmentsPins(List<AbstractAssignment> oldAssignments,
			List<AbstractAssignment> newAssignments) {
		// Create dict which will be used to adapt the pins of the
		// new assignments to the ones given in the targetAssignment
		Map<String, String> inPinEntityNameToInPinId = new HashMap<>();

		for (var assignment : oldAssignments) {
			for (var inPin : assignment.getInputPins()) {
				inPinEntityNameToInPinId.put(inPin.getEntityName(), inPin.getId());
			}
			var outPin = assignment.getOutputPin();
			inPinEntityNameToInPinId.put(outPin.getEntityName(), outPin.getId());
		}

		for (var assignment : newAssignments) {
			for (var pin : assignment.getInputPins()) {
				pin.setId(inPinEntityNameToInPinId.get(pin.getEntityName()));
			}
			var outputPin = assignment.getOutputPin();
			outputPin.setId(inPinEntityNameToInPinId.get(outputPin.getEntityName()));
		}
	}

}
