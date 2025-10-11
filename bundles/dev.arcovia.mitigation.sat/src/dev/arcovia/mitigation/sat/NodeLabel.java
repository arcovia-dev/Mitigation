package dev.arcovia.mitigation.sat;

/**
 * Represents a label specifically categorized as a {@link LabelCategory#Node}. This class is a concrete implementation
 * of the {@link CompositeLabel} class, associating the {@code LabelCategory.Node} category with a specific
 * {@link Label}. It is used to define and encapsulate labels that are explicitly associated with nodes within a system,
 * ensuring consistency for such categorizations. Instances of this class are immutable, inheriting the immutability
 * guarantees from its superclass {@link CompositeLabel}. Equality and hash code calculations, as well as string
 * representations, are provided by the superclass implementation.
 */
public class NodeLabel extends CompositeLabel {

    public NodeLabel(Label label) {
        super(LabelCategory.Node, label);
    }
}
