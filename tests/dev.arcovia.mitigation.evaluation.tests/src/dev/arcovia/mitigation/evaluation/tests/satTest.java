package dev.arcovia.mitigation.evaluation.tests;

import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;

import dev.arcovia.mitigation.sat.Mechanic;
import dev.arcovia.mitigation.sat.Constraint;
import dev.arcovia.mitigation.sat.MitigationApproach;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;

public class satTest extends TestBase{

	@Override
	protected MitigationApproach getApproach(DataFlowDiagramAndDictionary dfd, List<AnalysisConstraint> constraints) {
		List<Constraint> translatedConstraints = new ArrayList<>();
		
		for (var constraint : constraints) {
            var translation = new CNFTranslation(constraint);
            Constraint translatedConstraint = translation.constructCNF()
                    .get(0);
            translatedConstraints.add(translatedConstraint);
        }
		
		return new Mechanic(dfd, " ",translatedConstraints);
	}

	@Override
	protected String getApproachName() {
		return "SAT";
	}

}
