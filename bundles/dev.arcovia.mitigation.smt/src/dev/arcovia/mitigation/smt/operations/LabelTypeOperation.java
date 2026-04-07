package dev.arcovia.mitigation.smt.operations;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.LabelType;

/**
 * Adds a label type to a Datadictionary
 */
public class LabelTypeOperation extends DataDictionaryOperation {

    private String name;
    private String id;

    public LabelTypeOperation(String name) {
        this(name, String.valueOf(random.nextInt()));
    }

    public LabelTypeOperation(String name, String id) {
        this.name = name;
        this.id = id;
    }

    @Override
    public DataFlowDiagramAndDictionary doOperation(DataFlowDiagramAndDictionary dfd) {
        LabelType newLabelType = factory.createLabelType();
        newLabelType.setEntityName(name);
        newLabelType.setId(id);
        dfd.dataDictionary()
                .getLabelTypes()
                .add(newLabelType);
        logger.debug("Added Label Type " + name);
        return dfd;
    }

    @Override
    public DataFlowDiagramAndDictionary undoOperation(DataFlowDiagramAndDictionary dfd) {
        dfd.dataDictionary()
                .getLabelTypes()
                .removeIf(x -> x.getEntityName()
                        .equals(name));
        logger.debug("Removed label type " + name);
        return dfd;
    }
}