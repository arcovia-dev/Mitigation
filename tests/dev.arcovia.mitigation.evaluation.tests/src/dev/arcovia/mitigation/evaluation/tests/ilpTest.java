package dev.arcovia.mitigation.evaluation.tests;

import java.util.List;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import dev.arcovia.mitigation.ilp.OptimizationManager;
import dev.arcovia.mitigation.sat.MitigationApproach;

public class ilpTest extends TestBase {

	@Override
	protected MitigationApproach getApproach(DataFlowDiagramAndDictionary dfd, List<AnalysisConstraint> constraints) {
		return new OptimizationManager(dfd, constraints);
	}

	@Override
	protected String getApproachName() {
		return "ILP";
	}
	
	@Override
    protected List<Integer> getConstraintScaling() {
        return List.of(1, 2, 4, 6, 8, 10, 20, 30, 40, 50);
    }
	
}
