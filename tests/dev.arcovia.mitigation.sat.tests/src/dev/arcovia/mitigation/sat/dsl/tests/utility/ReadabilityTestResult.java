package dev.arcovia.mitigation.sat.dsl.tests.utility;

public record ReadabilityTestResult(
        int inputLiterals,
        int outputClauses,
        int outputLiterals,
        int outputLongestClause,
        float outputLiteralsPerClause
) {}
