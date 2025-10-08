package dev.arcovia.mitigation.ilp;

import dev.arcovia.mitigation.sat.CompositeLabel;

public record MitigationStrategy(CompositeLabel label,double cost, MitigationType type) {

}
