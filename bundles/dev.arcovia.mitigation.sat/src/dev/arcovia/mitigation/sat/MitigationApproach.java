package dev.arcovia.mitigation.sat;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;


public interface MitigationApproach {
	DataFlowDiagramAndDictionary repair() throws Exception;
    void restrictToLabelAddition();
}
