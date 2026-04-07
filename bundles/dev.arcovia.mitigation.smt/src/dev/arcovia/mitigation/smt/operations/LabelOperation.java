package dev.arcovia.mitigation.smt.operations;

import java.util.Optional;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelType;

/**
 * Adds a Label to a given LabelType if it exists.
 */
public class LabelOperation extends DataDictionaryOperation {

    String type;
    String name;
    String id;

    public LabelOperation(String type, String name, String id) {
        this.type = type;
        this.name = name;
        this.id = id;
    }

    public LabelOperation(String type, String name) {
        this(type, name, String.valueOf(random.nextInt()));
    }

    @Override
    public DataFlowDiagramAndDictionary doOperation(DataFlowDiagramAndDictionary dfd) {
        // Find correct label type
        Optional<LabelType> optionalLabelType = dfd.dataDictionary()
                .getLabelTypes()
                .stream()
                .filter(x -> x.getEntityName()
                        .equals(type))
                .findFirst();
        if (optionalLabelType.isEmpty()) {
            logger.debug("Couldn't find label type " + type);
        } else {
            // Add label
            Label label = factory.createLabel();
            label.setEntityName(name);
            label.setId(id);
            optionalLabelType.get()
                    .getLabel()
                    .add(label);
        }
        return dfd;
    }

    @Override
    public DataFlowDiagramAndDictionary undoOperation(DataFlowDiagramAndDictionary dfd) {
        // Find correct label type
        Optional<LabelType> optionalLabelType = dfd.dataDictionary()
                .getLabelTypes()
                .stream()
                .filter(x -> x.getEntityName()
                        .equals(type))
                .findFirst();
        if (optionalLabelType.isEmpty()) {
            logger.debug("Couldn't find label type " + type);
        } else {
            // Remove label
            optionalLabelType.get()
                    .getLabel()
                    .removeIf(x -> x.getEntityName()
                            .equals(name));
        }
        return dfd;
    }

}
