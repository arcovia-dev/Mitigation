package dev.arcovia.mitigation.smt.constraints;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.AbstractSelector;

import com.microsoft.z3.BoolExpr;

/**
 * Interface for selector translation
 */
public interface SelectorTranslator {
    /**
     * Encodes the matching logic for the given Selector on this Vertex
     * @param selector Input Selector
     * @param vertex Input Vertex
     * @param role Role of the selector
     * @return Expression that encodes whether the vertex matches the selector
     */
    public BoolExpr toBool(AbstractSelector selector, DFDVertex vertex);
}
