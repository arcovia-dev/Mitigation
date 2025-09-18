package dev.arcovia.mitigation.sat.dsl.tests.dummy;

import dev.arcovia.mitigation.sat.LabelCategory;
import dev.arcovia.mitigation.sat.dsl.tests.utility.CNFUtil;

public record DInData(boolean positive, String type, String value) {
    public DInData(boolean positive) {
        this(positive, LabelCategory.IncomingData.name(), String.valueOf(CNFUtil.getNewValue()));
    }
}
