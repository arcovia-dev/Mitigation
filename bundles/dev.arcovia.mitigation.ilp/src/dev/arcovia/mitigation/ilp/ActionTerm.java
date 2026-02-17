package dev.arcovia.mitigation.ilp;

import java.util.List;

import dev.arcovia.mitigation.sat.CompositeLabel;

public record ActionTerm(String domain, List<CompositeLabel> compositeLabels, ActionType type) {

}
