package dev.arcovia.mitigation.utils.dsl;

import java.util.*;

import dev.arcovia.mitigation.sat.*;
import dev.arcovia.mitigation.utils.dsl.selectors.conditional.DynamicConditionalSelector;
import dev.arcovia.mitigation.utils.dsl.selectors.conditional.DynamicEmptySetOperationConditionalSelector;
import dev.arcovia.mitigation.utils.dsl.selectors.conditional.DynamicVariableConditionalSelector;
import dev.arcovia.mitigation.utils.dsl.selectors.constant.*;
import dev.arcovia.mitigation.utils.dsl.selectors.dynamic.DynamicDataCharacteristicSelector;
import dev.arcovia.mitigation.utils.dsl.selectors.dynamic.DynamicDataSelector;
import dev.arcovia.mitigation.utils.dsl.selectors.dynamic.DynamicVertexCharacteristicSelector;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.selectors.*;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import tools.mdsd.modelingfoundations.identifier.NamedElement;

/**
 * Translates an {@link AnalysisConstraint} into a CNF representation. Handles constant and dynamic data/vertex
 * selectors, conditional selectors, and variable mappings. Provides methods to construct, format, and analyze the
 * resulting CNF formula.
 */
public class CNFTranslation {
    private final AnalysisConstraint analysisConstraint;

    private final List<ConstantDataSelector> constantSelectors = new ArrayList<>();
    private final Map<String, DynamicDataSelector> dynamicSelectors = new HashMap<>();
    private final List<DynamicConditionalSelector> conditionalSelectors = new ArrayList<>();

    private final Map<String, List<String>> variables;

    private List<Constraint> cnf;
    private BaseFormula baseFormula;
    private BaseFormula conditionalFormula;
    private BaseFormula cnfFormula;

    /**
     * Constructs a {@link CNFTranslation} with the given analysis constraint and variable mappings. Throws an exception if
     * the analysis constraint contains vertex source selectors.
     * @param analysisConstraint the {@link AnalysisConstraint} to translate
     * @param variables a map of variable names to their corresponding string values
     * @throws IllegalArgumentException if the analysis constraint contains vertex source selectors
     */
    public CNFTranslation(AnalysisConstraint analysisConstraint, Map<String, List<String>> variables) {
        this.analysisConstraint = Objects.requireNonNull(analysisConstraint);
        this.variables = Objects.requireNonNull(variables);
        if (!analysisConstraint.getVertexSourceSelectors()
                .getSelectors()
                .isEmpty()) {
            throw new IllegalArgumentException("OutgoingData (VertexSourceSelectors) is not allowed in this context.");
        }
    }

    /**
     * Constructs a {@link CNFTranslation} with the given analysis constraint and an empty variable map.
     * @param analysisConstraint the {@link AnalysisConstraint} to translate
     */
    public CNFTranslation(AnalysisConstraint analysisConstraint) {
        this(analysisConstraint, new HashMap<>());
    }

    /**
     * Constructs a {@link CNFTranslation} with the given analysis constraint and initializes variables using the provided
     * {@link DataFlowDiagramAndDictionary}.
     * @param analysisConstraint the {@link AnalysisConstraint} to translate
     * @param dataFlowDiagramAndDictionary the data flow diagram and dictionary used to initialize variables
     */
    public CNFTranslation(AnalysisConstraint analysisConstraint, DataFlowDiagramAndDictionary dataFlowDiagramAndDictionary) {
        this(analysisConstraint);
        initialiseVariables(dataFlowDiagramAndDictionary);
    }

    /**
     * Constructs the CNF representation of the analysis constraint by initializing the translation, combining the base and
     * conditional formulas, and converting the result to CNF.
     * @return a list of {@link Constraint} objects representing the CNF
     */
    public List<Constraint> constructCNF() {
        initialiseTranslation();
        cnfFormula = new BaseFormula().add(baseFormula)
                .add(conditionalFormula);
        cnf = cnfFormula.toCNF();
        return cnf;
    }

    private void initialiseTranslation() {
        List<AbstractSelector> selectors = new ArrayList<>();
        selectors.addAll(analysisConstraint.getDataSourceSelectors()
                .getSelectors());
        selectors.addAll(analysisConstraint.getVertexSourceSelectors()
                .getSelectors());
        selectors.addAll(analysisConstraint.getVertexDestinationSelectors()
                .getSelectors());

        selectors.forEach(this::initialiseSelector);
        analysisConstraint.getConditionalSelectors()
                .getSelectors()
                .forEach(this::initialiseConditionalSelector);

        constructBaseFormula();
        constructConditionalFormula();
    }

    private void initialiseSelector(AbstractSelector selector) {
        if (selector instanceof DataCharacteristicsSelector dataCharacteristicsSelector) {
            var value = dataCharacteristicsSelector.getDataCharacteristic()
                    .characteristicValue();
            if (value.isConstant()) {
                constantSelectors.add(new ConstantDataCharacteristicSelector(dataCharacteristicsSelector));
            } else {
                dynamicSelectors.put(value.name(), new DynamicDataCharacteristicSelector(dataCharacteristicsSelector));
            }
            return;
        }
        if (selector instanceof VertexCharacteristicsSelector vertexCharacteristicsSelector) {
            var value = vertexCharacteristicsSelector.getVertexCharacteristics()
                    .characteristicValue();
            if (value.isConstant()) {
                constantSelectors.add(new ConstantVertexCharacteristicSelector(vertexCharacteristicsSelector));
            } else {
                dynamicSelectors.put(value.name(), new DynamicVertexCharacteristicSelector(vertexCharacteristicsSelector));
            }
            return;
        }
        if (selector instanceof DataCharacteristicListSelector dataCharacteristicListSelector) {
            constantSelectors.add(new ConstantDataCharacteristicListSelector(dataCharacteristicListSelector));
            return;
        }
        if (selector instanceof VertexCharacteristicsListSelector vertexCharacteristicsListSelector) {
            constantSelectors.add(new ConstantVertexCharacteristicListSelector(vertexCharacteristicsListSelector));
            return;
        }
        throw new IllegalArgumentException("Unexpected selector type: " + selector);
    }

