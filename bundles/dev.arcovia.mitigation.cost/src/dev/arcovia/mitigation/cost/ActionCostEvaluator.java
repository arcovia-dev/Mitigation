package dev.arcovia.mitigation.cost;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import tools.mdsd.modelingfoundations.identifier.Identifier;

import java.util.*;

/**
 * Derives deterministic cost descriptors from a canonical action and base model.
 */
public final class ActionCostEvaluator {

    /**
     * Evaluates one canonical action key under a declared cost specification.
     *
     * @param actionKey     the canonical action to price
     * @param specification the declared cost specification
     * @param baseModel     the unrepaired base model, or {@code null} for an action-only profile
     * @return the action's complete descriptor and direct cost
     */
    public ActionCostDescriptor evaluate(RepairActionKey actionKey, RepairCostSpecification specification,
                                         DataFlowDiagramAndDictionary baseModel) {
        Objects.requireNonNull(actionKey, "actionKey must not be null");
        Objects.requireNonNull(specification, "specification must not be null");
        double impact = impact(actionKey, baseModel, specification.impactCoefficient() > 0);
        double ripple = ripple(actionKey, baseModel, specification.rippleCoefficient() > 0);
        double structuralMagnitude = structuralMagnitude(actionKey);
        double assuranceActivities = specification.assuranceActivitiesFor(actionKey.actionType());
        double baseCost = specification.baseCostFor(actionKey.actionType());
        double impactCost = specification.impactCoefficient() * impact;
        double rippleCost = specification.rippleCoefficient() * ripple;
        double structuralCost = specification.structuralCoefficient() * structuralMagnitude;
        double assuranceCost = specification.assuranceCoefficient() * assuranceActivities;
        double formulaCost = baseCost + impactCost + rippleCost + structuralCost + assuranceCost;
        OptionalDouble override = specification.actionCostOverrideFor(actionKey);
        return new ActionCostDescriptor(actionKey, impact, ripple, structuralMagnitude, assuranceActivities,
                baseCost, impactCost, rippleCost, structuralCost, assuranceCost,
                override.orElse(formulaCost), override.isPresent());
    }

    /**
     * Evaluates all available repair actions.
     *
     * @param actionKeys    the actions to price
     * @param specification the declared cost specification
     * @param baseModel     the unrepaired base model, or {@code null} for an action-only profile
     * @return immutable descriptors indexed by canonical action key
     */
    public Map<RepairActionKey, ActionCostDescriptor> evaluateAll(
            java.util.Collection<RepairActionKey> actionKeys, RepairCostSpecification specification,
            DataFlowDiagramAndDictionary baseModel) {
        Objects.requireNonNull(actionKeys, "actionKeys must not be null");
        Map<RepairActionKey, ActionCostDescriptor> descriptors = new TreeMap<>();
        for (RepairActionKey actionKey : actionKeys) {
            descriptors.put(actionKey, evaluate(actionKey, specification, baseModel));
        }
        return Map.copyOf(descriptors);
    }

    /**
     * Counts the DFD elements and flows directly changed by an action.
     *
     * @param actionKey    the action to inspect
     * @param baseModel    the base model, or {@code null}
     * @param requireExact whether unavailable structural context must fail
     * @return the directly affected element and flow count
     */
    private double impact(RepairActionKey actionKey, DataFlowDiagramAndDictionary baseModel, boolean requireExact) {
        return switch (actionKey.actionType()) {
            case Adding, Removing, RemoveFlow -> 1;
            case AddNode, AddSink -> 2;
            case RemoveNode -> incidentFlowCount(actionKey, baseModel, requireExact) + 1;
        };
    }

    /**
     * Counts flows incident to a removed node.
     *
     * @param actionKey    the node-removal action
     * @param baseModel    the base model, or {@code null}
     * @param requireExact whether unavailable context must fail
     * @return the incident flow count
     */
    private double incidentFlowCount(RepairActionKey actionKey, DataFlowDiagramAndDictionary baseModel,
                                     boolean requireExact) {
        if (baseModel == null) {
            if (requireExact) {
                throw unavailable("Imp(a)", actionKey);
            }
            return 0;
        }
        boolean nodeExists = baseModel.dataFlowDiagram().getNodes().stream()
                .anyMatch(node -> actionKey.domain().equals(node.getId()));
        if (!nodeExists) {
            if (requireExact) {
                throw unavailable("Imp(a)", actionKey);
            }
            return 0;
        }
        return baseModel.dataFlowDiagram().getFlows().stream()
                .filter(flow -> actionKey.domain().equals(flow.getSourceNode().getId())
                                || actionKey.domain().equals(flow.getDestinationNode().getId()))
                .count();
    }

