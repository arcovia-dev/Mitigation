package dev.arcovia.mitigation.smt.tests.smt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Test;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;
import dev.arcovia.mitigation.smt.TFGFlow;
import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.config.ConfigBuilder;
import dev.arcovia.mitigation.smt.preprocess.Preprocess;
import dev.arcovia.mitigation.smt.preprocess.PreprocessingResult;
import dev.arcovia.mitigation.smt.tests.evaluation.ConstraintMapProvider;
import dev.arcovia.mitigation.smt.util.Util;

public class SMTGetterTest {

    @Test
    public void testGetters() throws Exception {
        var tuhhModels = TuhhModels.getTuhhModels();

        Map<Integer, List<AnalysisConstraint>> constraintMap = ConstraintMapProvider.buildConstraintMap();
        for (var model : tuhhModels.keySet()) {
            if (!tuhhModels.get(model)
                    .contains(0))
                continue;
            for (int i : List.of(1, 2, 4, 5, 7, 8, 10, 11)) {
                List<AnalysisConstraint> constraint = constraintMap.get(i);
                if (constraint == null) {
                    continue;
                } else if (!tuhhModels.get(model)
                        .contains(i)) {
                    continue;
                }
                DataFlowDiagramAndDictionary dfd = Util.loadDFD(model, model + "_0");
                Preprocess preprocess = new Preprocess();
                PreprocessingResult pre = preprocess.preprocess(dfd, constraint, false);
                DataFlowDiagramAndDictionary dfdWithAddedLabels = pre.dfd();
                Config config = new ConfigBuilder().build();

                SMT smt = new SMT(pre, constraint, config);

                // SMT operates on data dictionary with added labels
                assertEquals(smt.getDataDictionary(), dfdWithAddedLabels.dataDictionary());

                Map<TFGFlow, Map<Label, BoolExpr>> flowLabels = smt.getFlowLabels();
                // If flow labels are encoded, every flow has labels. Otherwise none
                if (!(pre.relevantDataLabelsAdd()
                        .isEmpty()
                        && pre.relevantDataLabelsRemove()
                                .isEmpty())) {
                    assertEquals(flowLabels.keySet()
                            .size(),
                            pre.flows()
                                    .size());
                } else {
                    assertEquals(flowLabels.keySet()
                            .size(), 0);
                }
                // Correct data labels are encoded
                Set<Label> expectedDataLabels = new HashSet<>();
                expectedDataLabels.addAll(pre.relevantDataLabelsAdd());
                expectedDataLabels.addAll(pre.relevantDataLabelsRemove());
                for (Map<Label, BoolExpr> labels : flowLabels.values()) {
                    assertEquals(labels.keySet(), expectedDataLabels);
                }

                // Correct node labels are encoded
                Set<Label> expectedNodeLabels = new HashSet<>();
                expectedNodeLabels.addAll(pre.relevantNodeLabelsAdd());
                expectedNodeLabels.addAll(pre.relevantNodeLabelsRemove());
                Map<Node, Map<Label, BoolExpr>> nodeLabels = smt.getNodeLabels();
                if (!(pre.relevantNodeLabelsAdd()
                        .isEmpty()
                        && pre.relevantNodeLabelsRemove()
                                .isEmpty())) {
                    assertEquals(nodeLabels.keySet()
                            .size(),
                            dfdWithAddedLabels.dataFlowDiagram()
                                    .getNodes()
                                    .size());
                    for (Node n : dfdWithAddedLabels.dataFlowDiagram()
                            .getNodes()) {
                        Map<Label, BoolExpr> labels = nodeLabels.get(n);
                        assertEquals(expectedNodeLabels, labels.keySet());
                    }
                } else {
                    assertEquals(nodeLabels.keySet()
                            .size(), 0);
                }

                // Vertex incoming flows actually flow to vertex
                Map<DFDVertex, List<TFGFlow>> vertexIncomingFlows = smt.getVertexIncomingFlows();
                for (Entry<DFDVertex, List<TFGFlow>> entry : vertexIncomingFlows.entrySet()) {
                    for (TFGFlow flow : entry.getValue()) {
                        assertEquals(entry.getKey(), flow.getDstVertex());
                        assertTrue(entry.getKey()
                                .getReferencedElement()
                                .getBehavior()
                                .getInPin()
                                .contains(flow.getDstPin()));
                    }
                }
            }
        }
    }
}
