package dev.arcovia.mitigation.smt.tests.cost;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntExpr;

import dev.arcovia.mitigation.smt.cost.CostFunction;

class CostFunctionTest {

    @Test
    void emptyBuildIsZero() {
        try (Context ctx = new Context()) {
            IntExpr actual = CostFunction.create(ctx)
                    .build();
            IntExpr expected = ctx.mkInt(0);

            assertTrue(actual.simplify()
                    .equals(expected));
        }
    }

    @Test
    void addWithWeight1BuildsIteXor() {
        try (Context ctx = new Context()) {
            BoolExpr cur = ctx.mkBoolConst("cur");
            BoolExpr ref = ctx.mkBoolConst("ref");

            IntExpr actual = (IntExpr) CostFunction.create(ctx)
                    .add(cur, ref, 1)
                    .build()
                    .simplify();

            IntExpr expected = (IntExpr) ctx.mkITE(ctx.mkXor(cur, ref), ctx.mkInt(1), ctx.mkInt(0))
                    .simplify();

            assertTrue(actual.equals(expected));
        }
    }

    @Test
    void addWithWeight5BuildsMulIteXor() {
        try (Context ctx = new Context()) {
            BoolExpr cur = ctx.mkBoolConst("cur");
            BoolExpr ref = ctx.mkBoolConst("ref");

            IntExpr actual = (IntExpr) CostFunction.create(ctx)
                    .add(cur, ref, 5)
                    .build()
                    .simplify();

            IntExpr expected = (IntExpr) ctx.mkMul(ctx.mkInt(5), ctx.mkITE(ctx.mkXor(cur, ref), ctx.mkInt(1), ctx.mkInt(0)))
                    .simplify();

            assertTrue(actual.equals(expected));
        }
    }

    @Test
    void multipleTermsBecomeAdd() {
        try (Context ctx = new Context()) {
            BoolExpr a = ctx.mkBoolConst("a");
            BoolExpr b = ctx.mkBoolConst("b");
            BoolExpr c = ctx.mkBoolConst("c");

            IntExpr actual = (IntExpr) CostFunction.create(ctx)
                    .add(a, b, 1)
                    .add(b, c, 2)
                    .build()
                    .simplify();

            IntExpr t1 = (IntExpr) ctx.mkITE(ctx.mkXor(a, b), ctx.mkInt(1), ctx.mkInt(0));
            IntExpr t2 = (IntExpr) ctx.mkMul(ctx.mkInt(2), ctx.mkITE(ctx.mkXor(b, c), ctx.mkInt(1), ctx.mkInt(0)));

            IntExpr expected = (IntExpr) ctx.mkAdd(t1, t2)
                    .simplify();

            assertTrue(actual.equals(expected));
        }
    }

    @Test
    void weightZeroIsIgnored() {
        try (Context ctx = new Context()) {
            BoolExpr a = ctx.mkBoolConst("a");
            BoolExpr b = ctx.mkBoolConst("b");

            IntExpr actual = (IntExpr) CostFunction.create(ctx)
                    .add(a, b, 0)
                    .build()
                    .simplify();

            assertTrue(actual.equals(ctx.mkInt(0)));
        }
    }
}
