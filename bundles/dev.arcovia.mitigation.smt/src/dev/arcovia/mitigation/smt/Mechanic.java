package dev.arcovia.mitigation.smt;

import java.util.List;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;

import dev.arcovia.mitigation.sat.MitigationApproach;
import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.config.ConfigBuilder;
import dev.arcovia.mitigation.smt.preprocess.Preprocess;
import dev.arcovia.mitigation.smt.preprocess.PreprocessingResult;

public class Mechanic implements MitigationApproach{
	
	private Config config = null;
	DataFlowDiagramAndDictionary dfd;
	List<AnalysisConstraint> constraints;
	
	public Mechanic(DataFlowDiagramAndDictionary dfd, List<AnalysisConstraint> constraints) {
		this.dfd = dfd;
		this.constraints = constraints;
	};
	
	@Override
	public DataFlowDiagramAndDictionary repair() throws Exception {
		if (config == null) {
            config = new ConfigBuilder().build();
        }
        Preprocess preprocces = new Preprocess();
        PreprocessingResult preprocessingResult = preprocces.preprocess(dfd, constraints, config.onlyViolatingTFGs());
        SMT smt = new SMT(preprocessingResult, constraints, config);
        var result = smt.repair();
		return result.repairedDFD();
	}

	@Override
	public void restrictToLabelAddition() {
		config = new ConfigBuilder().removeDataLabels(false)
                .removeNodeLabels(false)
                .build();
		
	}
	
}
