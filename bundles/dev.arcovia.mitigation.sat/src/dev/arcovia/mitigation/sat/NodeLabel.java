package dev.arcovia.mitigation.sat;

public class NodeLabel extends AbstractLabel {

    public NodeLabel(String type, String value) {
        super(LabelCategory.Node, new Label(type, value));
    }
}
