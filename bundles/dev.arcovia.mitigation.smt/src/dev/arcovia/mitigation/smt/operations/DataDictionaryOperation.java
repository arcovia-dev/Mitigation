package dev.arcovia.mitigation.smt.operations;

import java.util.Random;

import org.apache.log4j.Logger;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryFactory;

/**
 * Represents an Action that can be executed to modify a Data Dictionary
 */
public abstract class DataDictionaryOperation implements Operation {
    protected final Logger logger = Logger.getLogger(getClass());
    protected static final datadictionaryFactory factory = datadictionaryFactory.eINSTANCE;
    protected static final Random random = new Random();

}
