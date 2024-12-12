package dev.arcovia.mitigation.sat;

public record Label(String type, String value) {
    @Override
    public String toString() {
        return ("(" + type + ", " + value +")");
    }
}
