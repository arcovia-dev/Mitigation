package dev.arcovia.mitigation.sat.dsl.tests.dummy;

import dev.arcovia.mitigation.sat.LabelCategory;
import dev.arcovia.mitigation.sat.dsl.tests.utility.CNFUtil;

public record DOutData(boolean positive, String type, String value) {
    public DOutData(boolean positive) {
        this(positive, LabelCategory.OutgoingData.name(), String.valueOf(CNFUtil.getNewValue()));
    }
}
