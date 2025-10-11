package dev.arcovia.mitigation.sat;

/**
 * Represents a composite label that combines a {@link LabelCategory} with a {@link Label}. This is an abstract base
 * class, intended to be extended by specific implementations of composite labels with predefined categories. The
 * primary purpose of this class is to encapsulate the association between a category and a label, as well as to provide
 * standard implementations for equality, hashing, and string representation of composite labels. The class ensures
 * immutability by using final fields for its components: {@code category} and {@code label}. Subclasses provide
 * specific categorizations by invoking the constructor with a predefined category and a {@link Label}. Equality and
 * hash code calculations are based on the category, label type, and label value. The string representation summarizes
 * the composite label with its category, type, and value.
 */
public abstract class CompositeLabel {
    private final LabelCategory category;
    private final Label label;

    public CompositeLabel(LabelCategory category, Label label) {
        this.category = category;
        this.label = label;
    }

    public LabelCategory category() {
        return category;
    }

    public Label label() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        CompositeLabel that = (CompositeLabel) o;

        return category.equals(that.category()) && label.equals(that.label());
    }

    @Override
    public int hashCode() {
        int result = category.hashCode();
        result = 31 * result + label.type()
                .hashCode();
        result = 31 * result + label.value()
                .hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AbstractChar[" + "what=" + category + ", type=" + label.type() + ", value=" + label.value() + ']';
    }
}
