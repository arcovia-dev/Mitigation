package dev.arcovia.mitigation.smt;

import java.util.List;
import java.util.Optional;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;

import dev.arcovia.mitigation.smt.operations.Operation;

/**
 * Contains the result of a mitigation attempt
 */
public record SolvingResult(boolean satisfiable, DataFlowDiagramAndDictionary repairedDFD, List<Operation> repairOperations, int repairCost,
        Optional<Long> expressionTreeSize, Optional<Integer> violationsAfter, long solverTimeMs, long findTFGsTimeMs) {
}
