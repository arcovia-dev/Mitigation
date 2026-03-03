package dev.arcovia.mitigation.smt.tests.constraints;

import java.util.List;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;

public record SelectorTestCase(String name, AnalysisConstraint constraint, List<String> validSourceFormulas, List<String> validSinkFormulas) {
}