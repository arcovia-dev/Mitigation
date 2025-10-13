package dev.arcovia.mitigation.sat;

/**
 * Represents a label with a type and value. The class provides a customized implementation of the toString method to
 * display the label in a formatted string representation.
 * @param type the type of the label as a string
 * @param value the value of the label as a string
 */
public record Label(String type, String value) {
    @Override
    public String toString() {
        return ("(" + type + ", " + value + ")");
    }
}
