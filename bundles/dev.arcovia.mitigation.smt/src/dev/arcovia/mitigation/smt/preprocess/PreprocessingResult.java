package dev.arcovia.mitigation.smt.preprocess;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;

import dev.arcovia.mitigation.smt.TFGFlow;

/**
 * Record type that possesses all relevant preprocessing information.
 */
public record PreprocessingResult(DataFlowDiagramAndDictionary dfd, Set<TFGFlow> flows, Set<DFDVertex> vertices, Set<Label> relevantNodeLabelsAdd,
        Set<Label> relevantNodeLabelsRemove, Set<Label> relevantDataLabelsAdd, Set<Label> relevantDataLabelsRemove,
        Map<DFDVertex, List<TFGFlow>> vertexIncomingFlows, long findTFGsTime) {
}
