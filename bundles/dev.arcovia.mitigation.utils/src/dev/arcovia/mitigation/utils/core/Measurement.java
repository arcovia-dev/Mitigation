package dev.arcovia.mitigation.utils.core;

public record Measurement(
        String timestamp,
        String runId,
        String key,
        String experiment,
        double tfgLengthScaling,
        double tfgAmountScaling,
        int amountConstraint,
        int numberWithLabel,
        int numberWithoutLabel,
        int numberWithCharacteristic,
        int numberWithoutCharacteristic,
        int numberDummyLabels,
        String runType,
        int repeatIndex,
        long executionTime,
        long solvingTime,
        long isolatedExecution,
        String status,
        String error
) {}
