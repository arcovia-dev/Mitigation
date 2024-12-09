package dev.arcovia.mitigation.ranking.tests;

import java.util.List;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.core.DataCharacteristic;
import org.dataflowanalysis.analysis.dfd.core.DFDCharacteristicValue;

public abstract class TestBase {
	
	public static final String TEST_MODEL_PROJECT_NAME = "dev.arcovia.mitigation.ranking.tests";
	
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
	
	protected List<String> retrieveAllDataLabels(AbstractVertex<?> vertex) {
	    System.out.println(vertex.getDataCharacteristicNamesMap("Stereotype"));
        return vertex.getAllDataCharacteristics().stream()
                .map(DataCharacteristic::getAllCharacteristics)
                .flatMap(List::stream).map(DFDCharacteristicValue.class::cast).map(DFDCharacteristicValue::getValueName)
                .toList();
    }
}
