package dev.arcovia.mitigation.smt.operations;

import java.util.List;
import java.util.Optional;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Behavior;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.Pin;

/**
 * Modifies a Pin Assignment for a given Pin and Label
 * @param <T> The type of assignment that should be used for the modification
 */
public sealed abstract class AbstractPinAssignmentOperation<T extends AbstractAssignment> extends DataDictionaryOperation
        permits SetAssignmentOperation, UnsetAssignmentOperation {

    protected final Pin pin;
    protected final Label label;

    protected AbstractPinAssignmentOperation(Pin pin, Label label) {
        this.pin = pin;
        this.label = label;
    }

    /**
     * Creates an assignment of the specified type
     * @return Newly created assignments
     */
    protected abstract T createAssignment();

    /**
     * Checks whether the input is of Type T
     * @param assignment
     * @return Indicating boolean
     */
    protected abstract boolean isInstance(AbstractAssignment a);

    /**
     * Casts this assignment to type T
     * @param assignment
     * @return
     */
    protected abstract T cast(AbstractAssignment a);

    /**
     * Adds the output label to the Assignment
     * @param assignment
     * @param label
     */
    protected abstract void addOutputLabel(T assignment, Label label);

    /**
     * Checks if the specified assignment has exactly the specified output label
     * @param assignment
     * @param label
     * @return Whether it has exactly the specified label
     */
    protected abstract boolean outputLabelEquals(T assignment, Label label);

    /**
     * Provides a String representation of this Assignment
     * @return String
     */
    protected abstract String assignmentName();

    @Override
    public DataFlowDiagramAndDictionary doOperation(DataFlowDiagramAndDictionary dfd) {
        T assignment = createAssignment();
        assignment.setOutputPin(pin);
        assignment.setId(String.valueOf(random.nextInt()));
        addOutputLabel(assignment, label);

        // Finds assignments for this operation's pin
        Optional<List<AbstractAssignment>> assignments = dfd.dataDictionary()
                .getBehavior()
                .stream()
                .filter(b -> b.getOutPin()
                        .contains(pin))
                .map(Behavior::getAssignment)
                .findAny();

        if (assignments.isEmpty()) {
            logger.debug("Couldnt't find Node behavior for pin " + pin.getId());
        } else {
            assignments.get()
                    .add(assignment);
        }
        return dfd;
    }

    @Override
    public DataFlowDiagramAndDictionary undoOperation(DataFlowDiagramAndDictionary dfd) {
        Optional<Behavior> behavior = dfd.dataDictionary()
                .getBehavior()
                .stream()
                .filter(b -> b.getOutPin()
                        .contains(pin))
                .findFirst();

        if (behavior.isEmpty()) {
            logger.debug("Couldn't find matching behavior for " + pin);
            return dfd;
        }

        // Find the assignment that was added earlier. This could remove the wrong
        // assignment if an identical one exists.
        Optional<T> found = behavior.get()
                .getAssignment()
                .stream()
                .filter(a -> a.getOutputPin()
                        .equals(pin))
                .filter(this::isInstance)
                .map(this::cast)
                .filter(a -> outputLabelEquals(a, label))
                .findAny();

        if (found.isEmpty()) {
            logger.debug("Couldn't find matching " + assignmentName() + " for pin " + pin + " with Labels " + label);
        } else {
            behavior.get()
                    .getAssignment()
                    .removeIf(a -> a.equals(found.get()));
        }

        return dfd; // your original returned null; returning dfd is usually intended
    }
}
