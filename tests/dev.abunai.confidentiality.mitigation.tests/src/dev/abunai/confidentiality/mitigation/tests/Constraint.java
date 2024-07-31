package dev.abunai.confidentiality.mitigation.tests;

public record Constraint(boolean positive, AbstractChar characteristic) {

    public Constraint(boolean positive, AbstractChar characteristic) {
        this.positive = positive;
        if (characteristic instanceof OutDataChar cast) {
            this.characteristic = cast.toIn();
        } else {
            this.characteristic = characteristic;
        }
    }
}
