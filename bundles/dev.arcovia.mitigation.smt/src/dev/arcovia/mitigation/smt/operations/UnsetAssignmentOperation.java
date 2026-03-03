package dev.arcovia.mitigation.smt.operations;

import java.util.List;

import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.UnsetAssignment;

/**
 * This operation creates a Unset Assignment for a specific label and pin
 */
public final class UnsetAssignmentOperation extends AbstractPinAssignmentOperation<UnsetAssignment> {

    public UnsetAssignmentOperation(Pin pin, Label label) {
        super(pin, label);
    }

    @Override
    protected UnsetAssignment createAssignment() {
        return factory.createUnsetAssignment();
    }

    @Override
    protected boolean isInstance(AbstractAssignment a) {
        return a instanceof UnsetAssignment;
    }

    @Override
    protected UnsetAssignment cast(AbstractAssignment a) {
        return (UnsetAssignment) a;
    }

    @Override
    protected void addOutputLabel(UnsetAssignment assignment, Label label) {
        assignment.getOutputLabels()
                .add(label);
    }

    @Override
    protected boolean outputLabelEquals(UnsetAssignment assignment, Label label) {
        return assignment.getOutputLabels()
                .equals(List.of(label));
    }

    @Override
    protected String assignmentName() {
        return "Unset Assignment";
    }

    @Override
    public String toString() {
        return "Unset at Pin " + pin.getId() + " with Label " + label.getEntityName();
    }
}
