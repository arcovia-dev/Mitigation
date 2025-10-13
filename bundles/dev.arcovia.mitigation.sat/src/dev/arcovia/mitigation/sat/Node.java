package dev.arcovia.mitigation.sat;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a Node entity that models a connection point with a set of input pins, output pins, and associated labels
 * containing characteristics or properties.
 * @param id Unique identifier of the Node.
 * @param inPins A map of input pins to their associated lists of labels.
 * @param outPins A TreeMap of output pins to their associated lists of labels.
 * @param nodeChars A list of labels representing general characteristics of the Node.
 */
public record Node(String id, Map<InPin, List<Label>> inPins, TreeMap<OutPin, List<Label>> outPins, List<Label> nodeChars) {

}
