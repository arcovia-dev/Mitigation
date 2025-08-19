package dev.arcovia.mitigation.sat.cnf.tests.utility;

import dev.arcovia.mitigation.sat.Constraint;

import java.util.List;

public record DCNF(List<Constraint> clauses) {
}
