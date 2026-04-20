from pathlib import Path
import json
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

APPROACHES = {
    "SAT": Path("../sat_performance_measurements.json"),
    "ILP": Path("../ilp_performance_measurements.json"),
    "SMT": Path("../smt_performance_measurements.json"),
}
OUT_DIR = Path("")
OUT_DIR.mkdir(parents=True, exist_ok=True)

EXPERIMENTS = {
    "tfg_length":             {"x_col": "tfgLengthScaling", "x_label": "Length of TFG",                          "prefix": "LengthTfg",             "rotation": 0},
    "tfg_amount":             {"x_col": "tfgAmountScaling", "x_label": "Amount of TFGs",                         "prefix": "AmountTfg",             "rotation": 45},
    "constraints_amount":     {"x_col": "amountConstraint", "x_label": "Amount of Constraints",                  "prefix": "AmountConstraints",     "rotation": 0},
    "constraints_complexity": {"x_col": "numberWithLabel",  "x_label": "Constraint Complexity (all dimensions)", "prefix": "ComplexityConstraints", "rotation": 0},
}

MARKERS = {"SAT": "o", "ILP": "s", "SMT": "^"}


def load_means(json_path):
    with open(json_path) as f:
        df = pd.DataFrame(json.load(f))
    if "status" in df.columns:
        df = df[df["status"] == "ok"].copy()
    if "runType" in df.columns:
        df = df[df["runType"] == "measurement"].copy()

    # --- Validate 3 repeats (0, 1, 2) per configuration ---
    EXPECTED = {0, 1, 2}
    problems = []
    for key, group in df.groupby("key"):
        got = set(group["repeatIndex"].tolist())
        if got != EXPECTED:
            missing = sorted(EXPECTED - got)
            extra   = sorted(got - EXPECTED)
            detail  = []
            if missing: detail.append(f"missing {missing}")
            if extra:   detail.append(f"extra {extra}")
            problems.append(f"  {key}: {', '.join(detail)} (got {sorted(got)})")
    if problems:
        raise ValueError(
            f"{json_path.name}: {len(problems)} config(s) do not have exactly "
            f"repeatIndex 0, 1, 2:\n" + "\n".join(problems)
        )

    out = {}
    for exp, cfg in EXPERIMENTS.items():
        sub = df[df["experiment"] == exp]
        if sub.empty:
            continue
        out[exp] = (sub.groupby(cfg["x_col"], as_index=False)["executionTime"]
                       .mean().sort_values(cfg["x_col"]))
    return out


def _save(fig, name):
    fig.tight_layout()
    fig.savefig(OUT_DIR / f"{name}.pdf", format="pdf")
    plt.close(fig)


def plot_comparison(exp, cfg, data_by_approach):
    """data_by_approach: {approach: DataFrame[x_col, executionTime]}"""
    # union of all x values across approaches, sorted
    x_col = cfg["x_col"]
    all_x = sorted({v for df in data_by_approach.values() for v in df[x_col].tolist()})
    x_idx = np.arange(len(all_x))
    xlabels = [str(int(v)) if float(v).is_integer() else str(v) for v in all_x]

    def series_for(df):
        lookup = dict(zip(df[x_col].tolist(), df["executionTime"].tolist()))
        return np.array([lookup.get(v, np.nan) for v in all_x], dtype=float)

    # linear
    fig, ax = plt.subplots()
    for approach, df in data_by_approach.items():
        ax.plot(x_idx, series_for(df), marker=MARKERS.get(approach, "o"), label=approach)
    ax.set_xticks(x_idx); ax.set_xticklabels(xlabels, rotation=cfg["rotation"])
    ax.set_xlabel(cfg["x_label"]); ax.set_ylabel("Mean total execution time in ms")
    ax.grid(True); ax.legend(loc="upper left")
    _save(fig, f"{cfg['prefix']}_linear")

    # log
    fig, ax = plt.subplots()
    for approach, df in data_by_approach.items():
        ax.plot(x_idx, series_for(df), marker=MARKERS.get(approach, "o"), label=approach)
    ax.set_yscale("symlog")
    ax.set_xticks(x_idx); ax.set_xticklabels(xlabels, rotation=cfg["rotation"] or 45)
    ax.set_xlabel(cfg["x_label"]); ax.set_ylabel("Mean total execution time in ms (log scale)")
    ax.grid(True, which="both"); ax.legend(loc="upper left")
    _save(fig, f"{cfg['prefix']}_log")


# Load all approaches
loaded = {}
for approach, path in APPROACHES.items():
    if not path.exists():
        print(f"[skip] {approach}: {path} not found")
        continue
    loaded[approach] = load_means(path)

# One plot pair per experiment, with all available approaches overlaid
for exp, cfg in EXPERIMENTS.items():
    data_by_approach = {a: d[exp] for a, d in loaded.items() if exp in d}
    if not data_by_approach:
        print(f"[skip] {exp}: no approach has data")
        continue
    plot_comparison(exp, cfg, data_by_approach)
    print(f"[ok] {cfg['prefix']}_linear.pdf, {cfg['prefix']}_log.pdf  ({', '.join(data_by_approach)})")

print(f"\nDone. PDFs in {OUT_DIR.resolve()}/")