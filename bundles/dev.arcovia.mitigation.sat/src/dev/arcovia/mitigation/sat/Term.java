package dev.arcovia.mitigation.sat;

import java.util.List;

public record Term(String domain, AbstractCharacteristic characteristic) {

    @Override
    public String toString() {
        return List.of(domain, characteristic.category(), characteristic.type(), characteristic.value())
                .toString();
    }
}
