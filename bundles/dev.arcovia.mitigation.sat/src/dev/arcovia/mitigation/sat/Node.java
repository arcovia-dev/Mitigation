package dev.arcovia.mitigation.sat;

import java.util.List;
import java.util.LinkedHashMap;

public record Node(String id, LinkedHashMap<InPin, List<Label>> inPins, LinkedHashMap<OutPin, List<Label>> outPins, List<Label> nodeChars) {

}
