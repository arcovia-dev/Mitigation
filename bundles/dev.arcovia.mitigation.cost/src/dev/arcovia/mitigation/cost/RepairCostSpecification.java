package dev.arcovia.mitigation.cost;

import java.util.*;

/**
 * Immutable configuration of the thesis repair-cost function.
 */
public final class RepairCostSpecification {
    private final String id;
    private final String version;
    private final String unit;
    private final EnumMap<ActionType, Double> baseCosts;
    private final EnumMap<ActionType, Double> assuranceActivities;
    private final double impactCoefficient;
    private final double rippleCoefficient;
    private final double structuralCoefficient;
    private final double assuranceCoefficient;
    private final Map<RepairActionKey, Double> actionCostOverrides;
    private final List<SharedCostGroup> sharedCostGroups;

    /**
     * Copies a validated builder into an immutable specification.
     *
     * @param builder the source builder
     */
    private RepairCostSpecification(Builder builder) {
        id = builder.id;
        version = builder.version;
        unit = builder.unit;
        baseCosts = new EnumMap<>(builder.baseCosts);
        assuranceActivities = new EnumMap<>(builder.assuranceActivities);
        impactCoefficient = builder.impactCoefficient;
        rippleCoefficient = builder.rippleCoefficient;
        structuralCoefficient = builder.structuralCoefficient;
        assuranceCoefficient = builder.assuranceCoefficient;
        actionCostOverrides = Map.copyOf(builder.actionCostOverrides);
        sharedCostGroups = List.copyOf(builder.sharedCostGroups);
    }

    /**
     * Returns the compatibility profile for the existing provisional scores.
     *
     * @return the dimensionless standard preference profile
     */
    public static RepairCostSpecification standardPreference() {
        return builder("standard-preference", "1")
                .withUnit("preference-score")
                .withBaseCost(ActionType.Adding, 1)
                .withBaseCost(ActionType.Removing, 1_000)
                .withBaseCost(ActionType.AddNode, 10)
                .withBaseCost(ActionType.RemoveNode, 6)
                .withBaseCost(ActionType.RemoveFlow, 4)
                .withBaseCost(ActionType.AddSink, 8)
                .build();
    }

    /**
     * Returns the profile that charges every action type once.
     * <p>
     * Under this profile the objective equals the number of selected actions, which is the
     * modification-count measure used by the compared single-model repair approaches.
     *
     * @return the uniform modification-count profile
     */
    public static RepairCostSpecification uniform() {
        Builder builder = builder("uniform", "1").withUnit("modification-count");
        for (ActionType actionType : ActionType.values()) {
            builder.withBaseCost(actionType, 1);
        }
        return builder.build();
    }

    /**
     * Returns a profile that exercises every descriptor term of the cost function.
     * <p>
     * The coefficients are declared, not calibrated: they demonstrate that impact, ripple,
     * structural magnitude, and assurance activities influence the selected repair. They are
     * not an estimate of implementation effort, which would require local productivity data.
     * Assurance activity counts follow the three-part split of requirements and design,
     * secure coding, and verification and validation.
     *
     * @return the descriptor-exercising demonstration profile
     */
    public static RepairCostSpecification effortDemonstration() {
        return builder("effort-demonstration", "1")
                .withUnit("declared-effort-score")
                .withBaseCost(ActionType.Adding, 1)
                .withBaseCost(ActionType.Removing, 2)
                .withBaseCost(ActionType.AddNode, 10)
                .withBaseCost(ActionType.RemoveNode, 6)
                .withBaseCost(ActionType.RemoveFlow, 4)
                .withBaseCost(ActionType.AddSink, 8)
                .withCoefficients(1, 0.1, 2, 1)
                .withAssuranceActivities(ActionType.Adding, 1)
                .withAssuranceActivities(ActionType.Removing, 1)
                .withAssuranceActivities(ActionType.AddNode, 3)
                .withAssuranceActivities(ActionType.RemoveNode, 3)
                .withAssuranceActivities(ActionType.RemoveFlow, 3)
                .withAssuranceActivities(ActionType.AddSink, 3)
                .build();
    }

