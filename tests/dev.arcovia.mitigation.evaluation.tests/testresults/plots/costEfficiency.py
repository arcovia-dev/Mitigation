import json
from pathlib import Path
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.colors import TwoSlopeNorm

FILES = {
    "SAT": "../sat_efficiency_results.json",
    "ILP": "../ilp_efficiency_results.json",
    "SMT": "../smt_efficiency_results.json",
}

F_MODEL    = "model"
F_VARIANT  = "variant"
F_APPROACH = "approachCost"
F_BASELINE = "tuhhCost"

Y_ORDER = [
    "anilallewar", "apssouza22", "callistaenterprise", "ewolff-kafka",
    "georgwittberger", "jferrater", "koushikkothagal", "mudigal-technologies",
    "spring-petclinic", "sqshq", "yidongnan",
]


def load_delta_matrix(path):
    """Read one JSON file -> pivot table of (model x variant) = approach - baseline."""
    with open(path) as f:
        data = json.load(f)
    rows = [{
        "project":    d[F_MODEL],
        "constraint": int(d[F_VARIANT]),
        "delta":      float(d[F_APPROACH]) - float(d[F_BASELINE]),
    } for d in data]
    df = pd.DataFrame(rows)
    mat = df.pivot(index="project", columns="constraint", values="delta")
    return mat.reindex(sorted(mat.columns), axis=1)


def save_heatmap_pdf(mat, title, vmax, out_path):
    Z = mat.to_numpy()
    mask = np.isnan(Z)

    cmap = plt.cm.RdYlGn_r.copy()
    cmap.set_bad(color="white")
    norm = TwoSlopeNorm(vmin=-vmax, vcenter=0.0, vmax=vmax)

    fig, ax = plt.subplots(figsize=(6.5, 6.8))
    ax.imshow(np.ma.array(Z, mask=mask), cmap=cmap, norm=norm, aspect="auto")

    ax.set_xticks(range(mat.shape[1]))
    ax.set_xticklabels(mat.columns.astype(int), rotation=30, ha="right")
    ax.set_yticks(range(mat.shape[0]))
    ax.set_yticklabels(mat.index, fontweight="bold")
    ax.set_xlabel("Variant")
    ax.set_title(title)

    ax.set_xticks(np.arange(-.5, mat.shape[1], 1), minor=True)
    ax.set_yticks(np.arange(-.5, mat.shape[0], 1), minor=True)
    ax.grid(which="minor", color="lightgray", linewidth=1)
    ax.tick_params(which="minor", bottom=False, left=False)
    for s in ax.spines.values():
        s.set_visible(False)

    for i in range(mat.shape[0]):
        for j in range(mat.shape[1]):
            v = Z[i, j]
            if np.isnan(v):
                continue
            color = "white" if abs(v) > 0.45 * vmax else "black"
            ax.text(j, i, f"{int(v):d}", ha="center", va="center",
                    color=color, fontsize=9)

    fig.tight_layout()
    fig.savefig(out_path, format="pdf", bbox_inches="tight")
    plt.close(fig)


# --- Load the three files ---
matrices = {name: load_delta_matrix(p) for name, p in FILES.items()}

# --- Unify rows and columns across all three so the plots line up ---
all_projects = set().union(*(m.index for m in matrices.values()))
all_variants = sorted(set().union(*(m.columns for m in matrices.values())))
row_order = [p for p in Y_ORDER if p in all_projects] + \
            sorted(p for p in all_projects if p not in Y_ORDER)

for name in matrices:
    matrices[name] = matrices[name].reindex(index=row_order, columns=all_variants)

# --- Shared diverging scale around 0 (so PDFs are visually comparable) ---
vmax = max(
    float(np.nanmax(np.abs(m.to_numpy()))) if m.notna().any().any() else 1.0
    for m in matrices.values()
)

# --- Save one PDF per approach ---
for name, mat in matrices.items():
    save_heatmap_pdf(mat, title=name.upper(), vmax=vmax, out_path=f"{name}_efficiency.pdf")
    print(f"wrote {name}.pdf")