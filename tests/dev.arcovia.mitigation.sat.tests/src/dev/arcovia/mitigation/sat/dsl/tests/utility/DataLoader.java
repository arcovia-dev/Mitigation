package dev.arcovia.mitigation.sat.dsl.tests.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.Activator;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;
import tools.mdsd.modelingfoundations.identifier.NamedElement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public abstract class DataLoader {
    public static DataFlowDiagramAndDictionary loadDFD(String model, String name) throws StandaloneInitializationException {
        final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
        final String location = Paths.get("scenarios","dfd", "TUHH-Models")
                .toString();
        return new DataFlowDiagramAndDictionary(PROJECT_NAME,
                Paths.get(location, model, (name + ".dataflowdiagram")).toString(),
                Paths.get(location, model, (name + ".datadictionary"))
                        .toString(), Activator.class);
    }

    public static DataFlowDiagramAndDictionary loadDFDfromPath(String inputDataFlowDiagram, String inputDataDictionary) throws StandaloneInitializationException {
        final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
        return new DataFlowDiagramAndDictionary(
                PROJECT_NAME,
                Paths.get(inputDataFlowDiagram).toString(),
                Paths.get(inputDataDictionary).toString(),
                Activator.class
        );
    }

    public static HashMap<String, List<String>> variables(DataFlowDiagramAndDictionary dfd){
        var variables = new HashMap<String, List<String>>();
        dfd.dataDictionary().getLabelTypes().forEach(it -> variables.put(
                it.getEntityName(),
                it.getLabel().stream().map(NamedElement::getEntityName).toList()));
        return variables;
    }

    public static void outputJsonArray(int[] output, String fileName) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(output);
        Path path = Paths.get("output", fileName);
        byte[] strToBytes = jsonString.getBytes();
        Files.deleteIfExists(path);
        Files.write(path, strToBytes);
    }

    public static void outputTestResults(ReadabilityTestResult[] results, String fileName) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(results);
        Path path = Paths.get("output", fileName);
        byte[] strToBytes = jsonString.getBytes();
        Files.deleteIfExists(path);
        Files.write(path, strToBytes);
    }
}
