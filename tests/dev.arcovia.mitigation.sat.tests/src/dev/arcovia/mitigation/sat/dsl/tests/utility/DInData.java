package dev.arcovia.mitigation.sat.dsl.tests.utility;

import dev.arcovia.mitigation.sat.LabelCategory;

public record DInData(boolean positive, String type, String value) {
    public DInData(boolean positive) {
        this(positive, LabelCategory.IncomingData.name(), String.valueOf(CNFUtil.getNewValue()));
    }
}
