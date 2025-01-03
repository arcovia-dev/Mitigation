package dev.arcovia.mitigation.sat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record Node(UUID id, String name, Map<InPin, List<Label>> inPins, Map<OutPin, List<Label>> outPins, List<Label> nodeChars) {

}
