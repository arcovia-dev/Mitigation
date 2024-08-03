package dev.abunai.confidentiality.mitigation.sat;

import java.util.List;

public record Delta(String where, AbstractChar characteristic) {
    
    @Override
    public String toString() {
        return List.of(where,characteristic.what(),characteristic.type(),characteristic.value()).toString();
    }
}
