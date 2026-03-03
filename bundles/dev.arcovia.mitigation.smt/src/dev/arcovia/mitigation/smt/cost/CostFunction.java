package dev.arcovia.mitigation.smt.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntExpr;

/**
 * Creates a Z3 IntExpr that represents a cost function from multiple smaller partial functions
 */
public final class CostFunction {

    private final Context ctx;
    // Partial cost terms
    private final List<IntExpr> terms = new ArrayList<>();

    private CostFunction(Context ctx) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
    }

    /**
     * Factory method for creating a cost function
     * @param ctx
     * @return Empty cost function
     */
    public static CostFunction create(Context ctx) {
        return new CostFunction(ctx);
    }

    /**
     * Adds a term to the cost function that adds cost if provided expressions differ
     * @param cur A boolean expression (usually a modifiable variable)
     * @param ref A reference (usually a constant value)
     * @param weight for this term
     * @return Same obejct with an added term
     */
    public CostFunction add(BoolExpr cur, BoolExpr ref, int weight) {
        // If weight is 0, the term is irrelevant
        if (weight == 0) {
            return this;
        }

        // The term evaluates to 1 if the two values differ, else 0.
        IntExpr base = (IntExpr) ctx.mkITE(ctx.mkXor(cur, ref), ctx.mkInt(1), ctx.mkInt(0));

        // If a relevant weight has been provided, multiply by it here.
        IntExpr weighted = (weight == 1) ? base : (IntExpr) ctx.mkMul(ctx.mkInt(weight), base);

        terms.add(weighted);
        return this;
    }

    /**
     * Allows for manual addition of Z3 Expressions
     * @param term Term that should be added
     * @return The object that now contains an additional term
     */
    public CostFunction addTerm(IntExpr term) {
        terms.add(term);
        return this;
    }

    /**
     * Returns an IntExpr that is the sum of all its partial terms
     * @return Total cost function
     */
    public IntExpr build() {
        if (terms.isEmpty()) {
            return ctx.mkInt(0);
        }
        if (terms.size() == 1) {
            return terms.get(0);
        }
        return (IntExpr) ctx.mkAdd(terms.toArray(IntExpr[]::new));
    }
}
