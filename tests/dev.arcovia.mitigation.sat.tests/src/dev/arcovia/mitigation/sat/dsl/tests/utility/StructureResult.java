package dev.arcovia.mitigation.sat.dsl.tests.utility;

public record StructureResult(int dataPos, int dataNeg, int nodePos, int nodeNeg, int inputLiterals, int outputClauses, int outputLiterals, int outputLongestClause,
        int outputLiteralsPerClause, int runtime) {
}
