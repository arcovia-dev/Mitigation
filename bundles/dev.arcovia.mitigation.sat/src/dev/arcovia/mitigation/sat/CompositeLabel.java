package dev.arcovia.mitigation.sat;

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
