package dev.arcovia.mitigation.smt.tests.preprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.dataflowanalysis.analysis.dfd.DFDConfidentialityAnalysis;
import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.core.DFDTransposeFlowGraph;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.TFGFlow;
import dev.arcovia.mitigation.smt.preprocess.Preprocess;
import dev.arcovia.mitigation.smt.preprocess.PreprocessingResult;
import dev.arcovia.mitigation.smt.tests.evaluation.ConstraintMapProvider;
import dev.arcovia.mitigation.smt.util.Util;

public class PreprocessTest {

    @Test
    public void testPreprocess() throws Exception {
        var tuhhModels = TuhhModels.getTuhhModels();
        Map<Integer, List<AnalysisConstraint>> constraintMap = ConstraintMapProvider.buildConstraintMap();
        for (var model : tuhhModels.keySet()) {
            if (!tuhhModels.get(model)
                    .contains(0))
                continue;
            for (int i : List.of(1, 2, 4, 5, 7, 8, 10, 11)) {
                List<AnalysisConstraint> constraint = constraintMap.get(i);
                if (constraint == null) {
                    System.out.println("Skipping " + model + " with constraint " + i + " because Constraint is undefined");
                    continue;
                } else if (!tuhhModels.get(model)
                        .contains(i)) {
                    System.out.println("Skipping " + model + " with constraint " + i + " because no model for this constraint is defined");
                    continue;
                }
                DataFlowDiagramAndDictionary dfdAndDD = Util.loadDFD(model, model + "_" + i);
                Preprocess pre = new Preprocess();
                PreprocessingResult preprocessingResult = pre.preprocess(dfdAndDD, constraint, false);

                DFDModelResourceProvider dfdModelResourceProvider = new DFDModelResourceProvider(preprocessingResult.dfd()
                        .dataDictionary(),
                        preprocessingResult.dfd()
                                .dataFlowDiagram());
                DFDConfidentialityAnalysis dfdConfidentialityAnalysis = new DFDDataFlowAnalysisBuilder().standalone()
                        .useCustomResourceProvider(dfdModelResourceProvider)
                        .build();
                DFDFlowGraphCollection flowGraphs = dfdConfidentialityAnalysis.findFlowGraphs();

                Set<DFDTransposeFlowGraph> tfgs = flowGraphs.getTransposeFlowGraphs()
                        .stream()
                        .filter(DFDTransposeFlowGraph.class::isInstance)
                        .map(DFDTransposeFlowGraph.class::cast)
                        .collect(Collectors.toSet());

                // Assert vertex numbers are the same
                Set<DFDVertex> expectedVertices = tfgs.stream()
                        .flatMap(x -> x.getVertices()
                                .stream())
                        .filter(DFDVertex.class::isInstance)
                        .map(DFDVertex.class::cast)
                        .collect(Collectors.toSet());
                Set<DFDVertex> actualVertices = preprocessingResult.vertices();
                assertEquals(expectedVertices.size(), actualVertices.size());

                // Assert flow numbers are as expected. TFG with n nodes contains n-1 flows
                int numFlows = tfgs.stream()
                        .mapToInt(x -> x.getVertices()
                                .size() - 1)
                        .sum();
                assertEquals(numFlows, preprocessingResult.flows()
                        .size());

                Map<DFDVertex, List<TFGFlow>> incomingFlowMap = preprocessingResult.vertexIncomingFlows();
                for (Entry<DFDVertex, List<TFGFlow>> entry : incomingFlowMap.entrySet()) {
                    for (TFGFlow flow : entry.getValue()) {
                        // Assert each flow actually flows to correct vertex
                        assertEquals(flow.getDstVertex(), entry.getKey());
                        // Destination pin of flow is present at node
                        assertTrue(entry.getKey()
                                .getPinFlowMap()
                                .keySet()
                                .contains(flow.getDstPin()));
                        // Flow actually flows from preceeding vertex
                        assertTrue(entry.getKey()
                                .getPreviousElements()
                                .contains(flow.getSrcVertex()));
                        // Source vertex of flow actually contains source pin of flows
                        assertTrue(flow.getSrcVertex()
                                .getReferencedElement()
                                .getBehavior()
                                .getOutPin()
                                .contains(flow.getSrcPin()));
                        for (Entry<Assignment, List<TFGFlow>> assigns : flow.getThisFlowEvaluatesOn()
                                .entrySet()) {
                            // Source pin of flow actually contains expected assignment
                            assertEquals(assigns.getKey()
                                    .getOutputPin(), flow.getSrcPin());
                            for (TFGFlow prev : assigns.getValue()) {
                                // Evaluated flow actually flows to vertex of our node
                                assertEquals(prev.getDstVertex(), flow.getSrcVertex());
                            }
                        }
                        for (Entry<ForwardingAssignment, List<TFGFlow>> forwards : flow.getThisFlowForwards()
                                .entrySet()) {
                            // Source pin of flow actually contains expected forward
                            assertEquals(forwards.getKey()
                                    .getOutputPin(), flow.getSrcPin());
                            for (TFGFlow prev : forwards.getValue()) {
                                // Forwarded flow actually flows to vertex of our node
                                assertEquals(prev.getDstVertex(), flow.getSrcVertex());
                            }
                        }

                    }
                }
                // Assert labels get properly extracted from dfd
                assertEquals(preprocessingResult.relevantDataLabelsAdd(), Util.getRelevantDataLabelsAdd(preprocessingResult.dfd()
                        .dataDictionary(), constraint));
                assertEquals(preprocessingResult.relevantDataLabelsRemove(), Util.getRelevantDataLabelsRemove(preprocessingResult.dfd()
                        .dataDictionary(), constraint));
                assertEquals(preprocessingResult.relevantNodeLabelsAdd(), Util.getRelevantNodeLabelsAdd(preprocessingResult.dfd()
                        .dataDictionary(), constraint));
                assertEquals(preprocessingResult.relevantNodeLabelsRemove(), Util.getRelevantNodeLabelsRemove(preprocessingResult.dfd()
                        .dataDictionary(), constraint));
            }
        }

    }

}
