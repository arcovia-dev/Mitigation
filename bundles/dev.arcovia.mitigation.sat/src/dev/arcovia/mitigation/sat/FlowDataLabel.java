package dev.arcovia.mitigation.sat;

public record FlowDataLabel(Flow flow, IncomingDataLabel incomingDataLabel) {
    @Override
    public String toString() {
        return (flow.toString() + " has Label: " + incomingDataLabel.label());
    }
}
