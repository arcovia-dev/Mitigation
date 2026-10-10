package dev.arcovia.mitigation.uncertainty.materialization;

import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintyScenario;
import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.*;
import dev.arcovia.mitigation.ranking.UncertaintySourceMitigationUtils;
import dev.arcovia.mitigation.uncertainty.enumeration.ScenarioSelection;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Behavior;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jdt.annotation.NonNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Materializes a source selection into a concrete DFD. Pipeline stage 3.
 */
public final class ScenarioMaterializer {

    /**
     * Applies the selected alternatives to a base model.
     *
     * @param baseModel  the model to materialize
     * @param selections the selections to apply
     * @return the identified materialized scenario
     * @throws IllegalArgumentException if a selection has an unsupported type
     */
    public MaterializedScenario materialize(
            @NonNull DataFlowDiagramAndDictionary baseModel,
            @NonNull List<ScenarioSelection> selections
    ) {
        Objects.requireNonNull(baseModel, "baseModel must not be null");
        Objects.requireNonNull(selections, "selections must not be null");

        var currentModel = baseModel;
        var appliedSelections = new ArrayList<ScenarioSelection>();

        for (var selection : selections) {
            Objects.requireNonNull(selection, "selection must not be null");
            appliedSelections.add(selection);

            if (selection.scenario().isEmpty()) {
                continue;
            }

            currentModel = applyScenario(currentModel, selection.source(), selection.scenario().get());
        }

        var id = appliedSelections.stream()
                .map(this::selectionId)
                .collect(Collectors.joining("__"));

        return new MaterializedScenario(id, currentModel, appliedSelections);
    }

    /**
     * Applies one alternative scenario to the current model.
     *
     * @param current  the model before the alternative is applied
     * @param source   the source that declares the alternative
     * @param scenario the alternative to apply
     * @return the resulting model
     * @throws IllegalArgumentException if the source and scenario types do not match a supported pair
     */
    private DataFlowDiagramAndDictionary applyScenario(
            DataFlowDiagramAndDictionary current,
            UncertaintySource source,
            UncertaintyScenario scenario
    ) {
        if (scenario instanceof DFDExternalUncertaintyScenario externalScenario
            && source instanceof DFDExternalUncertaintySource externalSource) {
            return chooseExternalScenario(current, externalSource, externalScenario);
        }

        if (scenario instanceof DFDBehaviorUncertaintyScenario behaviorScenario
            && source instanceof DFDBehaviorUncertaintySource behaviorSource) {
            return UncertaintySourceMitigationUtils.chooseBehaviorScenario(
                    current.dataFlowDiagram(), current.dataDictionary(), behaviorSource, behaviorScenario);
        }

        if (scenario instanceof DFDInterfaceUncertaintyScenario interfaceScenario
            && source instanceof DFDInterfaceUncertaintySource interfaceSource) {
            return rebindById(UncertaintySourceMitigationUtils.chooseInterfaceScenario(
                    current.dataFlowDiagram(), current.dataDictionary(), interfaceSource, interfaceScenario));
        }

        if (scenario instanceof DFDComponentUncertaintyScenario componentScenario
            && source instanceof DFDComponentUncertaintySource componentSource) {
            return UncertaintySourceMitigationUtils.chooseComponentScenario(
                    current.dataFlowDiagram(), current.dataDictionary(), componentSource, componentScenario);
        }

        if (scenario instanceof DFDConnectorUncertaintyScenario connectorScenario
            && source instanceof DFDConnectorUncertaintySource connectorSource) {
            return rebindById(UncertaintySourceMitigationUtils.chooseConnectorScenario(
                    current.dataFlowDiagram(), current.dataDictionary(), connectorSource, connectorScenario));
        }

        throw new IllegalArgumentException("Unsupported uncertainty scenario type: " + scenario.getClass().getName());
    }

