package dev.arcovia.mitigation.evaluation.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.AfterAll;

import dev.arcovia.mitigation.sat.Mechanic;
import dev.arcovia.mitigation.sat.Constraint;
import dev.arcovia.mitigation.sat.MitigationApproach;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;

public class satTest extends TestBase {

	@Override
	protected MitigationApproach getApproach(DataFlowDiagramAndDictionary dfd, List<AnalysisConstraint> constraints) {
		List<Constraint> translatedConstraints = new ArrayList<>();

		for (var constraint : constraints) {
			var translation = new CNFTranslation(constraint);
			translatedConstraints.addAll(translation.constructCNF());
		}

		return new Mechanic(dfd, "temp", translatedConstraints);
	}

	@Override
	protected String getApproachName() {
		return "SAT";
	}
		
    @AfterAll
    static void cleanup() throws IOException {
        Files.deleteIfExists(Path.of("testresults/temp.cnf"));
        Files.deleteIfExists(Path.of("testresults/temp-literalMapping.json"));
    }
    
    @Override
    protected List<Integer> getConstraintScaling() {
        return List.of(1, 2, 4, 6, 8, 10, 20, 30, 40, 50);
    }

}