    /**
     * Starts a cost-specification builder.
     *
     * @param id      the stable specification identifier
     * @param version the specification version
     * @return a builder with zero descriptor coefficients
     */
    public static Builder builder(String id, String version) {
        return new Builder(id, version);
    }

    /**
     * Returns the stable specification identifier.
     *
     * @return the specification identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the specification version.
     *
     * @return the specification version
     */
    public String version() {
        return version;
    }

    /**
     * Returns the declared cost unit.
     *
     * @return a locally defined unit such as preference-score or person-hours
     */
    public String unit() {
        return unit;
    }

    /**
     * Returns the direct base cost for an action type.
     *
     * @param actionType the action type
     * @return the non-negative base cost
     */
    public double baseCostFor(ActionType actionType) {
        return baseCosts.get(Objects.requireNonNull(actionType, "actionType must not be null"));
    }

    /**
     * Returns the assurance activities declared for an action type.
     *
     * @param actionType the action type
     * @return the non-negative assurance activity count
     */
    public double assuranceActivitiesFor(ActionType actionType) {
        return assuranceActivities.get(Objects.requireNonNull(actionType, "actionType must not be null"));
    }

    /**
     * Returns the impact coefficient \(\beta_{\mathrm{Imp}}\).
     *
     * @return the non-negative impact coefficient
     */
    public double impactCoefficient() {
        return impactCoefficient;
    }

    /**
     * Returns the ripple coefficient \(\beta_{\mathrm{Rip}}\).
     *
     * @return the non-negative ripple coefficient
     */
    public double rippleCoefficient() {
        return rippleCoefficient;
    }

    /**
     * Returns the structural coefficient \(\beta_{\mathrm{Str}}\).
     *
     * @return the non-negative structural coefficient
     */
    public double structuralCoefficient() {
        return structuralCoefficient;
    }

    /**
     * Returns the assurance coefficient \(\beta_{\mathrm{Assur}}\).
     *
     * @return the non-negative assurance coefficient
     */
    public double assuranceCoefficient() {
        return assuranceCoefficient;
    }

    /**
     * Returns an explicit direct-cost override when one is declared.
     *
     * @param actionKey the canonical action key
     * @return the override, or empty when the formula applies
     */
    public OptionalDouble actionCostOverrideFor(RepairActionKey actionKey) {
        Double override = actionCostOverrides.get(Objects.requireNonNull(actionKey, "actionKey must not be null"));
        return override == null ? OptionalDouble.empty() : OptionalDouble.of(override);
    }

    /**
     * Returns the declared shared-cost groups.
     *
     * @return the immutable shared-cost groups
     */
    public List<SharedCostGroup> sharedCostGroups() {
        return sharedCostGroups;
    }

    /**
     * Builds immutable thesis cost specifications.
     */
    public static final class Builder {
        private final String id;
        private final String version;
        private String unit = "preference-score";
        private final EnumMap<ActionType, Double> baseCosts = new EnumMap<>(ActionType.class);
        private final EnumMap<ActionType, Double> assuranceActivities = new EnumMap<>(ActionType.class);
        private double impactCoefficient;
        private double rippleCoefficient;
        private double structuralCoefficient;
        private double assuranceCoefficient;
        private final Map<RepairActionKey, Double> actionCostOverrides = new java.util.HashMap<>();
        private final List<SharedCostGroup> sharedCostGroups = new ArrayList<>();

        /**
         * Initializes zero costs and descriptor coefficients.
         *
         * @param id      the stable specification identifier
         * @param version the specification version
         */
        private Builder(String id, String version) {
            this.id = requireText(id, "id");
            this.version = requireText(version, "version");
            for (ActionType actionType : ActionType.values()) {
                baseCosts.put(actionType, 0.0);
                assuranceActivities.put(actionType, 0.0);
            }
        }

