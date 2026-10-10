package dev.arcovia.mitigation.uncertainty.materialization;

import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.abunai.confidentiality.analysis.model.uncertainty.dfd.DFDBehaviorUncertaintySource;
import dev.arcovia.mitigation.cost.ActionType;
import dev.arcovia.mitigation.ilp.ActionTerm;
import dev.arcovia.mitigation.sat.LabelCategory;
import org.dataflowanalysis.dfd.datadictionary.*;
import org.eclipse.jdt.annotation.NonNull;

import java.util.*;

/**
 * Completes outgoing-data label actions on the assignments a behavior source can swap in.
 * <p>
 * Makes sure a repair label sticks to a node's output pin in every scenario.
 */
public final class PinWideLabelApplication {

    private PinWideLabelApplication() {
    }

    /**
     * Applies the outgoing-data label actions to the behavior alternatives producing their pin.
     *
     * @param dictionary the dictionary of the model the actions were already applied to
     * @param actions    the applied actions; those that are not outgoing-data label actions are ignored
     * @param sources    the selected sources; only behavior sources contribute alternatives
     */
    public static void complete(@NonNull DataDictionary dictionary, @NonNull List<ActionTerm> actions,
                                @NonNull List<UncertaintySource> sources) {
        Objects.requireNonNull(dictionary, "dictionary must not be null");
        Objects.requireNonNull(actions, "actions must not be null");
        Objects.requireNonNull(sources, "sources must not be null");
        Set<String> alternatives = alternativeAssignmentIds(sources);
        if (alternatives.isEmpty()) {
            return;
        }
        for (ActionTerm action : actions) {
            if (action.compositeLabels() == null || action.compositeLabels().isEmpty()
                || action.compositeLabels().get(0).category() != LabelCategory.OutgoingData
                || (action.type() != ActionType.Adding && action.type() != ActionType.Removing)) {
                continue;
            }
            Optional<Label> label = findLabel(dictionary, action.compositeLabels().get(0).label().type(),
                    action.compositeLabels().get(0).label().value());
            if (label.isEmpty()) {
                continue;
            }
            dictionary.getBehavior().stream()
                    .flatMap(behavior -> behavior.getAssignment().stream())
                    .filter(assignment -> alternatives.contains(assignment.getId()))
                    .filter(assignment -> assignment.getOutputPin() != null
                                          && action.domain().equals(assignment.getOutputPin().getId()))
                    .forEach(assignment -> apply(assignment, label.get(), action.type() == ActionType.Adding));
        }
    }

    private static Set<String> alternativeAssignmentIds(List<UncertaintySource> sources) {
        Set<String> ids = new HashSet<>();
        sources.stream()
                .filter(source -> source instanceof DFDBehaviorUncertaintySource)
                .map(source -> (DFDBehaviorUncertaintySource) source)
                .forEach(behaviorSource -> behaviorSource.getScenarios().stream()
                        .filter(Objects::nonNull)
                        .forEach(behaviorScenario -> behaviorScenario.getTargetAssignments()
                                .forEach(assignment -> ids.add(assignment.getId())))
                );
        return ids;
    }

    private static void apply(AbstractAssignment assignment, Label label, boolean adding) {
        List<Label> labels;
        if (assignment instanceof Assignment cast) {
            labels = cast.getOutputLabels();
        } else if (assignment instanceof SetAssignment cast) {
            labels = cast.getOutputLabels();
        } else {
            // A forwarding alternative carries no labels of its own; nothing to complete.
            return;
        }
        if (adding && !labels.contains(label)) {
            labels.add(label);
        } else if (!adding) {
            labels.remove(label);
        }
    }

    private static Optional<Label> findLabel(DataDictionary dictionary, String type, String value) {
        return dictionary.getLabelTypes().stream()
                .filter(labelType -> type.equals(labelType.getEntityName()))
                .flatMap(labelType -> labelType.getLabel().stream())
                .filter(label -> value.equals(label.getEntityName()))
                .findFirst();
    }
}
