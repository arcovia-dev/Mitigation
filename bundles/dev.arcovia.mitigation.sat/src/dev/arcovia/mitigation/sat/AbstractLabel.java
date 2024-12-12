package dev.arcovia.mitigation.sat;

public abstract class AbstractLabel {
    private final LabelCategory category;
    private final Label label;

    public AbstractLabel(LabelCategory category, Label label) {
        this.category = category;
        this.label = label;
    }

    public LabelCategory category() {
        return category;
    }

    public String type() {
        return label.type();
    }

    public String value() {
        return label.value();
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

        AbstractLabel that = (AbstractLabel) o;

        return category.equals(that.category()) && label.type()
                .equals(that.type())
                && label.value()
                        .equals(that.value());
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
