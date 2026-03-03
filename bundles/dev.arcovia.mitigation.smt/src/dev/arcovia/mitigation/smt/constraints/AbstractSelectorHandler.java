package dev.arcovia.mitigation.smt.constraints;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.AbstractSelector;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;

/**
 * Base class for selector Encoding
 * @param <T> A concrete DSL Selector
 */
abstract class AbstractSelectorHandler<T extends AbstractSelector> {
    /**
     * Encodes the selector for a Vertex into a BoolExpr
     * @param selector DSL Selector
     * @param vertex The vertex that this selector will be encoded for
     * @param smt SMT object for access to required Encoding objects
     * @return BoolExpr that encodes, whether the specified vertex matches the selector
     */
    abstract protected BoolExpr encode(T selector, DFDVertex vertex, SMT smt);
}