    private void initialiseConditionalSelector(ConditionalSelector conditionalSelector) {
        if (conditionalSelector instanceof VariableConditionalSelector variableConditionalSelector) {
            conditionalSelectors.add(new DynamicVariableConditionalSelector(variableConditionalSelector));
            return;
        }
        if (conditionalSelector instanceof EmptySetOperationConditionalSelector emptySetOperationConditionalSelector) {
            conditionalSelectors.add(new DynamicEmptySetOperationConditionalSelector(emptySetOperationConditionalSelector));
            return;
        }
        throw new IllegalArgumentException("Unexpected conditional selector type: " + conditionalSelector);
    }

    private void constructBaseFormula() {
        baseFormula = new BaseFormula();
        constantSelectors.forEach(it -> it.addLiterals(baseFormula.getRoot()));
    }

    private void constructConditionalFormula() {
        conditionalFormula = new BaseFormula();

        if (conditionalSelectors.isEmpty()) {
            return;
        }
        if (variables.isEmpty()) {
            throw new IllegalStateException("Variables are empty.");
        }

        conditionalSelectors.forEach(it -> it.addLiterals(conditionalFormula.getRoot(), dynamicSelectors, variables));
    }

    private void initialiseVariables(DataFlowDiagramAndDictionary dfd) {
        dfd.dataDictionary()
                .getLabelTypes()
                .forEach(it -> variables.put(it.getEntityName(), it.getLabel()
                        .stream()
                        .map(NamedElement::getEntityName)
                        .toList()));
    }

    /**
     * Returns a string representation of the CNF formula, prefixed with a line separator and an exclamation mark.
     * @return the formatted string representation of the CNF formula
     */
    public String formulaToString() {
        var formulaString = cnfFormula.toString();
        return "%s!%s".formatted(System.lineSeparator(), formulaString);
    }

    /**
     * Returns a formatted string representation of the CNF, showing each constraint and its literals. Each literal is
     * displayed with its polarity, category, type, and value, combined using "OR" within constraints and "AND" between
     * constraints.
     * @return the formatted string representation of the CNF
     */
    public String cnfToString() {
        var s = new StringBuilder();
        s.append(System.lineSeparator());
        for (var constraint : cnf) {
            s.append("( ");
            for (var literal : constraint.literals()) {
                var positive = literal.positive() ? "" : "!";
                var label = literal.compositeLabel()
                        .label();
                s.append("%s[%s %s.%s]".formatted(positive, literal.compositeLabel()
                        .category()
                        .name(), label.type(), label.value()));
                s.append(" OR ");
            }
            s.delete(s.length() - 3, s.length());
            s.append(") AND");
            s.append(System.lineSeparator());
        }
        s.delete(s.length() - 5, s.length());
        return s.toString();
    }

    /**
     * Returns a simplified string representation of the CNF using indices for literals. Each literal is represented by its
     * index in the order of first occurrence, with negation indicated by "!". Constraints are enclosed in brackets.
     * @return the simplified string representation of the CNF
     */
    public String simpleCNFToString() {
        var s = new StringBuilder();
        var literals = new ArrayList<Literal>();
        s.append(System.lineSeparator());
        for (var constraint : cnf) {
            s.append("[");
            for (var literal : constraint.literals()) {
                if (literals.contains(literal)) {
                    s.append(literal.positive() ? "" : "!");
                    s.append(literals.indexOf(literal));
                    s.append(" ");
                } else {
                    s.append(literal.positive() ? "" : "!")
                            .append(literals.size())
                            .append(" ");
                    literals.add(literal);
                }
            }
            s.append("]");
            s.append(System.lineSeparator());
        }
        return s.toString();
    }

    /**
     * Returns a summary of CNF statistics, including the number of clauses, total literals, length of the longest clause,
     * and the average number of literals per clause.
     * @return a formatted string containing CNF statistics
     */
    public String getCNFStatistics() {
        return System.lineSeparator() + "Clauses: " + outputClauses() + System.lineSeparator() + "Literals: " + outputLiterals()
                + System.lineSeparator() + "Longest Clause: " + outputLongestClause() + System.lineSeparator() + "Literals per Clause (avg): "
                + (float) (outputLiterals()) / outputClauses() + System.lineSeparator();
    }

    /**
     * Returns the number of clauses in the CNF.
     * @return the total number of clauses
     */
    public int outputClauses() {
        return cnf.size();
    }

    /**
     * Returns the total number of literals across all CNF clauses.
     * @return the sum of literals in all clauses
     */
    public int outputLiterals() {
        return cnf.stream()
                .map(it -> it.literals()
                        .size())
                .reduce(0, Integer::sum);
    }

    /**
     * Returns the size of the longest clause in the CNF.
     * @return the number of literals in the longest clause
     */
    public int outputLongestClause() {
        var longest = 0;
        for (Constraint constraint : cnf) {
            var len = constraint.literals()
                    .size();
            if (len > longest) {
                longest = len;
            }
        }
        return longest;
    }
}