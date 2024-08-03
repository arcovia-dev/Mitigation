package dev.abunai.confidentiality.mitigation.tests.sat;

import java.util.List;

import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import dev.abunai.confidentiality.mitigation.sat.Constraint;
import dev.abunai.confidentiality.mitigation.sat.Label;
import dev.abunai.confidentiality.mitigation.sat.Mechanic;

public class SatTest {

    public final String MIN_SAT = "models/minsat.json";

    @Test
    public void automaticTest() throws ContradictionException, TimeoutException {
        var converter = new DataFlowDiagramConverter();
        var dfd = converter.webToDfd(MIN_SAT);

        // (personal AND nonEU) => encrypted
        var constraints = List.of(new Constraint(false, "Data", new Label("Sensitivity", "Personal")),
                new Constraint(false, "Node", new Label("Location", "nonEU")), new Constraint(true, "Data", new Label("Encryption", "Encrypted")));

        var repairedDfd = new Mechanic().repair(dfd, constraints);
        converter.storeWeb(converter.dfdToWeb(repairedDfd), "repaired.json");
    }
}
