package dev.arcovia.mitigation.smt.operations;

import java.util.List;

import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.SetAssignment;

/**
 * This operation creates a Set Assignment for a specific label and pin
 */
public final class SetAssignmentOperation extends AbstractPinAssignmentOperation<SetAssignment> {

    public SetAssignmentOperation(Pin pin, Label label) {
        super(pin, label);
    }

    @Override
    protected SetAssignment createAssignment() {
        return factory.createSetAssignment();
    }

    @Override
    protected boolean isInstance(AbstractAssignment assignment) {
        return assignment instanceof SetAssignment;
    }

    @Override
    protected SetAssignment cast(AbstractAssignment assignment) {
        return (SetAssignment) assignment;
    }

    @Override
    protected void addOutputLabel(SetAssignment assignment, Label label) {
        assignment.getOutputLabels()
                .add(label);
    }

    @Override
    protected boolean outputLabelEquals(SetAssignment assignment, Label label) {
        return assignment.getOutputLabels()
                .equals(List.of(label));
    }

    @Override
    protected String assignmentName() {
        return "Set Assignment";
    }

    @Override
    public String toString() {
        return "Set at Pin " + pin.getId() + " with Label " + label.getEntityName();
    }
}
