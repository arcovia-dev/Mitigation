package dev.arcovia.mitigation.sat;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record Node(String id, Map<InPin, List<Label>> inPins, TreeMap<OutPin, List<Label>> outPins, List<Label> nodeChars) {

}
