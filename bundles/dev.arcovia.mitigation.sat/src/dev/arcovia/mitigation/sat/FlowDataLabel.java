package dev.arcovia.mitigation.sat;

/**
 * The FlowDataLabel record encapsulates a Flow and its associated IncomingDataLabel.
 * It is used to represent the pairing of a unidirectional data Flow with a specific label
 * that provides additional context or metadata for the incoming data in the flow.
 *
 * @param flow the unidirectional Flow that is being labeled
 * @param incomingDataLabel the IncomingDataLabel that provides metadata or context for data within the Flow
 */
public record FlowDataLabel(Flow flow, IncomingDataLabel incomingDataLabel) {
    @Override
    public String toString() {
        return (flow.toString() + " has Label: " + incomingDataLabel.label());
    }
}
