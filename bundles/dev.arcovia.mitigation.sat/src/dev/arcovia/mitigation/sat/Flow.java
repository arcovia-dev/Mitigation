package dev.arcovia.mitigation.sat;

public record Flow(OutPin source, InPin sink) {
    @Override
    public String toString() {
        return ("Flow from OutPin: " + source.id() + " to InPin: " + sink.id());
    }
}
