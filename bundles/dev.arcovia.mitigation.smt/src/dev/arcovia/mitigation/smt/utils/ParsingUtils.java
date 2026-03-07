package dev.arcovia.mitigation.smt.utils;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dfd.DFDConfidentialityAnalysis;
import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dfd.dsl.DFDVertexType;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.result.DSLResult;
import org.dataflowanalysis.analysis.dsl.selectors.AbstractSelector;
import org.dataflowanalysis.analysis.dsl.selectors.CharacteristicsSelectorData;
import org.dataflowanalysis.analysis.dsl.selectors.ConditionalSelector;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicListSelector;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicsSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VariableNameSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsListSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexNameSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexTypeSelector;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.AND;
import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Behavior;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelReference;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.dfd.datadictionary.NOT;
import org.dataflowanalysis.dfd.datadictionary.OR;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.TRUE;
import org.dataflowanalysis.dfd.datadictionary.Term;
import org.dataflowanalysis.dfd.dataflowdiagram.External;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.dataflowanalysis.examplemodels.Activator;

import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

/**
 * Contains parsing functions
 */
public class ParsingUtils {

    private static final Logger logger = Logger.getLogger(ParsingUtils.class);

    /**
     * Maps characteristisc to Labels, given a contextual datadictionary
     * @param dd Datadictionary that contains labels
     * @param chars Incoming Characteristics
     * @return List of labels encoded in characteristics
     */
    public static Set<Label> getLabelsForCharacteristics(DataDictionary dd, List<CharacteristicsSelectorData> chars) {
        Set<Label> result = new HashSet<>();
        for (CharacteristicsSelectorData data : chars) {
            LabelType labelType = getLabelTypeByName(dd, data.characteristicType()
                    .toString());
            Label label = getLabelByName(labelType, data.characteristicValue()
                    .toString());
            result.add(label);
        }
        return result;
    }

    /**
     * Loads a dfd. Currently only static from the hardcoded folder
     * @param model Model that the dfd resides in
     * @param name Filename without file endings
     * @return Loaded dfd
     * @throws StandaloneInitializationException If input DFD at paths is incorrect or can not be properly resolved
     */
    public static DataFlowDiagramAndDictionary loadDFD(String model, String name) throws StandaloneInitializationException {
        final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
        final String location = Paths.get("scenarios", "dfd", "TUHH-Models")
                .toString();
        return new DataFlowDiagramAndDictionary(PROJECT_NAME, Paths.get(location, model, (name + ".dataflowdiagram"))
                .toString(),
                Paths.get(location, model, (name + ".datadictionary"))
                        .toString(),
                Activator.class);
    }

    /**
     * Returns the Type of the Node that this Vertex references
     * @param vertex
     * @return Type of the Vertex
     */
    public static DFDVertexType vertexToType(DFDVertex vertex) {
        return nodeToType(vertex.getReferencedElement());
    }

    /**
     * Returns the Vertex Type of this Node
     * @param node
     * @return Type of the Node
     */
    public static DFDVertexType nodeToType(Node n) {
        if (n instanceof External) {
            return DFDVertexType.EXTERNAL;
        } else if (n instanceof org.dataflowanalysis.dfd.dataflowdiagram.Process) {
            return DFDVertexType.PROCESS;
        } else {
            return DFDVertexType.STORE;
        }
    }

    /**
     * Finds relevant vertex types, based on incoming constraints
     * @param constraints Incoming constraints
     * @return Set of relevant vertex types
     */
    public static Set<DFDVertexType> getRelevantVertexTypes(List<AnalysisConstraint> constraints) {
        Set<DFDVertexType> result = new HashSet<>();
        for (AnalysisConstraint constr : constraints) {
            List<AbstractSelector> allSelectors = constr.getVertexDestinationSelectors()
                    .getSelectors();
            allSelectors.addAll(constr.getVertexSourceSelectors()
                    .getSelectors());
            for (AbstractSelector selector : allSelectors) {
                if (selector instanceof VertexTypeSelector cast) {
                    DFDVertexType type = (DFDVertexType) cast.getVertexType();
                    result.add(type);
                }
            }
        }
        return result;
    }

