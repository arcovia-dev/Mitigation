package dev.abunai.confidentiality.mitigation.sat;

import java.util.List;
import java.util.Map;

public record Node(String name, List<InPin> inPins, Map<OutPin, List<Label>> outPins, List<Label> nodeChars) {

}
