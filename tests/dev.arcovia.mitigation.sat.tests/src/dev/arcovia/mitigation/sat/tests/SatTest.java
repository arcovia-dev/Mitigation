package dev.arcovia.mitigation.sat.tests;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import dev.arcovia.mitigation.sat.Constraint;
import dev.arcovia.mitigation.sat.IncomingDataLabel;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.Literal;
import dev.arcovia.mitigation.sat.Mechanic;
import dev.arcovia.mitigation.sat.NodeLabel;
import org.dataflowanalysis.converter.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SatTest extends BaseTest{

    public final String MIN_SAT = "models/minsat.json";
    // (personal AND nonEU) => encrypted
    Constraint dataConstraint = new Constraint(List.of(new Literal(false, new IncomingDataLabel(new Label("Sensitivity", "Personal"))),
            new Literal(false, new NodeLabel(new Label("Location", "nonEU"))),
            new Literal(true, new IncomingDataLabel(new Label("Encryption", "Encrypted")))));
    Constraint nodeConstraint = new Constraint(List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
            new Literal(true, new NodeLabel(new Label("Stereotype", "local_logging")))));
    List<Constraint> constraints = List.of(dataConstraint, nodeConstraint);

    @Test
    public void automaticTest() throws ContradictionException, TimeoutException, IOException {
        var dfdConverter = new DataFlowDiagramConverter();

        var repairedDfdCosts = new Mechanic(MIN_SAT, constraints, costs).repair();
        checkIfConsistent(repairedDfdCosts);
        dfdConverter.storeWeb(dfdConverter.dfdToWeb(repairedDfdCosts), "testresults/minsat-repaired.json");

        var repairedDfdMinimal = new Mechanic(MIN_SAT, constraints).repair();
        checkIfConsistent(repairedDfdMinimal);
    }

    private void checkIfConsistent(DataFlowDiagramAndDictionary repairedDfd) {

        Map<String, List<String>> nodeBehavior = getNodeBehavior(repairedDfd);

        Map<String, List<String>> nodeProperties = getNodeProperties(repairedDfd);

        Map<String, List<String>> expectedNodeBehavior = Map.of("process", List.of("Forwarding: process_in_user", "Encrypted"), "user",
                List.of("Personal"), "db", List.of());

        Map<String, List<String>> expectedNodeProperties = Map.of("process", List.of("internal", "local_logging"), "user", List.of(), "db",
                List.of("nonEU"));

        assertEquals(expectedNodeBehavior, nodeBehavior);
        assertEquals(expectedNodeProperties, nodeProperties);
    }

}