    /**
     * Finds relevant Node labels of a datadictionary, i.e. those appearing in negated vertex selectors
     * @param dd Datadictionary
     * @param constraints constraints
     * @return List of relevant node labels to add
     */
    public static Set<Label> getRelevantNodeLabelsAdd(DataDictionary dd, List<AnalysisConstraint> constraints) {
        List<CharacteristicsSelectorData> relevantNodeCharacteristics = getAnalysisNodeCharacteristics(constraints, true);
        return getLabelsForCharacteristics(dd, relevantNodeCharacteristics);
    }

    /**
     * Finds relevant Node labels of a datadictionary, i.e. those appearing in non-negated vertex selectors
     * @param dd Datadictionary
     * @param constraints constraints
     * @return List of relevant node labels to remove
     */
    public static Set<Label> getRelevantNodeLabelsRemove(DataDictionary dd, List<AnalysisConstraint> constraints) {
        List<CharacteristicsSelectorData> relevantNodeCharacteristics = getAnalysisNodeCharacteristics(constraints, false);
        return getLabelsForCharacteristics(dd, relevantNodeCharacteristics);
    }

    /**
     * Finds relevant data labels of a Datadictionary, i.e. those appearing in negated data selectors *
     * @param dd Datadictionary
     * @param constraints constraints
     * @return List of relevant Data labels to add
     */
    public static Set<Label> getRelevantDataLabelsAdd(DataDictionary dd, List<AnalysisConstraint> constraints) {
        List<CharacteristicsSelectorData> relevantDataCharacteristics = getAnalysisDataCharacteristics(constraints, true);
        return getLabelsForCharacteristics(dd, relevantDataCharacteristics);
    }

    /**
     * Finds relevant data labels of a Datadictionary, i.e. those appearing in non-negated data selectors
     * @param dd Datadictionary
     * @param constraints constraints
     * @return List of relevant Data labels to remove
     */
    public static Set<Label> getRelevantDataLabelsRemove(DataDictionary dd, List<AnalysisConstraint> constraints) {
        List<CharacteristicsSelectorData> relevantDataCharacteristics = getAnalysisDataCharacteristics(constraints, false);
        return getLabelsForCharacteristics(dd, relevantDataCharacteristics);
    }

    /**
     * Given a list of analysis constraints, extracts vertex characteristics that are either negated or non-negated
     * @param constraints Incoming constraints
     * @param add Whether labels that need to be added should be found. This means that labels from negated selectors are
     * returned
     * @return List of requested vertex characteristics in constraints
     */
    private static List<CharacteristicsSelectorData> getAnalysisNodeCharacteristics(List<AnalysisConstraint> constraints, boolean add) {
        List<CharacteristicsSelectorData> characteristicsSelectorData = new ArrayList<>();
        for (AnalysisConstraint constr : constraints) {
            List<AbstractSelector> allSelectors = constr.getVertexDestinationSelectors()
                    .getSelectors();
            allSelectors.addAll(constr.getVertexSourceSelectors()
                    .getSelectors());
            for (AbstractSelector selector : allSelectors) {
                if (selector instanceof VertexCharacteristicsListSelector cast && (cast.isInverted() == add)) {
                    characteristicsSelectorData.addAll(cast.getVertexCharacteristics());
                } else if (selector instanceof VertexCharacteristicsSelector cast && (cast.isInverted() == add)) {
                    characteristicsSelectorData.add(cast.getVertexCharacteristics());
                } else if (selector instanceof ConditionalSelector) {
                    logger.error("Conditional Selector detected. Currently not supported.");
                    throw new UnsupportedOperationException("Conditional Selector detected. Currently not supported.");
                }
            }
        }
        return characteristicsSelectorData;
    }

