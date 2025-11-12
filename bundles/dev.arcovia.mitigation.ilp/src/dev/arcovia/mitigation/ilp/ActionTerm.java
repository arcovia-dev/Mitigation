package dev.arcovia.mitigation.ilp;

import dev.arcovia.mitigation.sat.CompositeLabel;

public record ActionTerm(String domain, CompositeLabel compositeLabel, ActionType type) {

}
