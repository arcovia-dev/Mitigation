package dev.arcovia.mitigation.ranking;

import java.util.*;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.Behavior;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
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

    public static DataFlowDiagramAndDictionary chooseBehaviorScenario(DataFlowDiagram dataFlowDiagram, DataDictionary dataDictionary,
            DFDBehaviorUncertaintySource source, DFDBehaviorUncertaintyScenario scenario) {

        // Extract assignments from default scenario and given scenario
        var newDD = (DataDictionary) EcoreUtil.copy(dataDictionary);
        var newDia = (DataFlowDiagram) EcoreUtil.copy(dataFlowDiagram);

        var targetBehaviorId = source.getTarget()
                .getId();
        var oldAssignmentsIds = source.getTargetAssignments()
                .stream()
                .map(a -> a.getId())
                .toList();
        var newAssignmentsIds = scenario.getTargetAssignments()
                .stream()
                .map(a -> a.getId())
                .toList();

        var allAssignments = newDD.getBehavior()
                .stream()
                .map(b -> b.getAssignment())
                .flatMap(List::stream)
                .toList();
        var oldAssignments = allAssignments.stream()
                .filter(a -> oldAssignmentsIds.contains(a.getId()))
                .toList();
        var newAssignments = allAssignments.stream()
                .filter(b -> newAssignmentsIds.contains(b.getId()))
                .toList();

        correctNewAssignmentsPins(oldAssignments, newAssignments);

        // Add assignments from given scenario, remove assignments from default scenario
        var ddTargetBehaviors = newDD.getBehavior()
                .stream()
                .filter(b -> b.getId()
                        .equals(targetBehaviorId))
                .toList();

        var targetBehavior = ddTargetBehaviors.get(0);
        targetBehavior.getAssignment()
                .removeIf(a -> oldAssignmentsIds.contains(a.getId()));
        targetBehavior.getAssignment()
                .addAll(newAssignments);

        replaceOldDDReferencesWithTheOnesFromNewDD(newDD, newDia);

        // Make sure pins do not occur twice
        var targetBehaviorInpinsIds = targetBehavior.getInPin()
                .stream()
                .map(p -> p.getId())
                .toList();
        var targetBehaviorOutpinsIds = targetBehavior.getInPin()
                .stream()
                .map(p -> p.getId())
                .toList();
        for (var behavior : newDD.getBehavior()) {
            if (behavior.getId()
                    .equals(targetBehavior.getId())) {
                continue;
            }
            var inPinsToRemove = new ArrayList<Pin>();
            var outPinsToRemove = new ArrayList<Pin>();
            for (var inPin : behavior.getInPin()) {
                if (targetBehaviorInpinsIds.contains(inPin.getId())) {
                    inPinsToRemove.add(inPin);
                }
            }
            behavior.getInPin()
                    .removeAll(inPinsToRemove);
            for (var outPin : behavior.getInPin()) {
                if (targetBehaviorOutpinsIds.contains(outPin.getId())) {
                    outPinsToRemove.add(outPin);
                }
            }
            behavior.getOutPin()
                    .removeAll(outPinsToRemove);
        }

        return new DataFlowDiagramAndDictionary(newDia, newDD);
    }

    public static DataFlowDiagramAndDictionary chooseInterfaceScenario(DataFlowDiagram dataFlowDiagram, DataDictionary dataDictionary,
            DFDInterfaceUncertaintySource source, DFDInterfaceUncertaintyScenario scenario) {
        var newDia = (DataFlowDiagram) EcoreUtil.copy(dataFlowDiagram);

        // Extract destination node and pin from given scenario
        var targetFlow = source.getTargetFlow();
        var newDestinationNode = scenario.getTargetNode();
        var newDestinationPin = scenario.getTargetInPin();

        // Set destination node and pin to the ones in the given scenario
        var ddTargetFlows = newDia.getFlows()
                .stream()
                .filter(f -> f.getId()
                        .equals(targetFlow.getId()))
                .toList();
        if (ddTargetFlows.size() == 0) {
            System.out.println("Flow " + targetFlow.getEntityName() + " not found");
            return new DataFlowDiagramAndDictionary(newDia, dataDictionary);
        }
        ddTargetFlows.get(0)
                .setDestinationNode(newDestinationNode);
        ddTargetFlows.get(0)
                .setDestinationPin(newDestinationPin);

        return new DataFlowDiagramAndDictionary(newDia, dataDictionary);
    }

    public static DataFlowDiagramAndDictionary chooseExternalScenario(DataFlowDiagram dataFlowDiagram, DataDictionary dataDictionary,
            DFDExternalUncertaintySource source, DFDExternalUncertaintyScenario scenario) {
        var newDia = (DataFlowDiagram) EcoreUtil.copy(dataFlowDiagram);

        // Extract Labels of default scenario (oldLabels) and of given scenario
        // (newLabels)
        var newNodeLabels = scenario.getTargetProperties();
        var oldNodeLabels = source.getTargetProperties();
        var targetNode = source.getTarget();

        // Remove Labels from default scenario and add Labels of given scenario
        var ddTargetNodes = newDia.getNodes()
                .stream()
                .filter(n -> n.getId()
                        .equals(targetNode.getId()))
                .toList();
        if (ddTargetNodes.size() == 0) {
            System.out.println(targetNode.getEntityName());
            return new DataFlowDiagramAndDictionary(newDia, dataDictionary);
        }

        ddTargetNodes.get(0)
                .getProperties()
                .removeAll(oldNodeLabels);
        ddTargetNodes.get(0)
                .getProperties()
                .addAll(newNodeLabels);

        return new DataFlowDiagramAndDictionary(newDia, dataDictionary);
    }

    public static DataFlowDiagramAndDictionary chooseComponentScenario(DataFlowDiagram dataFlowDiagram, DataDictionary dataDictionary,
            DFDComponentUncertaintySource source, DFDComponentUncertaintyScenario scenario) {
        var newDia = (DataFlowDiagram) EcoreUtil.copy(dataFlowDiagram);
        var newDD = (DataDictionary) EcoreUtil.copy(dataDictionary);

        // Extract Node from default and given scenario
        var oldNode = newDia.getNodes()
                .stream()
                .filter(n -> n.getId()
                        .equals(source.getTarget()
                                .getId()))
                .toList()
                .get(0);
        var oldNodeBehaviorId = oldNode.getBehavior()
                .getId();
        var newNode = newDia.getNodes()
                .stream()
                .filter(n -> n.getId()
                        .equals(scenario.getTarget()
                                .getId()))
                .toList()
                .get(0);
        var newNodeBehaviorId = newNode.getBehavior()
                .getId();

        // Replace oldNode with newNode in all Flows
        for (var flow : newDia.getFlows()) {
            if (flow.getDestinationNode()
                    .getId()
                    .equals(oldNode.getId())) {
                flow.setDestinationNode(newNode);
            }
            if (flow.getSourceNode()
                    .getId()
                    .equals(oldNode.getId())) {
                flow.setSourceNode(newNode);
            }
        }
        // Remove old Node
        newDia.getNodes()
                .removeIf(n -> n.getId()
                        .equals(oldNode.getId()));

        Behavior newNodeNewBehavior = newDD.getBehavior()
                .stream()
                .filter(b -> b.getId()
                        .equals(newNodeBehaviorId))
                .toList()
                .get(0);
        Behavior oldNodeOldBehavior = newDD.getBehavior()
                .stream()
                .filter(b -> b.getId()
                        .equals(oldNodeBehaviorId))
                .toList()
                .get(0);
        newNodeNewBehavior.getInPin()
                .addAll(oldNodeOldBehavior.getInPin());
        newNodeNewBehavior.getOutPin()
                .addAll(oldNodeOldBehavior.getOutPin());
        newDD.getBehavior()
                .remove(oldNodeOldBehavior);
        newNode.setBehavior(newNodeNewBehavior);

        // removeInPinsThatDontOccurInFlows(newDD, newDia);
        replaceOldDDReferencesWithTheOnesFromNewDD(newDD, newDia);

        return new DataFlowDiagramAndDictionary(newDia, newDD);
    }

    private static void replaceOldDDReferencesWithTheOnesFromNewDD(DataDictionary newDD, DataFlowDiagram newDia) {
        // Replace Behaviors and Properties
        for (var node : newDia.getNodes()) {
            var nodeBehaviorId = node.getBehavior()
                    .getId();
            var nodeBehaviorInDD = newDD.getBehavior()
                    .stream()
                    .filter(b -> b.getId()
                            .equals(nodeBehaviorId))
                    .toList()
                    .get(0);
            node.setBehavior(nodeBehaviorInDD);
            var nodePropertyIds = node.getProperties()
                    .stream()
                    .map(p -> p.getId())
                    .toList();
            node.getProperties()
                    .clear();
            for (var npid : nodePropertyIds) {
                var newProp = newDD.getLabelTypes()
                        .stream()
                        .map(l -> l.getLabel())
                        .flatMap(List::stream)
                        .filter(l -> l.getId()
                                .equals(npid))
                        .toList()
                        .get(0);
                node.getProperties()
                        .add(newProp);
            }
        }
        // Replace pins
        List<Flow> flowsToRemove = new ArrayList<Flow>();
        for (var flow : newDia.getFlows()) {
            var srcPinId = flow.getSourcePin()
                    .getId();
            var dstPinId = flow.getDestinationPin()
                    .getId();
            var newSrcPin = newDD.getBehavior()
                    .stream()
                    .map(b -> b.getOutPin())
                    .flatMap(List::stream)
                    .filter(p -> p.getId() != null)
                    .filter(p -> p.getId()
                            .equals(srcPinId))
                    .findFirst();
            var newDstPin = newDD.getBehavior()
                    .stream()
                    .map(b -> b.getInPin())
                    .flatMap(List::stream)
                    .filter(p -> p.getId() != null)
                    .filter(p -> p.getId()
                            .equals(dstPinId))
                    .findFirst();
            if (newDstPin.isPresent() && newSrcPin.isPresent()) {
                flow.setSourcePin(newSrcPin.get());
                flow.setDestinationPin(newDstPin.get());
            } else {
                flowsToRemove.add(flow);
            }
        }
        newDia.getFlows()
                .removeAll(flowsToRemove);
    }

    public static DataFlowDiagramAndDictionary chooseConnectorScenario(DataFlowDiagram dataFlowDiagram, DataDictionary dataDictionary,
            DFDConnectorUncertaintySource source, DFDConnectorUncertaintyScenario scenario) {
        var newDia = (DataFlowDiagram) EcoreUtil.copy(dataFlowDiagram);
        var newDD = (DataDictionary) EcoreUtil.copy(dataDictionary);

        // Extract fields for replacements
        var targetFlow = source.getTargetFlow();
        var newDestinationNode = scenario.getTargetNode();
        var newDestinationPin = scenario.getTargetPin();
        var oldAssignmentsIds = source.getTargetAssignments()
                .stream()
                .map(a -> a.getId())
                .toList();
        var newAssignmentsIds = scenario.getTargetAssignments()
                .stream()
                .map(a -> a.getId())
                .toList();
        var newAssignments = newDD.getBehavior()
                .stream()
                .map(b -> b.getAssignment())
                .flatMap(List::stream)
                .filter(a -> newAssignmentsIds.contains(a.getId()))
                .toList();

        // Set destination node and pin to the ones in the given scenario
        var ddTargetFlow = newDia.getFlows()
                .stream()
                .filter(f -> f.getId()
                        .equals(targetFlow.getId()))
                .toList()
                .get(0);
        ddTargetFlow.setDestinationNode(newDestinationNode);
        ddTargetFlow.setDestinationPin(newDestinationPin);

        replaceAssignments(newDD, oldAssignmentsIds, newAssignmentsIds, newAssignments);

        return new DataFlowDiagramAndDictionary(newDia, newDD);
    }

    private static void replaceAssignments(DataDictionary newDD, List<String> oldAssignmentsIds, List<String> newAssignmentsIds,
            List<AbstractAssignment> newAssignments) {

        for (var behavior : newDD.getBehavior()) {
            var addNewAssignments = false;
            List<AbstractAssignment> assignmentsToRemove = new ArrayList<>();
            for (var assignment : behavior.getAssignment()) {
                if (oldAssignmentsIds.contains(assignment.getId())) {
                    assignmentsToRemove.add(assignment);
                    addNewAssignments = true;
                }
                behavior.getAssignment()
                        .removeAll(assignmentsToRemove);
                if (addNewAssignments) {
                    behavior.getAssignment()
                            .addAll(newAssignments);
                }
            }
        }
    }

    /**
     * Replaces pins of newAssignments with the ones in the old assigments (matching them by entity name)
     **/
    private static void correctNewAssignmentsPins(List<AbstractAssignment> oldAssignments, List<AbstractAssignment> newAssignments) {
        // Create dict which will be used to adapt the pins of the
        // new assignments to the ones given in the targetAssignment
        Map<String, String> inPinEntityNameToInPinId = new HashMap<>();

        for (AbstractAssignment assignment : oldAssignments) {
            if (assignment instanceof ForwardingAssignment) {
                ForwardingAssignment forwardingAssignment = (ForwardingAssignment) assignment;
                for (var inPin : forwardingAssignment.getInputPins()) {
                    inPinEntityNameToInPinId.put(inPin.getEntityName(), inPin.getId());
                }
                var outPin = assignment.getOutputPin();
                inPinEntityNameToInPinId.put(outPin.getEntityName(), outPin.getId());
            } else if (assignment instanceof Assignment) {
                Assignment resolvedAssignment = (Assignment) assignment;
                for (var inPin : resolvedAssignment.getInputPins()) {
                    inPinEntityNameToInPinId.put(inPin.getEntityName(), inPin.getId());
                }
                var outPin = assignment.getOutputPin();
                inPinEntityNameToInPinId.put(outPin.getEntityName(), outPin.getId());
            }

        }

        for (var assignment : newAssignments) {
            if (assignment instanceof Assignment) {
                Assignment resolvedAssignment = (Assignment) assignment;
                for (var pin : resolvedAssignment.getInputPins()) {
                    pin.setId(inPinEntityNameToInPinId.get(pin.getEntityName()));
                }
                var outputPin = assignment.getOutputPin();
                outputPin.setId(inPinEntityNameToInPinId.get(outputPin.getEntityName()));
            }

            else if (assignment instanceof ForwardingAssignment) {
                ForwardingAssignment forwardingAssignment = (ForwardingAssignment) assignment;
                for (var pin : forwardingAssignment.getInputPins()) {
                    pin.setId(inPinEntityNameToInPinId.get(pin.getEntityName()));
                }
                var outputPin = assignment.getOutputPin();
                outputPin.setId(inPinEntityNameToInPinId.get(outputPin.getEntityName()));
            }
        }
    }

}
