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

    public static DataFlowDiagramAndDictionary loadDFDFromPath(String inputDataFlowDiagram, String inputDataDictionary) throws StandaloneInitializationException {
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
        byte[] strToBytes = jsonString.getBytes();
        printToFile(strToBytes, fileName);
    }

    public static void outputTestResults(ReadabilityTestResult[] results, String fileName) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(results);
        byte[] strToBytes = jsonString.getBytes();
        printToFile(strToBytes, fileName);
    }

    private static void printToFile(byte[] strToBytes, String fileName) throws IOException {
        Path dir = Paths.get("output");
        Files.createDirectories(dir);
        Path file = Paths.get(String.valueOf(dir), fileName);
        Files.deleteIfExists(file);
        Files.write(file, strToBytes);
    }
}
