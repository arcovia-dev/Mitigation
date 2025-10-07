package dev.arcovia.mitigation.sat.dsl.selectors.constant;

import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.sat.dsl.nodes.ConjunctionNode;
import dev.arcovia.mitigation.sat.dsl.nodes.DisjunctionNode;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsListSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsSelector;

public class ConstantVertexCharacteristicListSelector implements ConstantDataSelector {
    private final VertexCharacteristicsListSelector selector;

    /**
     * Constructs a {@link ConstantVertexCharacteristicListSelector} wrapping the given {@link VertexCharacteristicsListSelector}.
     *
     * @param selector the {@link VertexCharacteristicsListSelector} to wrap
     */
    public ConstantVertexCharacteristicListSelector(VertexCharacteristicsListSelector selector) {
        this.selector = selector;
    }

    /**
     * Adds literals to the root node based on the vertex characteristics of this selector.
     * Creates a conjunction or disjunction node depending on the inversion flag, and adds
     * corresponding constant vertex characteristic selectors as predicates.
     *
     * @param root the {@link BranchNode} to which literals are added
     */
    @Override
    public void addLiterals(BranchNode root) {
        var node = selector.isInverted() ? new ConjunctionNode() : new DisjunctionNode();
        root.addPredicate(node);
        selector.getVertexCharacteristics().forEach(it ->
                new ConstantVertexCharacteristicSelector(
                        new VertexCharacteristicsSelector(null, it, selector.isInverted())
                ).addLiterals(node));
    }
}