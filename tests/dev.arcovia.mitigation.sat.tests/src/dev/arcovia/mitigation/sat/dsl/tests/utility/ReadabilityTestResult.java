package dev.arcovia.mitigation.sat.dsl.tests.utility;

/**
 * Represents the results of a readability test for CNF formulas.
 * <p>
 * Contains metrics including the number of input literals, the number of output clauses, the total number of output
 * literals, the length of the longest clause, and the average number of literals per clause.
 */
public record ReadabilityTestResult(int inputLiterals, int outputClauses, int outputLiterals, int outputLongestClause,
        float outputLiteralsPerClause) {
}
