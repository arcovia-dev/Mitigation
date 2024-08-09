package dev.abunai.confidentiality.mitigation.tests.ranking;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainFlowGraphCollection;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainTransposeFlowGraph;
import dev.abunai.confidentiality.mitigation.ranking.UncertaintyRanker;
import dev.abunai.confidentiality.mitigation.tests.MitigationTestBase;

public class ExternalUncertaintyMitigationTest extends MitigationTestBase {

	protected String getFolderName() {
		return "DFDExternalUncertaintyMitigation";
	}

	protected String getFilesName() {
		return "ext";
	}

	protected List<Predicate<? super AbstractVertex<?>>> getConstraints() {
		List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();

		constraints.add(it -> {
			boolean vio = this.retrieveNodeLabels(it).contains("Develop")
					&& this.retrieveDataLabels(it).contains("Personal");
			return vio;
		});
		constraints.add(it -> {
			boolean vio = this.retrieveNodeLabels(it).contains("nonEU")
					&& this.retrieveDataLabels(it).contains("Personal");
			return vio;
		});
		return constraints;
	}

	@Test
	@Order(2)
	@RepeatedTest(30)
	public void createMitigationCandidatesAutomatically() {
		var startTime = System.currentTimeMillis();
		var rankedUncertaintyEntityName = loadRanking();
		var result = mitigateWithIncreasingAmountOfUncertainties(rankedUncertaintyEntityName,analysis.getUncertaintySources());
		if (result.size() == 0) {
			System.out.println("mitigation failed");
		}
		var duration = System.currentTimeMillis()-startTime;
		storeMeassurement(duration);
	}

	@Test
	@Order(3)
	@RepeatedTest(30)
	public void createMitigationCandidatesAutomatically2() {
		var startTime = System.currentTimeMillis();
		var rankedUncertaintyEntityName = loadRanking();
		var result = mitigateWithFixAmountOfUncertainties(rankedUncertaintyEntityName,
				rankedUncertaintyEntityName.size() / 2, analysis.getUncertaintySources());
		if (result.size() == 0) {
			var result2 = mitigateWithFixAmountOfUncertainties(rankedUncertaintyEntityName,
					rankedUncertaintyEntityName.size(), analysis.getUncertaintySources());
			if (result2.size() == 0) {
				System.out.println("mitigation failed");
			}
		}
		var duration = System.currentTimeMillis()-startTime;
		storeMeassurement(duration);
	}

	@Test
	@Order(4)
	@RepeatedTest(30)
	public void createMitigationCandidatesAutomatically3() {
		var startTime = System.currentTimeMillis();
		var rankedUncertaintyEntityName = loadRanking();
		boolean success = false;
		for (int i = 1; i <= 4 && !success; i++) {
			var result = mitigateWithFixAmountOfUncertainties(rankedUncertaintyEntityName,
					i * (rankedUncertaintyEntityName.size() / 4),analysis.getUncertaintySources());
			if(result.size() > 0) {
				success = true;
				break;
			}
		}
		if (!success) {
			System.out.println("mitigation failed");
		}
		var duration = System.currentTimeMillis()-startTime;

		storeMeassurement(duration);
	}

	@Test
	//@RepeatedTest(30)
	@Order(5)
	public void createMitigationCandidatesAutomatically4() {
		var startTime = System.currentTimeMillis();
		var rankedUncertaintyEntityName = analysis.getUncertaintySources().stream().map(u -> u.getEntityName()).toList();
		var result = mitigateWithFixAmountOfUncertainties(rankedUncertaintyEntityName, rankedUncertaintyEntityName.size(),analysis.getUncertaintySources());
		if (result.size() == 0) {
			System.out.println("mitigation failed");
		}
		var duration = System.currentTimeMillis()-startTime;

		storeMeassurement(duration);
	}
}
