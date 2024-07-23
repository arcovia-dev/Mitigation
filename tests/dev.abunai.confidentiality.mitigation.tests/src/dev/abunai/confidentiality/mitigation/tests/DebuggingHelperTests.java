package dev.abunai.confidentiality.mitigation.tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.core.DataCharacteristic;
import org.dataflowanalysis.analysis.dfd.core.DFDCharacteristicValue;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.analysis.UncertaintyAwareConfidentialityAnalysis;
import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainFlowGraphCollection;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertaintyAwareConfidentialityAnalysisBuilder;
import org.dataflowanalysis.converter.DataFlowDiagramConverter;

public class DebuggingHelperTests {
	
	public final String TEST_MODEL_PROJECT_NAME = "dev.abunai.confidentiality.mitigation.tests";
	protected final String pathToMeassurements = "C:/Users/Jonas/Desktop/Masterarbeit_Paper/Mitigation/tests/dev.abunai.confidentiality.mitigation.tests/meassurements.txt";
	
	
	@Test
	public void webToDfd() {
		String path = "C:/Users/Jonas/Desktop/Masterarbeit_Paper/Mitigation/tests/dev.abunai.confidentiality.mitigation.tests/beh.json";
		DataFlowDiagramConverter conv = new DataFlowDiagramConverter();
		var dd = conv.webToDfd(path);
		conv.storeDFD(dd, "beh");
	}
	
	@Test
	public void seeAverageRuntime() {
		Path filePath = Paths.get(pathToMeassurements);
		try {
			var contentLines = Files.readAllLines(filePath);
			int sum = 0;
			for(int i = contentLines.size()-20; i < contentLines.size();i++) {
				sum += Integer.parseInt(contentLines.get(i));
			}
			System.out.println(sum/20);
			Files.write(filePath, "".getBytes(StandardCharsets.UTF_8));			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	@Test
	public void runUIA() {
		final var dataFlowDiagramPath = Paths.get("models", "mitigation", "mitigation0" + ".dataflowdiagram")
				.toString();
		final var dataDictionaryPath = Paths.get("models", "mitigation", "mitigation0" + ".datadictionary")
				.toString();
		final var uncertaintyPath = Paths.get("models", "mitigation", "mitigation" + ".uncertainty")
				.toString();

		List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();
		constraints.add(it -> {
			System.out.println(this.retrieveNodeLabels(it));
			System.out.println(this.retrieveDataLabels(it));
			return this.retrieveNodeLabels(it).contains("Processable")
					&& this.retrieveDataLabels(it).contains("Encrypted");
		});
		
		var builder = new DFDUncertaintyAwareConfidentialityAnalysisBuilder().standalone()
				.modelProjectName(TEST_MODEL_PROJECT_NAME).usePluginActivator(Activator.class)
				.useDataDictionary(dataDictionaryPath).useDataFlowDiagram(dataFlowDiagramPath)
				.useUncertaintyModel(uncertaintyPath);

		UncertaintyAwareConfidentialityAnalysis analysis = builder.build();
		analysis.initializeAnalysis();
		
		DFDUncertainFlowGraphCollection flowGraphs = (DFDUncertainFlowGraphCollection) analysis.findFlowGraph();
		DFDUncertainFlowGraphCollection uncertainFlowGraphs = flowGraphs.createUncertainFlows();
		uncertainFlowGraphs.evaluate();
		
		boolean noConstraintViolated = true;
		for(var constraint: constraints) {
			List<UncertainConstraintViolation> violations = analysis.queryUncertainDataFlow(uncertainFlowGraphs,constraint);
			if(violations.size() > 0) {
				noConstraintViolated = false;
				break;
			}
		}
		
		if(noConstraintViolated) {
			System.out.println("Valid Model");
		}
	}
	
	protected List<String> retrieveNodeLabels(AbstractVertex<?> vertex) {
		return vertex.getAllVertexCharacteristics().stream().map(DFDCharacteristicValue.class::cast)
				.map(DFDCharacteristicValue::getValueName).toList();
	}

	protected List<String> retrieveDataLabels(AbstractVertex<?> vertex) {
		return vertex.getAllIncomingDataCharacteristics().stream()
				.map(DataCharacteristic::getAllCharacteristics)
				.flatMap(List::stream).map(DFDCharacteristicValue.class::cast).map(DFDCharacteristicValue::getValueName)
				.toList();
	}

}
