package dev.arcovia.mitigation.sat;

import java.util.List;

public record Term(String domain, AbstractChar characteristic) {

    @Override
    public String toString() {
        return List.of(domain, characteristic.what(), characteristic.type(), characteristic.value())
                .toString();
    }
}
