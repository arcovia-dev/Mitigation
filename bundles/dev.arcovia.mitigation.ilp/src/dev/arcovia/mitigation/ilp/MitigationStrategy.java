package dev.arcovia.mitigation.ilp;

import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;

import dev.arcovia.mitigation.sat.CompositeLabel;

public class MitigationStrategy {
    List<CompositeLabel> label;
    double cost;
    MitigationType type;
    List<MitigationStrategy> required = new ArrayList<>();
    List<Constraint> notAllowedIfViolated = new ArrayList<>();

    public MitigationStrategy(List<CompositeLabel> label, double cost, MitigationType type) {
        this.label = label;
        this.cost = cost;
        this.type = type;
    }

    public void addRequired(List<MitigationStrategy> required) {
        for (var mitigation : required) {
            if (!mitigation.type.toString()
                    .startsWith("Delete"))
                this.required.add(mitigation);
        }
    }

    public void addConstraint(Constraint constraint) {
        notAllowedIfViolated.add(constraint);
    }

    public boolean checkIfAllowed(DFDVertex vertex) {
        for (var constraint : notAllowedIfViolated) {
            if (constraint.isMatched(vertex))
                return false;
        }
        return true;
    }
}