    /**
     * Counts the target's downstream reachability footprint in the base DFD.
     *
     * @param actionKey the action to inspect
     * @param baseModel the base model, or {@code null}
     * @param required  whether unavailable context must fail
     * @return the reachable node and flow count
     */
    private double ripple(RepairActionKey actionKey, DataFlowDiagramAndDictionary baseModel, boolean required) {
        if (baseModel == null) {
            if (required) {
                throw unavailable("Rip(a)", actionKey);
            }
            return 0;
        }
        Optional<String> startNodeId = startNodeId(actionKey, baseModel);
        if (startNodeId.isEmpty()) {
            if (required) {
                throw unavailable("Rip(a)", actionKey);
            }
            return 0;
        }
        Map<String, List<Flow>> outgoingFlows = outgoingFlows(baseModel);
        Set<String> visitedNodes = new HashSet<>();
        Set<String> visitedFlows = new HashSet<>();
        Deque<String> pendingNodes = new ArrayDeque<>();
        pendingNodes.add(startNodeId.get());

        while (!pendingNodes.isEmpty()) {
            String nodeId = pendingNodes.removeFirst();
            if (!visitedNodes.add(nodeId)) {
                continue;
            }
            for (Flow flow : outgoingFlows.getOrDefault(nodeId, List.of())) {
                visitedFlows.add(flow.getId());
                pendingNodes.addLast(flow.getDestinationNode().getId());
            }
        }
        return visitedNodes.size() + visitedFlows.size();
    }

    /**
     * Resolves the DFD node from which an action's ripple starts.
     *
     * @param actionKey the action to resolve
     * @param baseModel the base model
     * @return the start node identifier, when known
     */
    private Optional<String> startNodeId(RepairActionKey actionKey, DataFlowDiagramAndDictionary baseModel) {
        if (actionKey.actionType() == ActionType.Adding || actionKey.actionType() == ActionType.Removing
            || actionKey.actionType() == ActionType.AddSink || actionKey.actionType() == ActionType.RemoveNode) {
            Optional<String> nodeId = baseModel.dataFlowDiagram().getNodes().stream()
                    .map(Identifier::getId)
                    .filter(actionKey.domain()::equals)
                    .findFirst();
            if (nodeId.isPresent()) {
                return nodeId;
            }
        }
        return baseModel.dataFlowDiagram().getFlows().stream()
                .filter(flow -> actionKey.domain().equals(flow.getId())
                                || actionKey.domain().equals(flow.getSourcePin().getId()))
                .map(flow -> flow.getDestinationNode().getId())
                .findFirst();
    }

    /**
     * Groups model flows by source node identifier.
     *
     * @param baseModel the base model to inspect
     * @return outgoing flows by source node
     */
    private Map<String, List<Flow>> outgoingFlows(DataFlowDiagramAndDictionary baseModel) {
        Map<String, List<Flow>> flowsBySource = new HashMap<>();
        for (Flow flow : baseModel.dataFlowDiagram().getFlows()) {
            flowsBySource.computeIfAbsent(flow.getSourceNode().getId(), ignored -> new ArrayList<>()).add(flow);
        }
        return flowsBySource;
    }

    /**
     * Calculates the number of structural model edits in an action.
     *
     * @param actionKey the action to inspect
     * @return the structural change magnitude
     */
    private double structuralMagnitude(RepairActionKey actionKey) {
        return switch (actionKey.actionType()) {
            case Adding, Removing -> 0;
            case AddNode, AddSink -> 2;
            case RemoveNode, RemoveFlow -> 1;
        };
    }

    /**
     * Creates a clear unavailable-descriptor error.
     *
     * @param descriptor the descriptor that could not be derived
     * @param actionKey  the affected action
     * @return the exception to throw
     */
    private IllegalArgumentException unavailable(String descriptor, RepairActionKey actionKey) {
        return new IllegalArgumentException("Cannot calculate " + descriptor + " for action "
                                            + actionKey.stableId() + " from the base model");
    }
}
