package dev.abunai.confidentiality.mitigation;

import java.util.*;
import java.util.stream.Stream;

import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Behaviour;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;


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
		var newDD = (DataDictionary)EcoreUtil.copy(dataDictionary);
		
		var targetBehaviorId = source.getTarget().getId();
		var newAssignments = scenario.getTargetAssignments();
		var oldAssignments = source.getTargetAssignments();
		var oldAssignmentsIds = oldAssignments.stream().map(a -> a.getId()).toList();

		correctNewAssignmentsPins(oldAssignments, newAssignments);

		// Add assignments from given scenario, remove assignments from default scenario
		var ddTargetBehaviors = newDD.getBehaviour().stream().filter(b -> b.getId() == targetBehaviorId)
				.toList();
		var ddTargetBehaviorAssignments = ddTargetBehaviors.stream().map(b -> b.getAssignment()).toList();
		for (var ddTargetBehaviorAssignment : ddTargetBehaviorAssignments) {
			ddTargetBehaviorAssignment.addAll(newAssignments);
			ddTargetBehaviorAssignment.removeIf(a -> oldAssignmentsIds.contains(a.getId()));
		}
		return new DataFlowDiagramAndDictionary(dataFlowDiagram, newDD);
	}

	public static DataFlowDiagramAndDictionary chooseInterfaceScenario(DataFlowDiagram dataFlowDiagram,
			DataDictionary dataDictionary, DFDInterfaceUncertaintySource source,
			DFDInterfaceUncertaintyScenario scenario) {

		var newDia = (DataFlowDiagram)EcoreUtil.copy(dataFlowDiagram);
		// Extract destination node and pin from given scenario
		var targetFlow = source.getTargetFlow();
		var newDestinationNode = scenario.getTargetNode();
		var newDestinationPin = scenario.getTargetInPin();

		// Set destination node and pin to the ones in the given scenario
		var ddTargetFlow = newDia.getFlows().stream().filter(f -> f.getId() == targetFlow.getId()).toList()
				.get(0);
		ddTargetFlow.setDestinationNode(newDestinationNode);
		ddTargetFlow.setDestinationPin(newDestinationPin);

		return new DataFlowDiagramAndDictionary(newDia, dataDictionary);
	}

	public static DataFlowDiagramAndDictionary chooseExternalScenario(DataFlowDiagram dataFlowDiagram,
			DataDictionary dataDictionary, DFDExternalUncertaintySource source,
			DFDExternalUncertaintyScenario scenario) {

		// Extract Labels of default scenario (oldLabels) and of given scenario
		// (newLabels)
		var newDia = (DataFlowDiagram)EcoreUtil.copy(dataFlowDiagram);
		var newNodeLabels = scenario.getTargetProperties();
		var oldNodeLabels = source.getTargetProperties();
		var targetNode = source.getTarget();

		// Remove Labels from default scenario and add Labels of given scenario
		var ddTargetNode = newDia.getNodes().stream().filter(n -> n.getId() == targetNode.getId()).toList()
				.get(0);
		ddTargetNode.getProperties().removeAll(oldNodeLabels);
		ddTargetNode.getProperties().addAll(newNodeLabels);

		return new DataFlowDiagramAndDictionary(newDia, dataDictionary);
	}

	public static DataFlowDiagramAndDictionary chooseComponentScenario(DataFlowDiagram dataFlowDiagram,
			DataDictionary dataDictionary, DFDComponentUncertaintySource source,
			DFDComponentUncertaintyScenario scenario) {

		var newDia = (DataFlowDiagram)EcoreUtil.copy(dataFlowDiagram);
		var newDD = (DataDictionary)EcoreUtil.copy(dataDictionary);
		System.out.println(newDia.getNodes().get(0).getBehaviour().getAssignment().get(0));
		
		// Extract Node from default and given scenario
		var oldNode = newDia.getNodes().stream().filter(n -> n.getId() == source.getTarget().getId()).toList()
				.get(0);
		var newNode = newDia.getNodes().stream().filter(n -> n.getId() == scenario.getTarget().getId())
				.toList().get(0);

		// Take over pins from target node in default scenario
		newNode.getBehaviour().getInPin().addAll(oldNode.getBehaviour().getInPin());
		oldNode.getBehaviour().getInPin().clear();
		newNode.getBehaviour().getOutPin().addAll(oldNode.getBehaviour().getOutPin());
		oldNode.getBehaviour().getOutPin().clear();
		var oldNodeBehavior = newDD.getBehaviour().stream().filter(b->b.getId() == oldNode.getBehaviour().getId())
				.toList().get(0);
		var newNodeBehavior = newDD.getBehaviour().stream().filter(b->b.getId() == newNode.getBehaviour().getId())
				.toList().get(0);
		newNodeBehavior.getInPin().addAll(oldNodeBehavior.getInPin());
		newNodeBehavior.getOutPin().addAll(oldNodeBehavior.getOutPin());

		// Replace oldNode with newNode in all Flows
		for (var flow : newDia.getFlows()) {
			if (flow.getDestinationNode().getId() == oldNode.getId()) {
				flow.setDestinationNode(newNode);
			}
			if (flow.getSourceNode().getId() == oldNode.getId()) {
				flow.setSourceNode(newNode);
			}
		}

		newDia.getNodes().removeIf(n -> n.getId() == oldNode.getId());
		newDD.getBehaviour().remove(oldNodeBehavior);
		return new DataFlowDiagramAndDictionary(newDia, newDD);
	}

	/*public static DataFlowDiagramAndDictionary chooseConnectorScenario(DataFlowDiagram dataFlowDiagram,
			DataDictionary dataDictionary, DFDConnectorUncertaintySource source,
			DFDConnectorUncertaintyScenario scenario) {

		// Extract destination node and pin from given scenario
		var targetFlow = source.getTargetFlow();
		var newDestinationNode = scenario.getTargetNode();
		var newDestinationPin = scenario.getTargetPin();

		var newAssignments = scenario.getTargetAssignments();
		var oldAssignments = source.getTargetAssignments();
		var oldAssignmentsIds = oldAssignments.stream().map(a -> a.getId()).toList();

		correctNewAssignmentsPins(oldAssignments, newAssignments);

		// Set destination node and pin to the ones in the given scenario
		var ddTargetFlow = dataFlowDiagram.getFlows().stream().filter(f -> f.getId() == targetFlow.getId()).toList()
				.get(0);
		ddTargetFlow.setDestinationNode(newDestinationNode);
		ddTargetFlow.setDestinationPin(newDestinationPin);

		// Add assignments from given scenario, remove assignments from default scenario
		var ddTBehaviors = dataDictionary.getBehaviour();
		var ddTargetAssignments = ddTBehaviors.stream()
				.map(b -> b.getAssignment())
				.filter(a -> )
				.toList();
		for (var ddTargetBehaviorAssignment : ddTargetAssignments) {
			ddTargetBehaviorAssignment.addAll(newAssignments);
			ddTargetBehaviorAssignment.removeIf(a -> oldAssignmentsIds.contains(a.getId()));
		}

		return new DataFlowDiagramAndDictionary(dataFlowDiagram, dataDictionary);
	}*/

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
