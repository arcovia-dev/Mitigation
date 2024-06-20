package dev.abunai.confidentiality.mitigation;

import java.util.*;
import java.util.stream.Stream;

import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Behaviour;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;

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

public class MitigationUtils {

	public static DataFlowDiagramAndDictionary chooseBehaviorScenario(DataFlowDiagram dataFlowDiagram,
			DataDictionary dataDictionary, DFDBehaviorUncertaintySource source,
			DFDBehaviorUncertaintyScenario scenario) {

		// Extract assignments from default scenario and given scenario
		var targetBehaviorId = source.getTarget().getId();
		var newAssignments = scenario.getTargetAssignments();
		var oldAssignments = source.getTargetAssignments();
		var oldAssignmentsIds = oldAssignments.stream().map(a -> a.getId()).toList();

		correctNewAssignmentsPins(oldAssignments, newAssignments);

		// Add assignments from given scenario, remove assignments from default scenario
		var ddTargetBehaviors = dataDictionary.getBehaviour().stream().filter(b -> b.getId() == targetBehaviorId)
				.toList();
		var ddTargetBehaviorAssignments = ddTargetBehaviors.stream().map(b -> b.getAssignment()).toList();
		for (var ddTargetBehaviorAssignment : ddTargetBehaviorAssignments) {
			ddTargetBehaviorAssignment.addAll(newAssignments);
			ddTargetBehaviorAssignment.removeIf(a -> oldAssignmentsIds.contains(a.getId()));
		}
		return new DataFlowDiagramAndDictionary(dataFlowDiagram, dataDictionary);
	}

	public static DataFlowDiagramAndDictionary chooseInterfaceScenario(DataFlowDiagram dataFlowDiagram,
			DataDictionary dataDictionary, DFDInterfaceUncertaintySource source,
			DFDInterfaceUncertaintyScenario scenario) {

		// Extract destination node and pin from given scenario
		var targetFlow = source.getTargetFlow();
		var newDestinationNode = scenario.getTargetNode();
		var newDestinationPin = scenario.getTargetInPin();

		// Set destination node and pin to the ones in the given scenario
		var ddTargetFlow = dataFlowDiagram.getFlows().stream().filter(f -> f.getId() == targetFlow.getId()).toList()
				.get(0);
		ddTargetFlow.setDestinationNode(newDestinationNode);
		ddTargetFlow.setDestinationPin(newDestinationPin);

		return new DataFlowDiagramAndDictionary(dataFlowDiagram, dataDictionary);
	}

	public static DataFlowDiagramAndDictionary chooseExternalScenario(DataFlowDiagram dataFlowDiagram,
			DataDictionary dataDictionary, DFDExternalUncertaintySource source,
			DFDExternalUncertaintyScenario scenario) {

		// Extract Labels of default scenario (oldLabels) and of given scenario
		// (newLabels)
		var newNodeLabels = scenario.getTargetProperties();
		var oldNodeLabels = source.getTargetProperties();
		var targetNode = source.getTarget();

		// Remove Labels from default scenario and add Labels of given scenario
		var ddTargetNode = dataFlowDiagram.getNodes().stream().filter(n -> n.getId() == targetNode.getId()).toList()
				.get(0);
		ddTargetNode.getProperties().removeAll(oldNodeLabels);
		ddTargetNode.getProperties().addAll(newNodeLabels);

		return new DataFlowDiagramAndDictionary(dataFlowDiagram, dataDictionary);
	}

	public static DataFlowDiagramAndDictionary chooseComponentScenario(DataFlowDiagram dataFlowDiagram,
			DataDictionary dataDictionary, DFDComponentUncertaintySource source,
			DFDComponentUncertaintyScenario scenario) {

		// Extract Node from default and given scenario
		var oldNode = dataFlowDiagram.getNodes().stream().filter(n -> n.getId() == source.getTarget().getId()).toList()
				.get(0);
		var newNode = dataFlowDiagram.getNodes().stream().filter(n -> n.getId() == scenario.getTarget().getId())
				.toList().get(0);

		// Take over pins from target node in default scenario
		newNode.getBehaviour().getInPin().addAll(oldNode.getBehaviour().getInPin());
		newNode.getBehaviour().getOutPin().addAll(oldNode.getBehaviour().getOutPin());

		// Replace oldNode with newNode in all Flows
		for (var flow : dataFlowDiagram.getFlows()) {
			if (flow.getDestinationNode().getId() == oldNode.getId()) {
				flow.setDestinationNode(newNode);
			}
			if (flow.getSourceNode().getId() == oldNode.getId()) {
				flow.setSourceNode(newNode);
			}
		}

		dataFlowDiagram.getNodes().removeIf(n -> n.getId() == oldNode.getId());

		return new DataFlowDiagramAndDictionary(dataFlowDiagram, dataDictionary);
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