    /**
     * Replaces the source's default node labels with the scenario's labels, matching both by id.
     *
     * @param current  the model before the alternative is applied
     * @param source   the external source naming the target node and its default labels
     * @param scenario the alternative naming the replacement labels
     * @return a model whose diagram is a copy of {@code current}'s and whose dictionary is {@code current}'s;
     * unchanged when the target node no longer exists
     * @throws IllegalStateException if a scenario label does not exist in {@code current}'s dictionary
     */
    private static DataFlowDiagramAndDictionary chooseExternalScenario(DataFlowDiagramAndDictionary current,
                                                                       DFDExternalUncertaintySource source,
                                                                       DFDExternalUncertaintyScenario scenario) {
        var diagram = EcoreUtil.copy(current.dataFlowDiagram());
        var labels = labelsById(current);
        var target = diagram.getNodes().stream()
                .filter(node -> node.getId().equals(source.getTarget().getId()))
                .findFirst()
                .orElse(null);

        if (target == null) {
            return new DataFlowDiagramAndDictionary(diagram, current.dataDictionary());
        }

        var defaultIds = source.getTargetProperties().stream().map(Label::getId).collect(Collectors.toSet());
        target.getProperties().removeIf(label -> defaultIds.contains(label.getId()));
        scenario.getTargetProperties().forEach(alternative -> {
            var resolved = labels.get(alternative.getId());
            if (resolved == null) {
                throw new IllegalStateException("Scenario " + scenario.getEntityName()
                                                + " sets a label absent from the data dictionary: " + alternative.getId());
            }
            if (!target.getProperties().contains(resolved)) {
                target.getProperties().add(resolved);
            }
        });
        return new DataFlowDiagramAndDictionary(diagram, current.dataDictionary());
    }

    /**
     * Points every node behavior, node property, flow endpoint, and flow pin of the diagram at the
     * element with the same id in the returned model.
     *
     * @param model the materialized model to repair in place
     * @return {@code model}
     */
    private static DataFlowDiagramAndDictionary rebindById(DataFlowDiagramAndDictionary model) {
        var dictionary = model.dataDictionary();
        var nodes = byId(model.dataFlowDiagram().getNodes(), Node::getId);
        var behaviors = byId(dictionary.getBehavior(), Behavior::getId);
        var labels = labelsById(model);
        var inPins = byId(dictionary.getBehavior().stream()
                .flatMap(behavior -> behavior.getInPin().stream()).toList(), Pin::getId);
        var outPins = byId(dictionary.getBehavior().stream()
                .flatMap(behavior -> behavior.getOutPin().stream()).toList(), Pin::getId);

        model.dataFlowDiagram().getNodes().forEach(node -> {
            if (node.getBehavior() != null) {
                node.setBehavior(behaviors.getOrDefault(node.getBehavior().getId(), node.getBehavior()));
            }

            var properties = node.getProperties().stream()
                    .map(label -> labels.getOrDefault(label.getId(), label))
                    .distinct()
                    .toList();
            node.getProperties().clear();
            node.getProperties().addAll(properties);
        });

        model.dataFlowDiagram().getFlows().forEach(flow -> {
            flow.setSourceNode(nodes.getOrDefault(flow.getSourceNode().getId(), flow.getSourceNode()));
            flow.setDestinationNode(nodes.getOrDefault(flow.getDestinationNode().getId(), flow.getDestinationNode()));
            flow.setSourcePin(outPins.getOrDefault(flow.getSourcePin().getId(), flow.getSourcePin()));
            flow.setDestinationPin(inPins.getOrDefault(flow.getDestinationPin().getId(), flow.getDestinationPin()));
        });
        return model;
    }

    private static Map<String, Label> labelsById(DataFlowDiagramAndDictionary model) {
        return byId(model.dataDictionary().getLabelTypes().stream()
                .flatMap(type -> type.getLabel().stream()).toList(), Label::getId);
    }

    private static <T> Map<String, T> byId(List<T> elements, Function<T, String> id) {
        Map<String, T> index = new LinkedHashMap<>();
        elements.stream()
                .filter(element -> id.apply(element) != null)
                .forEach(element -> index.putIfAbsent(id.apply(element), element));
        return index;
    }

    private String selectionId(ScenarioSelection selection) {
        return selection.scenario()
                .map(uncertaintyScenario -> selection.source().getEntityName() + ":" + uncertaintyScenario.getId())
                .orElseGet(() -> selection.source().getEntityName() + ":default");
    }
}
