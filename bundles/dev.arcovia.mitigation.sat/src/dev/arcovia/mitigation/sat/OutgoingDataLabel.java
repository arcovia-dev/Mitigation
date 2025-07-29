package dev.arcovia.mitigation.sat;

/**
 * Represents a label categorized as {@link LabelCategory#OutgoingData}.
 *
 * This class extends {@link CompositeLabel}, associating a specific category
 * of outgoing data with a {@link Label}. It serves to provide clear semantic
 * representation for labels related to outgoing data.
 *
 * Instances of this class are immutable and utilize the parent {@code CompositeLabel}'s
 * standard implementations for equality, hashing, and string representation.
 *
 * @see CompositeLabel
 * @see Label
 * @see LabelCategory
 */
public class OutgoingDataLabel extends CompositeLabel {

    public OutgoingDataLabel(Label label) {
        super(LabelCategory.OutgoingData, label);
    }

}
