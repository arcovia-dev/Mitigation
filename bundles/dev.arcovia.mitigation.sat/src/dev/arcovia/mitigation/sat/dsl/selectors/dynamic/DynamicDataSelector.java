package dev.arcovia.mitigation.sat.dsl.selectors.dynamic;

import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.dsl.nodes.BranchNode;

import java.util.List;
import java.util.Map;

/**
 * Represents a dynamic data selector capable of generating literals and labels based on variable mappings.
 * <p>
 * Implementations of this interface dynamically resolve data characteristics (e.g., types and values)
 * at runtime using provided variable maps. These selectors can add corresponding literals to a logical
 * formula tree and also provide the resolved {@link Label} objects.
 *
 * <p>Dynamic selectors differ from constant selectors in that their values are not fixed at compile time
 * but depend on external inputs such as user-defined variable assignments.
 *
 * @see BranchNode
 * @see Label
 */
public interface DynamicDataSelector {
    List<Label> getLabels(Map<String, List<String>> variables);
    void addLiterals(BranchNode root, Map<String, List<String>> variables, boolean inverted);
    boolean isInverted();
}