    /**
     * Given a list of analysis constraints, extracts data characteristics that are either negated or non-negated
     * @param constraints Incoming constraints
     * @param add Whether labels that need to be added should be found. This means that labels from negated selectors are
     * returned
     * @return List of data characteristics in constraints
     */
    private static List<CharacteristicsSelectorData> getAnalysisDataCharacteristics(List<AnalysisConstraint> constraints, boolean add) {
        List<CharacteristicsSelectorData> characteristicsSelectorData = new ArrayList<>();

        for (AnalysisConstraint constr : constraints) {
            List<AbstractSelector> allSelectors = constr.getDataSourceSelectors()
                    .getSelectors();
            for (AbstractSelector selector : allSelectors) {
                if (selector instanceof DataCharacteristicsSelector cast && (cast.isInverted() == add)) {
                    characteristicsSelectorData.add(cast.getDataCharacteristic());
                } else if (selector instanceof DataCharacteristicListSelector cast && (cast.isInverted() == add)) {
                    characteristicsSelectorData.addAll(cast.getDataCharacteristics());
                } else if (selector instanceof ConditionalSelector) {
                    logger.error("Conditional Selector detected. Currently not supported.");
                    throw new UnsupportedOperationException("Conditional Selector detected. Currently not supported.");
                }
            }
        }
        return characteristicsSelectorData;
    }

    /**
     * Checks if a list of constraints contains a DataNameSelector
     * @param constraints
     */
    public static boolean containsFlowNameSelector(List<AnalysisConstraint> constraints) {
        return constraints.stream()
                .flatMap(x -> x.getDataSourceSelectors()
                        .getSelectors()
                        .stream())
                .anyMatch(VariableNameSelector.class::isInstance);
    }

    /**
     * Checks if a list of constraints contains a VertexNameSelector
     * @param constraints
     */
    public static boolean containsVertexNameSelector(List<AnalysisConstraint> constraints) {
        return constraints.stream()
                .flatMap(x -> x.getVertexDestinationSelectors()
                        .getSelectors()
                        .stream())
                .anyMatch(VertexNameSelector.class::isInstance);
    }

    /**
     * Checks if a list of constraints contains a Vertex Type Selector
     * @param constraints
     */
    public static boolean containsVertexTypeSelector(List<AnalysisConstraint> constraints) {
        return constraints.stream()
                .flatMap(c -> Stream.concat(c.getVertexDestinationSelectors()
                        .getSelectors()
                        .stream(),
                        c.getVertexSourceSelectors()
                                .getSelectors()
                                .stream()))
                .anyMatch(VertexTypeSelector.class::isInstance);
    }

    /**
     * Extracts all data/vertex characteristics from constraints
     * @param constraints List of constraints
     * @return List of characteristics
     */
    public static Set<CharacteristicsSelectorData> getAnalysisCharacteristics(List<AnalysisConstraint> constraints) {
        Set<CharacteristicsSelectorData> all = new HashSet<>();
        all.addAll(getAnalysisDataCharacteristics(constraints, false));
        all.addAll(getAnalysisDataCharacteristics(constraints, true));
        all.addAll(getAnalysisNodeCharacteristics(constraints, false));
        all.addAll(getAnalysisNodeCharacteristics(constraints, true));
        return all;
    }

    /**
     * Transforms a map of string representations of labels to their concrete obejcts
     * @param dd that the labels will be searched for
     * @param labelCosts Input map with strings
     * @return map with label objects
     */
    public static Map<Label, Integer> transformLabelCosts(DataDictionary dd, Map<String, Integer> labelCosts) {
        Map<Label, Integer> result = new HashMap<>();
        labelCosts.forEach((string, integer) -> result.put(getLabelByName(dd, string.split("\\.")[0], string.split("\\.")[1]), integer));
        return result;
    }

