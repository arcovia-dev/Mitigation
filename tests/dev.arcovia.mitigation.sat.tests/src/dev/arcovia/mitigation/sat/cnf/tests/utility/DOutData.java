package dev.arcovia.mitigation.sat.cnf.tests.utility;

import dev.arcovia.mitigation.sat.LabelCategory;

public record DOutData(boolean positive, String type, String value) {
    public DOutData(boolean positive) {
        this(positive, LabelCategory.OutgoingData.name(), String.valueOf(CNFUtil.getNewValue()));
    }
}
