package dev.arcovia.mitigation.sat;

/**
 * Represents a term consisting of a specific domain and an associated composite label.
 *
 * The {@code Term} class is a record that combines a domain string with a
 * {@link CompositeLabel}, which encapsulates a label category and its label.
 * The term provides a structured way of expressing the relationship between
 * a domain and the associated composite label.
 *
 * The class overrides the {@code toString()} method to provide a meaningful
 * representation of the term. Specifically:
 * - If the {@code compositeLabel} category is {@code Node}, the string representation
 *   indicates that the domain "has Property" corresponding to the label.
 * - For other categories, the representation highlights the label associated
 *   with a pin identified by the domain.
 */
public record Term(String domain, CompositeLabel compositeLabel) {

    @Override
    public String toString() {
        if (compositeLabel.category()
                .equals(LabelCategory.Node))
            return (compositeLabel.category() + " " + domain + " has Property " + compositeLabel.label()
                    .toString());
        else
            return (compositeLabel.category() + " at Pin " + domain + " has Label " + compositeLabel.label()
                    .toString());
    }
}
