package dev.arcovia.mitigation.sat;

/**
 * Represents a literal that consists of a polarity (positive or negative) and a composite label. A literal is a
 * compound data structure used in contexts where logical expressions or categorical labeling are involved. It combines
 * a boolean value indicating its polarity (positive or negative) with a {@link CompositeLabel}, which provides the
 * categorical and descriptive context of the literal.
 */
public record Literal(boolean positive, CompositeLabel compositeLabel) {

}
