package dev.arcovia.mitigation.smt.tests.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntExpr;

import dev.arcovia.mitigation.smt.cost.CostFunction;
import dev.arcovia.mitigation.smt.utils.Z3NativeLoader;

class CostFunctionTest {

    @BeforeAll
    static void loadZ3Natives() {
        Z3NativeLoader.ensureLoaded();
    }

    @Test
    void emptyBuildIsZero() {
        try (Context context = new Context()) {
            IntExpr actual = CostFunction.create(context)
                    .build();
            IntExpr expected = context.mkInt(0);

            assertEquals(actual.simplify(), expected);
        }
    }

    @Test
    void addWithWeight1BuildsIteXor() {
        try (Context context = new Context()) {
            BoolExpr current = context.mkBoolConst("current");
            BoolExpr reference = context.mkBoolConst("reference");

            IntExpr actual = (IntExpr) CostFunction.create(context)
                    .add(current, reference, 1)
                    .build()
                    .simplify();

            IntExpr expected = (IntExpr) context.mkITE(context.mkXor(current, reference), context.mkInt(1), context.mkInt(0))
                    .simplify();

            assertEquals(actual, expected);
        }
    }

    @Test
    void addWithWeight5BuildsMulIteXor() {
        try (Context context = new Context()) {
            BoolExpr current = context.mkBoolConst("current");
            BoolExpr reference = context.mkBoolConst("reference");

            IntExpr actual = (IntExpr) CostFunction.create(context)
                    .add(current, reference, 5)
                    .build()
                    .simplify();

            IntExpr expected = (IntExpr) context
                    .mkMul(context.mkInt(5), context.mkITE(context.mkXor(current, reference), context.mkInt(1), context.mkInt(0)))
                    .simplify();

            assertEquals(actual, expected);
        }
    }

    @Test
    void multipleTermsBecomeAdd() {
        try (Context context = new Context()) {
            BoolExpr a = context.mkBoolConst("a");
            BoolExpr b = context.mkBoolConst("b");
            BoolExpr c = context.mkBoolConst("c");

            IntExpr actual = (IntExpr) CostFunction.create(context)
                    .add(a, b, 1)
                    .add(b, c, 2)
                    .build()
                    .simplify();

            IntExpr term1 = (IntExpr) context.mkITE(context.mkXor(a, b), context.mkInt(1), context.mkInt(0));
            IntExpr term2 = (IntExpr) context.mkMul(context.mkInt(2), context.mkITE(context.mkXor(b, c), context.mkInt(1), context.mkInt(0)));

            IntExpr expected = (IntExpr) context.mkAdd(term1, term2)
                    .simplify();

            assertEquals(actual, expected);
        }
    }

    @Test
    void weightZeroIsIgnored() {
        try (Context context = new Context()) {
            BoolExpr a = context.mkBoolConst("a");
            BoolExpr b = context.mkBoolConst("b");

            IntExpr actual = (IntExpr) CostFunction.create(context)
                    .add(a, b, 0)
                    .build()
                    .simplify();

            assertEquals(actual, context.mkInt(0));
        }
    }
}
