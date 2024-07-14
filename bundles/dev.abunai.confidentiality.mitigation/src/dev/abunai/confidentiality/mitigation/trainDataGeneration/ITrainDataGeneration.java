package dev.abunai.confidentiality.mitigation.trainDataGeneration;

import java.util.List;

import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;

public interface ITrainDataGeneration {

	public void violationDataToCSV(List<UncertainConstraintViolation> violations,
			List<UncertaintySource> allUncertainties, String outputPath);
	
}
