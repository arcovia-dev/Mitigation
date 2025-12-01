package dev.arcovia.mitigation.ilp;

import java.util.Set;

import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;

public interface EvaluationFunction {
	Set<Node> evaluate(DFDFlowGraphCollection flowGraph);
}
