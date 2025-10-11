package dev.arcovia.mitigation.sat;

/**
 * Represents a unidirectional flow from a source OutPin to a sink InPin. A Flow object establishes a connection between
 * the output pin and input pin, providing a way to model system design or data flow connections.
 * @param source the source OutPin where the flow originates
 * @param sink the sink InPin where the flow terminates
 */
public record Flow(OutPin source, InPin sink) {
    @Override
    public String toString() {
        return ("Flow from OutPin: " + source.id() + " to InPin: " + sink.id());
    }
}
