package dev.arcovia.mitigation.sat;

public class IncomingDataLabel extends AbstractLabel {

    public IncomingDataLabel(String type, String value) {
        super(LabelCategory.IncomingData, new Label(type, value));
    }

}