        /**
         * Sets the unit used by this specification.
         *
         * @param unit the locally defined unit
         * @return this builder
         */
        public Builder withUnit(String unit) {
            this.unit = requireText(unit, "unit");
            return this;
        }

        /**
         * Sets the base cost for one action type.
         *
         * @param actionType the action type
         * @param baseCost   the non-negative base cost
         * @return this builder
         */
        public Builder withBaseCost(ActionType actionType, double baseCost) {
            baseCosts.put(Objects.requireNonNull(actionType, "actionType must not be null"),
                    requireNonNegative(baseCost, "baseCost"));
            return this;
        }

        /**
         * Sets the four descriptor coefficients.
         *
         * @param impactCoefficient     \(\beta_{\mathrm{Imp}}\)
         * @param rippleCoefficient     \(\beta_{\mathrm{Rip}}\)
         * @param structuralCoefficient \(\beta_{\mathrm{Str}}\)
         * @param assuranceCoefficient  \(\beta_{\mathrm{Assur}}\)
         * @return this builder
         */
        public Builder withCoefficients(double impactCoefficient, double rippleCoefficient,
                                        double structuralCoefficient, double assuranceCoefficient) {
            this.impactCoefficient = requireNonNegative(impactCoefficient, "impactCoefficient");
            this.rippleCoefficient = requireNonNegative(rippleCoefficient, "rippleCoefficient");
            this.structuralCoefficient = requireNonNegative(structuralCoefficient, "structuralCoefficient");
            this.assuranceCoefficient = requireNonNegative(assuranceCoefficient, "assuranceCoefficient");
            return this;
        }

        /**
         * Sets the assurance activities required by one action type.
         *
         * @param actionType the action type
         * @param activities the non-negative activity count
         * @return this builder
         */
        public Builder withAssuranceActivities(ActionType actionType, double activities) {
            assuranceActivities.put(Objects.requireNonNull(actionType, "actionType must not be null"),
                    requireNonNegative(activities, "activities"));
            return this;
        }

        /**
         * Overrides the direct formula cost for one canonical action.
         *
         * @param actionKey  the action to override
         * @param directCost the non-negative direct cost
         * @return this builder
         */
        public Builder withActionCostOverride(RepairActionKey actionKey, double directCost) {
            actionCostOverrides.put(Objects.requireNonNull(actionKey, "actionKey must not be null"),
                    requireNonNegative(directCost, "directCost"));
            return this;
        }

        /**
         * Adds one explicit shared-cost group.
         *
         * @param sharedCostGroup the group to add
         * @return this builder
         */
        public Builder withSharedCostGroup(SharedCostGroup sharedCostGroup) {
            sharedCostGroups.add(Objects.requireNonNull(sharedCostGroup, "sharedCostGroup must not be null"));
            return this;
        }

        /**
         * Builds an immutable and internally consistent specification.
         *
         * @return the completed cost specification
         */
        public RepairCostSpecification build() {
            Set<String> groupIds = new HashSet<>();
            for (SharedCostGroup sharedCostGroup : sharedCostGroups) {
                if (!groupIds.add(sharedCostGroup.id())) {
                    throw new IllegalArgumentException("shared cost group IDs must be unique");
                }
            }
            return new RepairCostSpecification(this);
        }
    }

    /**
     * Requires non-blank metadata.
     *
     * @param value the metadata value
     * @param name  the metadata field name
     * @return the validated value
     */
    private static String requireText(String value, String name) {
        if (Objects.requireNonNull(value, name + " must not be null").isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    /**
     * Requires a finite non-negative numeric value.
     *
     * @param value the numeric value
     * @param name  the numeric field name
     * @return the validated value
     */
    private static double requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
        return value;
    }
}
