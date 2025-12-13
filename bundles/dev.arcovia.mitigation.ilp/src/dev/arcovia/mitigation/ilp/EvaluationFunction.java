package dev.arcovia.mitigation.ilp;

import java.util.Set;

import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;

public interface EvaluationFunction {
    /***
     * This function determines all Nodes violating the Constraint
     * @param flowGraph
     * @return Set of Nodes that violate that specific constraint
     */
	Set<Node> evaluate(DFDFlowGraphCollection flowGraph);
	/***
	 * isMathced determines if the provided node matches the Antecedent of the constraint
	 * @param vertex
	 * @return
	 */
	boolean isMatched(DFDVertex vertex);
}
