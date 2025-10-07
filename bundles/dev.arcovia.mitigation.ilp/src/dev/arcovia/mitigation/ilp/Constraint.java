package dev.arcovia.mitigation.ilp;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;

public class Constraint {
    public final AnalysisConstraint dsl;
    
    public Constraint(AnalysisConstraint dsl) {
        this.dsl = dsl;
    }
}
