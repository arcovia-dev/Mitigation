package dev.arcovia.mitigation.smt.tests.evaluation;

import java.util.List;

import org.dataflowanalysis.analysis.dfd.DFDConfidentialityAnalysis;
import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.Mitigation;
import dev.arcovia.mitigation.smt.util.Util;

public class SpecificTest {

    @Test
    public void newTest() throws Exception {
        DataFlowDiagramAndDictionary dfd = Util.loadDFD("koushikkothagal", "koushikkothagal_0");
        List<AnalysisConstraint> constraints = ConstraintMapProvider.getOrThrow(4);

        System.out.println(Util.countViolations(dfd, constraints));
        System.out.println(Mitigation.run(dfd, constraints, null));
    }

    /**
     * Finds cyclic DFDs from the TUHH set.
     */
    public void test() throws Exception {
        List<EvaluationSupport.Configuration> configs = EvaluationSupport.configurations();
        for (EvaluationSupport.Configuration cfg : configs) {
            DataFlowDiagramAndDictionary base = Util.loadDFD(cfg.model(), cfg.model() + "_0");
            DFDModelResourceProvider dfdModelResourceProvider = new DFDModelResourceProvider(base.dataDictionary(), base.dataFlowDiagram());
            DFDConfidentialityAnalysis dfdConfidentialityAnalysis = new DFDDataFlowAnalysisBuilder().standalone()
                    .useCustomResourceProvider(dfdModelResourceProvider)
                    .build();
            DFDFlowGraphCollection flowGraphs = dfdConfidentialityAnalysis.findFlowGraphs();
            if (flowGraphs.wasCyclic()) {
                System.out.println(cfg.model());
            }
        }
    }

}
