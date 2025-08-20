package dev.arcovia.mitigation.sat.cnf.tests.utility;

import dev.arcovia.mitigation.sat.*;

import java.util.ArrayList;
import java.util.List;

public abstract class CNFUtil {
    private static int literalValueCounter = 0;
    public static int getNewValue() {
        return literalValueCounter++;
    }

    public static Constraint generateClause(List<DInData> incomingData, List<DOutData> outgoingData, List<DNode> nodes) {
        ArrayList<Literal> constraint = new ArrayList<>();
        incomingData.forEach(it ->
                constraint.add(new Literal(!it.positive(), new IncomingDataLabel(new Label(LabelCategory.IncomingData.name(), it.value())))));
        outgoingData.forEach(it ->
                constraint.add(new Literal(!it.positive(), new OutgoingDataLabel(new Label(LabelCategory.OutgoingData.name(), it.value())))));
        nodes.forEach(it ->
                constraint.add(new Literal(!it.positive(), new NodeLabel(new Label(LabelCategory.Node.name(), it.value())))));
        return new Constraint(constraint);
    }

    public static Constraint generateNodeClause( List<DInData> incomingData, List<DOutData> outgoingData, List<DNode> nodes) {
        ArrayList<Literal> constraint = new ArrayList<>();
        nodes.forEach(it ->
                constraint.add(new Literal(!it.positive(), new NodeLabel(new Label(LabelCategory.Node.name(), it.value())))));
        incomingData.forEach(it ->
                constraint.add(new Literal(!it.positive(), new IncomingDataLabel(new Label(LabelCategory.IncomingData.name(), it.value())))));
        outgoingData.forEach(it ->
                constraint.add(new Literal(!it.positive(), new OutgoingDataLabel(new Label(LabelCategory.OutgoingData.name(), it.value())))));
        return new Constraint(constraint);
    }

    public static boolean compareCNF(DCNF firstCNF, DCNF secondCNF) {
        return firstCNF.equals(secondCNF);
    }
}
