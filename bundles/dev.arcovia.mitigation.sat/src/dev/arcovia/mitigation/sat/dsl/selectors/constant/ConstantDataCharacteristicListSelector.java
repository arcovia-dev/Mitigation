package dev.arcovia.mitigation.sat.dsl.selectors.constant;

import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;
import dev.arcovia.mitigation.sat.dsl.nodes.ConjunctionNode;
import dev.arcovia.mitigation.sat.dsl.nodes.DisjunctionNode;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicListSelector;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicsSelector;

public class ConstantDataCharacteristicListSelector implements ConstantDataSelector {
    private final DataCharacteristicListSelector selector;

    /**
     * Constructs a {@link ConstantDataCharacteristicListSelector} wrapping the given {@link DataCharacteristicListSelector}.
     *
     * @param selector the {@link DataCharacteristicListSelector} to wrap
     */
    public ConstantDataCharacteristicListSelector(DataCharacteristicListSelector selector) {
        this.selector = selector;
    }

    /**
     * Adds literals to the root node based on the data characteristics of this selector.
     * Creates a conjunction or disjunction node depending on the inversion flag, and adds
     * corresponding constant data characteristic selectors as predicates.
     *
     * @param root the {@link BranchNode} to which literals are added
     */
    @Override
    public void addLiterals(BranchNode root) {
        var incomingDataNode = selector.isInverted() ? new ConjunctionNode() : new DisjunctionNode();
        root.addPredicate(incomingDataNode);

        var dataSelectors = selector.getDataCharacteristics().stream().map(it ->
                new ConstantDataCharacteristicSelector(
                        new DataCharacteristicsSelector(null, it, selector.isInverted())
                )).toList();
        dataSelectors.forEach(it -> it.addLiterals(incomingDataNode));
    }
}