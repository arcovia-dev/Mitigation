package dev.abunai.confidentiality.mitigation.testBases;

import java.nio.file.Paths;
import java.util.List;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.core.DataCharacteristic;
import org.dataflowanalysis.analysis.dfd.core.DFDCharacteristicValue;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;

public abstract class TestBase {
	public static final String TEST_MODEL_PROJECT_NAME = "dev.abunai.confidentiality.mitigation.tests";
	public static final String PROJECT_ROOT_PATH = Paths.get("").toAbsolutePath().toString();
	public DataDictionary dd;
	public DataFlowDiagram dfd;
	
	protected abstract String getFolderName();

	protected abstract String getFilesName();

	protected String getBaseFolder() {
		return "models";
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
