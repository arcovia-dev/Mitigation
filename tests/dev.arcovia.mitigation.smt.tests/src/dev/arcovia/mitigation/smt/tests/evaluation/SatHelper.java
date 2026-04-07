package dev.arcovia.mitigation.smt.tests.evaluation;

import static java.util.Map.entry;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import dev.arcovia.mitigation.sat.Constraint;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.Mechanic;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

public final class SatHelper {

    private SatHelper() {
    }

    public static final Map<Label, Integer> MIN_COSTS = Map.ofEntries(entry(new Label("Stereotype", "gateway"), 1),
            entry(new Label("Stereotype", "authenticated_request"), 1), entry(new Label("Stereotype", "transform_identity_representation"), 1),
            entry(new Label("Stereotype", "token_validation"), 1), entry(new Label("Stereotype", "login_attempts_regulation"), 1),
            entry(new Label("Stereotype", "encrypted_connection"), 1), entry(new Label("Stereotype", "log_sanitization"), 1),
            entry(new Label("Stereotype", "local_logging"), 1));

    public record RepairResult(DataFlowDiagramAndDictionary repairedDfd, int violationsBefore, int violationsAfter, long runtimeInMilliseconds) {
    }

    public static List<Constraint> toSatConstraints(List<AnalysisConstraint> analysisConstraints) {
        return analysisConstraints.stream()
                .flatMap(ac -> new CNFTranslation(ac).constructCNF()
                        .stream())
                .toList();
    }

    public static RepairResult runRepair(DataFlowDiagramAndDictionary dfd, boolean store, List<AnalysisConstraint> analysisConstraints,
            Map<Label, Integer> costMap) throws StandaloneInitializationException, ContradictionException, TimeoutException, IOException {

        String name = dfd.dataFlowDiagram()
                .getEntityName();
        String cnfName = store ? name : "aName";

        long start = System.currentTimeMillis();
        List<Constraint> translated = toSatConstraints(analysisConstraints);
        Mechanic mechanic = new Mechanic(dfd, cnfName, translated, costMap);
        DataFlowDiagramAndDictionary repairedDfd = mechanic.repair();
        long end = System.currentTimeMillis();

        int violationsAfter = new Mechanic(repairedDfd, null, null).amountOfViolations(repairedDfd, translated);

        return new RepairResult(repairedDfd, mechanic.getViolations(), violationsAfter, end - start);
    }
}
