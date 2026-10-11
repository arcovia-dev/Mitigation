from pathlib import Path
import json

import numpy as np
import pandas as pd
import statsmodels.formula.api as smf


APPROACHES = {
    "SAT": "../sat_performance_measurements.json",
    "SMT": "../smt_performance_measurements.json",
    "ILP": "../ilp_performance_measurements.json",
}

EXPERIMENTS = {
    "tfg_amount": "tfgAmountScaling",
    "tfg_length": "tfgLengthScaling",
    "constraints_amount": "amountConstraint",
    "constraints_complexity": "numberDummyLabels",
}


def load_data():
    dfs = []

    for approach, path in APPROACHES.items():
        with open(path) as f:
            df = pd.DataFrame(json.load(f))

        df = df[df["status"] == "ok"]
        df = df[df["runType"] == "measurement"]

        df["approach"] = approach
        dfs.append(df)

    return pd.concat(dfs, ignore_index=True)


def format_p(p):
    if p < 0.00001:
        return "p < 0.00001"
    return f"p = {p:.5f}"


def slope_expression(method, param_names):
    """
    Returns a contrast vector that extracts the log-log slope
    for the given method.
    """
    v = np.zeros(len(param_names))

    # Every method includes the base log_input slope
    v[param_names.index("log_input")] = 1

    # ILP/SMT have additional interaction terms relative to SAT
    if method != "SAT":
        interaction = (
            f"C(approach, Treatment(reference='SAT'))"
            f"[T.{method}]:log_input"
        )

        if interaction in param_names:
            v[param_names.index(interaction)] = 1

    return v


df = load_data()


for experiment, x_col in EXPERIMENTS.items():

    sub = df[df["experiment"] == experiment].copy()

    sub = sub.rename(columns={
        x_col: "input_size",
        "executionTime": "runtime"
    })

    sub = sub[(sub["input_size"] > 0) & (sub["runtime"] > 0)].copy()

    sub["approach"] = pd.Categorical(
        sub["approach"],
        categories=["SAT", "SMT", "ILP"]
    )

    sub["log_input"] = np.log(sub["input_size"])
    sub["log_runtime"] = np.log(sub["runtime"])

    model = smf.ols(
        "log_runtime ~ C(approach, Treatment(reference='SAT')) * log_input",
        data=sub
    ).fit(cov_type="HC3")

    param_names = list(model.params.index)

    slopes = {}
    for method in ["SAT", "SMT", "ILP"]:
        v = slope_expression(method, param_names)
        slopes[method] = float(v @ model.params)

    print("\n" + "=" * 80)
    print(experiment)
    print("=" * 80)

    print("\nScaling exponents:")
    for method, slope in slopes.items():
        print(f"{method}: {slope:.2f}")

    print("\nPairwise scaling comparisons:")
    comparisons = [
        ("SAT", "SMT"),
        ("SAT", "ILP"),
        ("SMT", "ILP"),
    ]

    for a, b in comparisons:
        va = slope_expression(a, param_names)
        vb = slope_expression(b, param_names)

        contrast = va - vb
        test = model.t_test(contrast)

        diff = slopes[a] - slopes[b]
        p = float(test.pvalue)

        if diff < 0:
            direction = "scales better than"
        elif diff > 0:
            direction = "scales worse than"
        else:
            direction = "scales the same as"

        print(
            f"{a} vs {b}: "
            f"Delta exponent = {diff:.2f}, "
            f"{a} {direction} {b}, "
            f"{format_p(p)}"
        )

    print(f"\nR_Square = {model.rsquared:.3f}")