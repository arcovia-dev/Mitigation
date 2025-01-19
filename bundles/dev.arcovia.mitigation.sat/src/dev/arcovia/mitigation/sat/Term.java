package dev.arcovia.mitigation.sat;

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
