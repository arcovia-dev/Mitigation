package dev.arcovia.mitigation.utils.dsl.tests.utility;

import dev.arcovia.mitigation.sat.*;
import dev.arcovia.mitigation.utils.dsl.tests.dummy.DInData;
import dev.arcovia.mitigation.utils.dsl.tests.dummy.DNode;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class providing helper methods for constructing, manipulating, and comparing CNF (Conjunctive Normal Form)
 * constraints for testing purposes.
 * <p>
 * This class includes methods to generate clauses from test data, obtain unique literal values, compare CNF lists for
 * differences, check literal equality in constraints, and produce formatted string representations of CNF constraints.
 */
public abstract class CNFUtil {

    private static final Logger logger = Logger.getLogger(CNFUtil.class);
    private static int literalValueCounter = 0;

    /**
     * Returns a unique integer value for use in literals and increments the internal counter.
     * @return a new unique integer value
     */
    public static int getNewValue() {
        return literalValueCounter++;
    }

    /**
     * Generates a {@link Constraint} clause from the given incoming data and node lists. Each item is converted to a
     * literal with inverted polarity.
     * @param incomingData the list of {@link DInData} elements
     * @param nodes the list of {@link DNode} elements
     * @return a {@link Constraint} representing the generated clause
     */
    public static Constraint generateClause(List<DInData> incomingData, List<DNode> nodes) {
        ArrayList<Literal> constraint = new ArrayList<>();
        incomingData.forEach(
                it -> constraint.add(new Literal(!it.positive(), new IncomingDataLabel(new Label(LabelCategory.IncomingData.name(), it.value())))));
        nodes.forEach(it -> constraint.add(new Literal(!it.positive(), new NodeLabel(new Label(LabelCategory.Node.name(), it.value())))));
        return new Constraint(constraint);
    }

    /**
     * Returns the greatest differences between two lists of CNF {@link Constraint} objects. If the lists are identical,
     * returns an empty list. Differences are determined by matching constraints.
     * @param expected the list of expected {@link Constraint} objects
     * @param actual the list of actual {@link Constraint} objects
     * @return the list of constraints representing the greatest differences
     */
    public static List<Constraint> getGreatestDifference(List<Constraint> expected, List<Constraint> actual) {
        var expectedDifferences = new ArrayList<>(expected);
        var actualDifferences = new ArrayList<>(actual);
        expected.forEach(constraint -> actual.forEach(it -> {
            if (matches(constraint, it)) {
                expectedDifferences.remove(constraint);
                actualDifferences.remove(it);
            }
        }));

        logger.info("Differences in expected: " + expectedDifferences);
        logger.info("Differences in actual: " + actualDifferences);

        return expectedDifferences.size() > actualDifferences.size() ? expectedDifferences : actualDifferences;
    }

    /**
     * Checks whether two {@link Constraint} objects match exactly by comparing their literals.
     * @param expected the expected {@link Constraint}
     * @param actual the actual {@link Constraint}
     * @return true if both constraints contain the same literals, false otherwise
     */
    public static boolean matches(Constraint expected, Constraint actual) {
        var differences = new ArrayList<>(expected.literals());
        actual.literals()
                .forEach(differences::remove);
        if (!differences.isEmpty()) {
            return false;
        }

        differences = new ArrayList<>(actual.literals());
        expected.literals()
                .forEach(differences::remove);

        return differences.isEmpty();
    }

    /**
     * Returns a formatted string representation of a given CNF list of {@link Constraint} objects. Each constraint is shown
     * with its literals using "OR" within the constraint and "AND" between constraints.
     * @param cnf the list of CNF {@link Constraint} objects to format
     * @return the formatted string representation of the CNF
     */
    public static String cnfToString(List<Constraint> cnf) {
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
}
