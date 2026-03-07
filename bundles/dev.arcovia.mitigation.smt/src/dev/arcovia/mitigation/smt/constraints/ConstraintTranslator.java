package dev.arcovia.mitigation.smt.constraints;

import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.selectors.AbstractSelector;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import dev.arcovia.mitigation.smt.SMT;

/**
 * This class translates a DSL constraint into a Z3 BoolExpr
 */
public class ConstraintTranslator {
    // Translator for selectors
    private final DefaultSelectorTranslator selectorTranslator;
    // SMT object that requested translation
    private final SMT smt;

    /**
     * Creates a new constraint translator
     * @param smt used to access relevant encoding objects in the selector matching logic.
     */
    public ConstraintTranslator(SMT smt) {
        this.selectorTranslator = new DefaultSelectorTranslator(smt);
        this.smt = smt;
    }

    /**
     * Creates a Boolean expression, that encodes that the given Vertex DOES NOT match the given constraint. In general a
     * constraint matches, if all selectors match.
     * @param constr input constraint
     * @param vertex input vertex
     * @return Created expression
     */
    public BoolExpr translateConstraint(AnalysisConstraint constr, DFDVertex vertex) {
        // Differentiate selectors into their roles.
        List<AbstractSelector> dataSource = constr.getDataSourceSelectors()
                .getSelectors();
        List<AbstractSelector> vertexSource = constr.getVertexSourceSelectors()
                .getSelectors();
        List<AbstractSelector> vertexDestination = constr.getVertexDestinationSelectors()
                .getSelectors();

        Context ctx = smt.getContext();

        // Vertex Destination Selectors
        List<BoolExpr> allDestinationSelectors = new ArrayList<>();
        for (AbstractSelector dstSelector : vertexDestination) {
            allDestinationSelectors.add(selectorTranslator.toBool(dstSelector, vertex));
        }
        // This evaluates to true, if no destination selectors are present
        BoolExpr allDestinationSatisfied = ctx.mkAnd(allDestinationSelectors.toArray(new BoolExpr[0]));

        // Data Selectors
        List<BoolExpr> allDataSource = new ArrayList<>();
        for (AbstractSelector source : dataSource) {
            allDataSource.add(selectorTranslator.toBool(source, vertex));
        }
        // Evaluates to true, if no data selectors are present
        BoolExpr allDataSourceSatisfied = ctx.mkAnd(allDataSource.toArray(new BoolExpr[0]));

        // Vertex Source Selectors.
        List<BoolExpr> allVertexSource = new ArrayList<>();
        for (AbstractSelector source : vertexSource) {
            allVertexSource.add(selectorTranslator.toBool(source, vertex));
        }
        // Evaluates to true, if no vertex source selectors are present
        BoolExpr allVertexSourceSatisfied = ctx.mkAnd(allVertexSource.toArray(new BoolExpr[0]));

        // Constraint is satisfied if all selectors are satisfied.
        BoolExpr allSatisfied = ctx.mkAnd(allDestinationSatisfied, allDataSourceSatisfied, allVertexSourceSatisfied);
        // But all selectors should not be satisfied
        BoolExpr notAllSatisfied = ctx.mkNot(allSatisfied);

        return notAllSatisfied;
    }

}