    /**
     * Finds label types by name
     * @param dd Datadictionary
     * @param name Label Type name
     * @return Label type, if exists, elsenull
     */
    public static LabelType getLabelTypeByName(DataDictionary dd, String name) {
        return dd.getLabelTypes()
                .stream()
                .filter(x -> x.getEntityName()
                        .equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds label by name
     * @param labelType Type of the label
     * @param name Name of the label
     * @return Label, if exists, else null
     */
    private static Label getLabelByName(LabelType labelType, String name) {
        return labelType.getLabel()
                .stream()
                .filter(x -> x.getEntityName()
                        .equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a label by name
     * @param dd that will be searched
     * @param typeName of the label type
     * @param labelName of the label
     * @return The label object
     */
    private static Label getLabelByName(DataDictionary dd, String typeName, String labelName) {
        LabelType labelType = getLabelTypeByName(dd, typeName);
        return getLabelByName(labelType, labelName);
    }

    /**
     * Checks if label type with name is present in Datadictionary
     * @param dd Data dictionary
     * @param name of the label type
     * @return True if present, else false
     */
    public static boolean containsLabelType(DataDictionary dd, String name) {
        return dd.getLabelTypes()
                .stream()
                .anyMatch(x -> x.getEntityName()
                        .equals(name));
    }

    /**
     * Checks if label type contains a label with name
     * @param type Present label type
     * @param name Name of label
     * @return True if label type contains label else false
     */
    public static boolean containsLabel(LabelType type, String name) {
        return type.getLabel()
                .stream()
                .anyMatch(x -> x.getEntityName()
                        .equals(name));
    }

    /**
     * Given a list of nodes, maps output pins to their respective assignments
     * @param nodes List of nodes
     * @return Map
     */
    public static Map<Pin, List<AbstractAssignment>> outPinToAssignments(List<Node> nodes) {
        Map<Pin, List<AbstractAssignment>> outPinToAss = new HashMap<Pin, List<AbstractAssignment>>();
        for (Node n : nodes) {
            Behavior b = n.getBehavior();
            for (int i = 0; i < b.getAssignment()
                    .size(); i++) {
                AbstractAssignment a = b.getAssignment()
                        .get(i);
                List<AbstractAssignment> list = outPinToAss.getOrDefault(a.getOutputPin(), new ArrayList<AbstractAssignment>());
                list.add(a);
                outPinToAss.put(a.getOutputPin(), list);
            }
        }
        return outPinToAss;
    }

    /**
     * Counts violations of a DFD using existing tooling
     * @param dfd Input dfd
     * @param analysisConstraints Constraints to evaluate on
     * @return Amount of violations
     */
    public static int countViolations(DataFlowDiagramAndDictionary dfd, List<AnalysisConstraint> analysisConstraints) {
        DFDModelResourceProvider dfdModelResourceProvider = new DFDModelResourceProvider(dfd.dataDictionary(), dfd.dataFlowDiagram());
        DFDConfidentialityAnalysis dfdConfidentialityAnalysis = new DFDDataFlowAnalysisBuilder().standalone()
                .useCustomResourceProvider(dfdModelResourceProvider)
                .build();
        DFDFlowGraphCollection flowGraphs = dfdConfidentialityAnalysis.findFlowGraphs();
        flowGraphs.evaluate();

        List<DSLResult> result = new ArrayList<>();
        for (int i = 0; i < analysisConstraints.size(); i++) {
            List<DSLResult> violations = analysisConstraints.get(i)
                    .findViolations(flowGraphs);
            result.addAll(violations);
        }
        if (result.size() > 0) {
            System.out.println("DFA found " + result.size() + " tfg violations");
            return result.size();
        } else {
            // System.out.println("No confidentiality violations found.");
            return 0;
        }

    }

    /**
     * Reduces a Assignment term to its LabelReferences and discards all other terms
     * @param term Input term
     * @return All LabelReferences in this Term
     */
    public static Set<LabelReference> reduceToLabelReferences(Term term) {
        if (term instanceof TRUE)
            return new HashSet<>();

        if (term instanceof NOT notTerm)
            return reduceToLabelReferences(notTerm.getNegatedTerm());

        if (term instanceof OR orTerm)
            return orTerm.getTerms()
                    .stream()
                    .flatMap(t -> reduceToLabelReferences(t).stream())
                    .collect(Collectors.toSet());

        if (term instanceof AND andTerm)
            return andTerm.getTerms()
                    .stream()
                    .flatMap(t -> reduceToLabelReferences(t).stream())
                    .collect(Collectors.toSet());

        if (term instanceof LabelReference labelReference)
            return Set.of(labelReference);

        return Set.of();
    }

}
