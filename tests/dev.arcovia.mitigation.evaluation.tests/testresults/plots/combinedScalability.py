from pathlib import Path
import json
import pandas as pd
import matplotlib.pyplot as plt

APPROACHES = {
    "SAT": Path("../sat_performance_measurements.json"),
    "SMT": Path("../smt_performance_measurements.json"),
    "ILP": Path("../ilp_performance_measurements.json"),
}

OUT_DIR = Path("")
OUT_DIR.mkdir(parents=True, exist_ok=True)

EXPERIMENTS = {
    "tfg_length": {
        "x_col": "tfgLengthScaling",
        "x_label": "Length of TFG",
    },
    "tfg_amount": {
        "x_col": "tfgAmountScaling",
        "x_label": "Number of TFGs",
    },
    "constraints_amount": {
        "x_col": "amountConstraint",
        "x_label": "Number of Constraints",
    },
    "constraints_complexity": {
        "x_col": "numberDummyLabels",
        "x_label": "Labels per Constraint",
    },
}

MARKERS = {"SAT": "o", "SMT": "s", "ILP": "^"}


def load_means(json_path: Path):
    with open(json_path) as f:
        df = pd.DataFrame(json.load(f))

    if "status" in df.columns:
        df = df[df["status"] == "ok"].copy()
    if "runType" in df.columns:
        df = df[df["runType"] == "measurement"].copy()

    expected = {0, 1, 2}
    problems = []

    for key, group in df.groupby("key"):
        got = set(group["repeatIndex"].tolist())
        if got != expected:
            missing = sorted(expected - got)
            extra = sorted(got - expected)
            detail = []
            if missing:
                detail.append(f"missing {missing}")
            if extra:
                detail.append(f"extra {extra}")
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

        out[exp] = (
            sub.groupby(cfg["x_col"], as_index=False)["executionTime"]
            .mean()
            .sort_values(cfg["x_col"])
        )

    return out


def plot_scalability_panel(loaded, out_name="combined_scalability.pdf"):
    panels = [
        ("tfg_amount", "(a) Number of TFGs", "linear"),
        ("tfg_length", "(b) TFG Length", "linear"),
        ("constraints_amount", "(c) Number of Constraints", "log"),
        ("constraints_complexity", "(d) Constraint Complexity", "log"),
    ]

    fig, axes = plt.subplots(2, 2, figsize=(10, 7.5))
    axes = axes.flatten()

    for ax, (exp, panel_title, scale) in zip(axes, panels):
        cfg = EXPERIMENTS[exp]
        x_col = cfg["x_col"]

        data_by_approach = {
            approach: data[exp]
            for approach, data in loaded.items()
            if exp in data
        }

        if not data_by_approach:
            ax.set_visible(False)
            continue

        for approach, df in data_by_approach.items():
            ax.plot(
                df[x_col],
                df["executionTime"],
                marker=MARKERS.get(approach, "o"),
                label=approach,
                linewidth=1.5,
                markersize=4,
            )

        ax.set_xlabel(cfg["x_label"])
        ax.set_ylabel("Execution time [ms]")

        if scale == "log":
            ax.set_yscale("symlog")
            ax.set_ylabel("Execution time [ms] (log)")
            ax.grid(True, which="both")
        else:
            ax.grid(True)

        ax.legend(
            loc="upper left",
            fontsize=8,
            frameon=True
        )

    fig.tight_layout(rect=[0, 0, 1, 0.96])
    fig.savefig(OUT_DIR / out_name, format="pdf", bbox_inches="tight")
    plt.close(fig)


loaded = {}

for approach, path in APPROACHES.items():
    if not path.exists():
        print(f"[skip] {approach}: {path} not found")
        continue

    loaded[approach] = load_means(path)

if not loaded:
    raise RuntimeError("No input files found.")

plot_scalability_panel(loaded)