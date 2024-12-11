package dev.arcovia.mitigation.sat;


public record Term(String domain, AbstractLabel characteristic) {

    @Override
    public String toString() {
        if (characteristic.category().equals(LabelCategory.Node))
            return (characteristic.category() + " " + domain + " has Label (" + characteristic.type() + (", ") +characteristic.value() + ")");
        else 
            return (characteristic.category() + " at " + domain + " has Label (" + characteristic.type() + (", ") +characteristic.value() + ")");
    }
}
