package dev.arcovia.mitigation.sat;

public class OutgoingDataCharacteristic extends AbstractCharacteristic {

    public OutgoingDataCharacteristic(String type, String value) {
        super(CharacteristicCategory.OutgoingData, type, value);
    }

}
