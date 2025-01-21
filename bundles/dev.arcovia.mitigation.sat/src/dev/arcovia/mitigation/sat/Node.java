package dev.arcovia.mitigation.sat;

import java.util.List;
import java.util.Map;

public record Node(String id, Map<InPin, List<Label>> inPins, Map<OutPin, List<Label>> outPins, List<Label> nodeChars) {

}
