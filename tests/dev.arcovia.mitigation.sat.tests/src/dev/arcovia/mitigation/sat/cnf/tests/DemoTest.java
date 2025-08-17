package dev.arcovia.mitigation.sat.cnf.tests;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.examplemodels.Activator;
import org.junit.jupiter.api.Test;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;
import tools.mdsd.modelingfoundations.identifier.NamedElement;
import org.apache.log4j.Logger;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DemoTest {

    private final Logger logger = Logger.getLogger(DemoTest.class);

    @Test
    public void testImplementation() throws StandaloneInitializationException {
        String model = "ewolff";
        int variant = 5;

        String name = model + "_" + variant;

        var dfd = loadDFD(model, name);
        var dd = dfd.dataDictionary();

        var labels = getLabels(dfd);

//        var labelTypes = dd.getLabelTypes();

//        for (var labelType : labelTypes) {
//            System.out.println("------------------------");
//            System.out.println("Label Type: " + labelType.getEntityName());
//            var labels = labelType.getLabel();
//            for (var label : labels) {
//                System.out.println("Label: " + label.getEntityName());
//            }
//            System.out.println("------------------------");
//        }

        System.out.println("Done");
    }

    private DataFlowDiagramAndDictionary loadDFD(String model, String name) throws StandaloneInitializationException {
        final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
        final String location = Paths.get("scenarios","dfd", "TUHH-Models")
                .toString();
        return new DataFlowDiagramAndDictionary(PROJECT_NAME,
                Paths.get(location, model, (name + ".dataflowdiagram")).toString(),
                Paths.get(location, model, (name + ".datadictionary"))
                        .toString(), Activator.class);
    }

    private Map<String, List<String>> getLabels(DataFlowDiagramAndDictionary dfd){
        Map<String, List<String>> labels = new HashMap<>();
        dfd.dataDictionary().getLabelTypes().forEach(it -> labels.put(
                it.getEntityName(),
                it.getLabel().stream().map(NamedElement::getEntityName).toList()));
        return labels;
    }
}
