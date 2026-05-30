import json
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.colors import TwoSlopeNorm

FILES = {
    "SAT": "../sat_efficiency_results.json",
    "SMT": "../smt_efficiency_results.json",
    "ILP": "../ilp_efficiency_results.json",
}

F_MODEL    = "model"
F_VARIANT  = "variant"
F_APPROACH = "approachCost"

Y_ORDER = [
    "anilallewar", "apssouza22", "callistaenterprise", "ewolff-kafka",
    "georgwittberger", "jferrater", "koushikkothagal", "mudigal-technologies",
    "spring-petclinic", "sqshq", "yidongnan",
]


def load_delta_matrix(path):
    with open(path) as f:
        data = json.load(f)

    rows = [{
        "project": d[F_MODEL],
        "variant": int(d[F_VARIANT]),
        "delta": float(d[F_APPROACH]),
    } for d in data]

    df = pd.DataFrame(rows)
    mat = df.pivot(index="project", columns="variant", values="delta")
    return mat.reindex(sorted(mat.columns), axis=1)


def prepare_matrices(files):
    matrices = {name: load_delta_matrix(path) for name, path in files.items()}

    all_projects = set().union(*(m.index for m in matrices.values()))
    all_variants = sorted(set().union(*(m.columns for m in matrices.values())))

    row_order = (
        [p for p in Y_ORDER if p in all_projects]
        + sorted(p for p in all_projects if p not in Y_ORDER)
    )

    for name in matrices:
        matrices[name] = matrices[name].reindex(
            index=row_order,
            columns=all_variants,
        )

    return matrices


def plot_combined_heatmaps(matrices, out_path="combined_efficiency.pdf"):
    vmax = max(
        float(np.nanmax(np.abs(m.to_numpy()))) if m.notna().any().any() else 1.0
        for m in matrices.values()
    )

    cmap = plt.cm.RdYlGn_r.copy()
    cmap.set_bad(color="white")
    norm = TwoSlopeNorm(vmin=-vmax, vcenter=0.0, vmax=vmax)

    fig, axes = plt.subplots(
        1, 3,
        figsize=(13.8, 5.2),
        sharey=True,
        constrained_layout=True,
    )

    im = None

    for ax, (name, mat) in zip(axes, matrices.items()):
        Z = mat.to_numpy()
        mask = np.isnan(Z)

        im = ax.imshow(
            np.ma.array(Z, mask=mask),
            cmap=cmap,
            norm=norm,
            aspect="auto",
        )

        ax.set_title(name, fontweight="bold")

        ax.set_xticks(range(mat.shape[1]))
        ax.set_xticklabels(mat.columns.astype(int), rotation=30, ha="right")

        ax.set_xticks(np.arange(-0.5, mat.shape[1], 1), minor=True)
        ax.set_yticks(np.arange(-0.5, mat.shape[0], 1), minor=True)
        ax.grid(which="minor", color="lightgray", linewidth=0.8)
        ax.tick_params(which="minor", bottom=False, left=False)

        ax.set_xlabel("Variant")

        for spine in ax.spines.values():
            spine.set_visible(False)

        for i in range(mat.shape[0]):
            for j in range(mat.shape[1]):
                v = Z[i, j]
                if np.isnan(v):
                    continue
                color = "white" if abs(v) > 0.45 * vmax else "black"
                ax.text(
                    j, i, f"{int(v):d}",
                    ha="center",
                    va="center",
                    color=color,
                    fontsize=8,
                )

    axes[0].set_yticks(range(next(iter(matrices.values())).shape[0]))
    axes[0].set_yticklabels(next(iter(matrices.values())).index)

    for ax in axes[1:]:
        ax.tick_params(
            axis="y",
            which="both",
            left=False,
            labelleft=False
        )

    fig.savefig(out_path, format="pdf", bbox_inches="tight")
    plt.close(fig)


matrices = prepare_matrices(FILES)
plot_combined_heatmaps(matrices, out_path="combined_efficiency.pdf")