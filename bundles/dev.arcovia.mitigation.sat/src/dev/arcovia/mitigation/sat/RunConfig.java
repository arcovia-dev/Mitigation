package dev.arcovia.mitigation.sat;


public record RunConfig(
        String experiment,
        int tfgLengthScaling,
        int tfgAmountScaling,
        int amountConstraint,
        int numberWithLabel,
        int numberWithoutLabel,
        int numberWithCharacteristic,
        int numberWithoutCharacteristic,
        int numberDummyLabels
) {
    public static RunConfig forTFG(String experiment, int tfgLengthScaling, int tfgAmountScaling) {
        return new RunConfig(experiment, tfgLengthScaling, tfgAmountScaling,
                1, 1, 1, 1, 1, 0);
    }

    public static RunConfig forConstraints(
            String experiment,
            int amountConstraint,
            int numberWithLabel,
            int numberWithoutLabel,
            int numberWithCharacteristic,
            int numberWithoutCharacteristic,
            int numberDummyLabels
    ) {
        return new RunConfig(experiment, 0, 0,
                amountConstraint, numberWithLabel, numberWithoutLabel,
                numberWithCharacteristic, numberWithoutCharacteristic,
                numberDummyLabels);
    }

    String key() {
        return String.join("|",
                experiment,
                "tfgL=" + tfgLengthScaling,
                "tfgA=" + tfgAmountScaling,
                "ac=" + amountConstraint,
                "wl=" + numberWithLabel,
                "wol=" + numberWithoutLabel,
                "wc=" + numberWithCharacteristic,
                "woc=" + numberWithoutCharacteristic,
                "dummy=" + numberDummyLabels
        );
    }
}
