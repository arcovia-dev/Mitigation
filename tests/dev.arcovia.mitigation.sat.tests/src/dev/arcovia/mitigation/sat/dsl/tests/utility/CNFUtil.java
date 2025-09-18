package dev.arcovia.mitigation.sat.dsl.tests.utility;

import dev.arcovia.mitigation.sat.*;
import dev.arcovia.mitigation.sat.dsl.tests.dummy.DInData;
import dev.arcovia.mitigation.sat.dsl.tests.dummy.DNode;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public abstract class CNFUtil {

    private static final Logger logger = Logger.getLogger(CNFUtil.class);

    private static int literalValueCounter = 0;
    public static int getNewValue() {
        return literalValueCounter++;
    }

    public static Constraint generateClause(List<DInData> incomingData, List<DNode> nodes) {
        ArrayList<Literal> constraint = new ArrayList<>();
        incomingData.forEach(it ->
                constraint.add(new Literal(!it.positive(), new IncomingDataLabel(new Label(LabelCategory.IncomingData.name(), it.value())))));
        nodes.forEach(it ->
                constraint.add(new Literal(!it.positive(), new NodeLabel(new Label(LabelCategory.Node.name(), it.value())))));
        return new Constraint(constraint);
    }

    // returns greatest differences between two clauses or empty list if the same
    public static List<Constraint> compare(List<Constraint> expected, List<Constraint> actual) {
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

    public static boolean matches(Constraint expected, Constraint actual) {
        var differences = new ArrayList<>(expected.literals());
        actual.literals().forEach(differences::remove);
        if(!differences.isEmpty()) { return false; }

        differences = new ArrayList<>(actual.literals());
        expected.literals().forEach(differences::remove);

        return differences.isEmpty();
    }


    public static String cnfToString(List<Constraint> cnf) {
        var s = new StringBuilder();
        s.append("\n");
        for (var constraint : cnf) {
            s.append("( ");
            for (var literal : constraint.literals()) {
                var positive = literal.positive() ? "" : "!";
                var label = literal.compositeLabel().label();
                s.append("%s[%s %s.%s]".formatted(positive, literal.compositeLabel().category().name(), label.type(), label.value()));
                s.append(" OR ");
            }
            s.delete(s.length() - 3, s.length());
            s.append(") AND");
            s.append("\n");
        }
        s.delete(s.length() - 5, s.length());
        return s.toString();
    }
}
