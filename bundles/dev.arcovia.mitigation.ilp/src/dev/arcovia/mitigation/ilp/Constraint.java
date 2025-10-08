package dev.arcovia.mitigation.ilp;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import java.util.List;

public class Constraint {
    public final AnalysisConstraint dsl;
    private final List<MitigationStrategy> mitigations;
    
    
    public Constraint(AnalysisConstraint dsl, List<MitigationStrategy> mitigations) {
        this.dsl = dsl;
        this.mitigations = mitigations;
    }
    
    public Constraint(AnalysisConstraint dsl) {
        this.dsl = dsl;
        this.mitigations = null;
        
    }
    
    public List<MitigationStrategy> getMitigations() {
        return mitigations;
    }
    
}
