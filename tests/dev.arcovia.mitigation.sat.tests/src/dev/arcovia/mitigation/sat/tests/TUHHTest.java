package dev.arcovia.mitigation.sat.tests;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.dataflowanalysis.converter.DataFlowDiagramConverter;
import org.dataflowanalysis.examplemodels.Activator;
import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import com.google.common.collect.ImmutableMap;

import dev.arcovia.mitigation.sat.Constraint;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.Literal;
import dev.arcovia.mitigation.sat.Mechanic;
import dev.arcovia.mitigation.sat.NodeLabel;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

public class TUHHTest {
    @Test
    public void tuhhTest() throws ContradictionException, TimeoutException, IOException, StandaloneInitializationException {
        var dfdConverter = new DataFlowDiagramConverter();
        final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
        final String location = Paths.get("casestudies", "TUHH-Models")
                .toString();

       
        var nodeConstraint = new Constraint(List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
                new Literal(true, new NodeLabel(new Label("Stereotype", "local_logging")))));
        var constraints = List.of(nodeConstraint);

        Map<Label, Integer> costs = ImmutableMap.<Label, Integer>builder()
                .put(new Label("Stereotype", "internal"), 3)
                .put(new Label("Stereotype", "local_logging"), 1)
                .build();

        var dfd = dfdConverter.loadDFD(PROJECT_NAME, Paths.get(location, "jferrater", "jferrater_0.dataflowdiagram")
                .toString(),
                Paths.get(location, "jferrater", "jferrater_0.datadictionary")
                        .toString(),
                Activator.class);
        
        var repairedDfdCosts = new Mechanic(dfd, constraints, costs).repair();
        
        dfdConverter.storeWeb(dfdConverter.dfdToWeb(repairedDfdCosts), "tuhhrepaired.json");
        
    }
}
