package dev.arcovia.mitigation.ranking;

import java.util.List;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;

public record MitigationModel(DataFlowDiagramAndDictionary model, String modelName, List<String> chosenScenarios) {
    public MitigationModel(DataFlowDiagramAndDictionary model, String modelName, List<String> chosenScenarios) {
        this.model = model;
        this.modelName = modelName;
        this.chosenScenarios = chosenScenarios;
    }
}
