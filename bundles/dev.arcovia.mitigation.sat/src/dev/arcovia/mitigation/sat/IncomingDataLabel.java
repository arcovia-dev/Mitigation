package dev.arcovia.mitigation.sat;

/**
 * Represents a label specifically categorized as {@link LabelCategory#IncomingData}.
 * This class is used to encapsulate a {@link Label} with the predefined category
 * {@link LabelCategory#IncomingData}. It extends the functionality of {@link CompositeLabel},
 * providing a specialized form of a composite label for incoming data labels.
 *
 * The equality, hash code, and string representation of this class are derived from
 * its parent class {@link CompositeLabel}.
 */
public class IncomingDataLabel extends CompositeLabel {

    public IncomingDataLabel(Label label) {
        super(LabelCategory.IncomingData, label);
    }

}
