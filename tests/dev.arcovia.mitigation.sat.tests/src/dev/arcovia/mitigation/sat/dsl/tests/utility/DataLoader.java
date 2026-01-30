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

/**
 * Utility class for loading data flow diagrams and dictionaries, extracting variables, and exporting data to JSON files
 * for testing purposes.
 * <p>
 * Provides methods to load {@link DataFlowDiagramAndDictionary} instances from file paths, convert them into variable
 * maps, and write arrays or test results to JSON files.
 */
public abstract class DataLoader {

    /**
     * Loads a {@link DataFlowDiagramAndDictionary} from the specified file paths.
     * @param inputDataFlowDiagram the path to the data flow diagram file
     * @param inputDataDictionary the path to the data dictionary file
     * @return a {@link DataFlowDiagramAndDictionary} instance initialized with the given files
     * @throws StandaloneInitializationException if loading fails
     */
    public static DataFlowDiagramAndDictionary loadDFDFromPath(String inputDataFlowDiagram, String inputDataDictionary)
            throws StandaloneInitializationException {
        final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
        return new DataFlowDiagramAndDictionary(PROJECT_NAME, Paths.get(inputDataFlowDiagram)
                .toString(),
                Paths.get(inputDataDictionary)
                        .toString(),
                Activator.class);
    }

    /**
     * Extracts variables from a {@link DataFlowDiagramAndDictionary} into a map. The map keys are entity names, and values
     * are lists of corresponding label names.
     * @param dfd the {@link DataFlowDiagramAndDictionary} to extract variables from
     * @return a map of entity names to lists of label names
     */
    public static HashMap<String, List<String>> variables(DataFlowDiagramAndDictionary dfd) {
        var variables = new HashMap<String, List<String>>();
        dfd.dataDictionary()
                .getLabelTypes()
                .forEach(it -> variables.put(it.getEntityName(), it.getLabel()
                        .stream()
                        .map(NamedElement::getEntityName)
                        .toList()));
        return variables;
    }

    /**
     * Writes an array of integers as a JSON array to a file.
     * @param output the array of integers to write
     * @param fileName the name of the file to write to
     * @throws IOException if an I/O error occurs during writing
     */
    public static void outputJsonArray(int[] output, String fileName) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(output);
        byte[] strToBytes = jsonString.getBytes();
        printToFile(strToBytes, fileName);
    }

    /**
     * Writes an array of {@link ReadabilityTestResult} objects as a JSON array to a file.
     * @param results the array of test results to write
     * @param fileName the name of the file to write to
     * @throws IOException if an I/O error occurs during writing
     */
    public static void outputTestResults(ReadabilityTestResult[] results, String fileName) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(results);
        byte[] strToBytes = jsonString.getBytes();
        printToFile(strToBytes, fileName);
    }
    
    public static void outputTestResults(List<StructureResult> results, String fileName) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(results);
        byte[] strToBytes = jsonString.getBytes();
        printToFile(strToBytes, fileName);
    }

    private static void printToFile(byte[] strToBytes, String fileName) throws IOException {
        final String OUTPUT_DIRECTORY = "output";
        Path dir = Paths.get(OUTPUT_DIRECTORY);
        Files.createDirectories(dir);
        Path file = Paths.get(String.valueOf(dir), fileName);
        Files.deleteIfExists(file);
        Files.write(file, strToBytes);
    }
}
