package dev.arcovia.mitigation.sat.tests;

import java.io.IOException;
import java.util.List;

import dev.arcovia.mitigation.sat.Constraint;
import dev.arcovia.mitigation.sat.IncomingDataLabel;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.Literal;
import dev.arcovia.mitigation.sat.Mechanic;
import dev.arcovia.mitigation.sat.NodeLabel;
import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.dataflowanalysis.converter.WebEditorConverter;
import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ICSETest extends BaseTest{

    public final String ICSE_Sat = "models/ICSE.json";
    // (personal AND nonEU) => encrypted
    Constraint dataConstraint = new Constraint(List.of(new Literal(false, new IncomingDataLabel(new Label("Sensitivity", "Personal"))),
            new Literal(false, new NodeLabel(new Label("Location", "nonEU"))),
            new Literal(true, new IncomingDataLabel(new Label("Encryption", "Encrypted")))));
    // internal -> local_logging
    Constraint nodeConstraint = new Constraint(List.of(new Literal(false, new NodeLabel(new Label("Location", "internal"))),
            new Literal(true, new NodeLabel(new Label("Stereotype", "logging")))));
    List<Constraint> constraints = List.of(dataConstraint, nodeConstraint);

    @Test
    public void automaticTest() throws ContradictionException, TimeoutException, IOException {
        var dfdConverter = new DataFlowDiagramConverter();

        dfdConverter.storeWeb(dfdConverter.dfdToWeb( new WebEditorConverter().webToDfd(ICSE_Sat)), ICSE_Sat);

        var repairedDfdCosts = new Mechanic(ICSE_Sat, constraints, costs).repair();
        dfdConverter.storeWeb(dfdConverter.dfdToWeb(repairedDfdCosts), "testresults/ICSE-repaired.json");

    }

}
