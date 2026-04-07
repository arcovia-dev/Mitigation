package dev.arcovia.mitigation.smt.operations;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;

/**
 * Represents an Action that can be executed on a DFD to modify it.
 */
public interface Operation {

    /**
     * Applies this action to the DFD
     * @param dfd incoming dfd
     * @return resulting dfd
     */
    abstract DataFlowDiagramAndDictionary doOperation(DataFlowDiagramAndDictionary dfd);

    /**
     * Reverts the changes of this action on the DFD
     * @param dfd incoming dfd
     * @return resulting dfd
     */
    abstract DataFlowDiagramAndDictionary undoOperation(DataFlowDiagramAndDictionary dfd);
}
