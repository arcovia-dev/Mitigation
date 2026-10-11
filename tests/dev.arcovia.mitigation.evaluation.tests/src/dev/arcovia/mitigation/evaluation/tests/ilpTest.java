package dev.arcovia.mitigation.evaluation.tests;

import java.util.ArrayList;
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
	    List<Integer> evens = new ArrayList<>();
        for (int i = 2; i <= 100; i += 2) {
            evens.add(i);
        }
        return evens;
    }
	
}
