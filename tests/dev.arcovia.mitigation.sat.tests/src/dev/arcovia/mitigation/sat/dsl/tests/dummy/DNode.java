package dev.arcovia.mitigation.sat.dsl.tests.dummy;

import dev.arcovia.mitigation.sat.LabelCategory;
import dev.arcovia.mitigation.sat.dsl.tests.utility.CNFUtil;

public record DNode(boolean positive, String type, String value) {
    public DNode(boolean positive) {
        this(positive, LabelCategory.Node.name(), String.valueOf(CNFUtil.getNewValue()));
    }
}
