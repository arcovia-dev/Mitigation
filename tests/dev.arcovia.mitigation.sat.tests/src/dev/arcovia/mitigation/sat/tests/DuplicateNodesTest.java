package dev.arcovia.mitigation.sat.tests;

import dev.arcovia.mitigation.sat.*;
import org.dataflowanalysis.converter.dfd2web.DFD2WebConverter;
import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import java.io.IOException;
import java.util.List;

public class DuplicateNodesTest {
    @Test
    public void automaticTest() throws ContradictionException, TimeoutException, IOException {
        var dfdConverter = new DFD2WebConverter();

        // (personal AND nonEU) => encrypted
        var dataConstraint = new Constraint(List.of(new Literal(false, new IncomingDataLabel(new Label("Sensitivity", "Personal"))),
                new Literal(false, new NodeLabel(new Label("Location", "nonEU"))),
                new Literal(true, new IncomingDataLabel(new Label("Encryption", "Encrypted")))));

        var constraints = List.of(dataConstraint);

        var repairedDfdCosts = new Mechanic("models/duplicateNodes.json", constraints).repair();

        dfdConverter.convert(repairedDfdCosts)
                .save("testresults/", "duplicateNodes-repaired.json");
    }
}
