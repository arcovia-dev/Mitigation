package dev.arcovia.mitigation.smt.tests.evaluation;

import java.util.List;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;

import dev.arcovia.mitigation.smt.Mitigation;
import dev.arcovia.mitigation.smt.utils.ParsingUtils;

public class MemoryIsolatedRunner {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: MemoryIsolatedRunner <model> <constraintId>");
            System.exit(2);
        }

        String model = args[0];
        int variantId = Integer.parseInt(args[1]);

        List<AnalysisConstraint> constraints = ConstraintMapProvider.getOrThrow(variantId);

        DataFlowDiagramAndDictionary dfd = ParsingUtils.loadDFD(model, model + "_0");
        Mitigation.run(dfd, constraints, null);
    }
}
