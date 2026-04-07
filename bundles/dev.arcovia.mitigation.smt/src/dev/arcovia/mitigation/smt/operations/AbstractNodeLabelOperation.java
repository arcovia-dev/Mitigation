package dev.arcovia.mitigation.smt.operations;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

/**
 * This operation modifies node labels.
 */
public abstract class AbstractNodeLabelOperation extends DataDictionaryOperation {

    protected Node node;
    protected Label label;

    /**
     * Constructor
     * @param node that should be affected
     * @param label that should be modified
     */
    public AbstractNodeLabelOperation(Node node, Label label) {
        this.node = node;
        this.label = label;
    }

    /**
     * This operation adds a node label
     */
    @Override
    public DataFlowDiagramAndDictionary doOperation(DataFlowDiagramAndDictionary dfd) {
        dfd.dataFlowDiagram()
                .getNodes()
                .stream()
                .filter(x -> x.equals(node))
                .forEach(x -> x.getProperties()
                        .add(label));
        return dfd;
    }

    /**
     * This operation removes a node label
     */
    @Override
    public DataFlowDiagramAndDictionary undoOperation(DataFlowDiagramAndDictionary dfd) {
        dfd.dataFlowDiagram()
                .getNodes()
                .stream()
                .filter(x -> x.equals(node))
                .forEach(x -> x.getProperties()
                        .remove(label));
        return dfd;
    }

}
