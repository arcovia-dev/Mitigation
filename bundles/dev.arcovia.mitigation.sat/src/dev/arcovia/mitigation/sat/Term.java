package dev.arcovia.mitigation.sat;

public record Term(String domain, AbstractLabel label) {

    @Override
    public String toString() {
        if (label.category()
                .equals(LabelCategory.Node))
            return (label.category() + " " + domain + " has Property (" + label.type() + (", ") + label.value() + ")");
        else
            return (label.category() + " at Pin " + domain + " has Property (" + label.type() + (", ") + label.value() + ")");
    }
}
