package dev.arcovia.mitigation.evaluation.tests;

import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;

import dev.arcovia.mitigation.sat.MitigationApproach;
import dev.arcovia.mitigation.smt.Mechanic;

public class smtTest extends TestBase {

	@Override
	protected MitigationApproach getApproach(DataFlowDiagramAndDictionary dfd, List<AnalysisConstraint> constraints) {
		return new Mechanic(dfd, constraints);
	}

	@Override
	protected String getApproachName() {
		return "SMT";
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
