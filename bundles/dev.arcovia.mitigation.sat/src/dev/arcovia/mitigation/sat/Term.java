package dev.arcovia.mitigation.sat;

public record Term(String domain, AbstractLabel label) {

    @Override
    public String toString() {
        if (label.category()
                .equals(LabelCategory.Node))
            return (label.category() + " " + domain + " has Property " + label.label().toString());
        else
            return (label.category() + " at Pin " + domain + " has Label " + label.label().toString());
    }
}
