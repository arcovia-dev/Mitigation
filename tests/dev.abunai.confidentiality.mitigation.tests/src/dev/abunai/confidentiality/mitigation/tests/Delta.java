package dev.abunai.confidentiality.mitigation.tests;

import java.util.List;

public record Delta(String node, AbstractChar characteristic) {
    
    @Override
    public String toString() {
        return List.of(node,characteristic.what(),characteristic.type(),characteristic.value()).toString();
    }
}
