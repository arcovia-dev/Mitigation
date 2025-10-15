package dev.arcovia.mitigation.ilp;

import java.util.ArrayList;
import java.util.List;

import dev.arcovia.mitigation.sat.CompositeLabel;

public class MitigationStrategy{
    CompositeLabel label;
    double cost;
    MitigationType type;
    List<MitigationStrategy> required = new ArrayList<>();
    
    public MitigationStrategy(CompositeLabel label,double cost, MitigationType type) {
        this.label = label;
        this.cost = cost;
        this.type = type;
    }
    
    public void addRequired(List<MitigationStrategy> required) {
        this.required.addAll(required);
    }
}
