package dev.arcovia.mitigation.sat;

public class OutgoingDataLabel extends AbstractLabel {

    public OutgoingDataLabel(String type, String value) {
        super(LabelCategory.OutgoingData, new Label (type, value));
    }

}
