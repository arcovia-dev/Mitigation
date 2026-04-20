import re
import json
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.colors import ListedColormap

FILES = {
    "sat": "../sat_violation_results.json",
    "ilp": "../ilp_violation_results.json",
    "smt": "../smt_violation_results.json",
}

F_MODEL  = "modelName"
F_BEFORE = "violationsBefore"
F_AFTER  = "violationsAfter"

KEY_PAT = re.compile(r"^(.*)_(\d+)$")


def load_tables(path):
    """Return (before_pivot, after_pivot) keyed by project x variant."""
    with open(path) as f:
        data = json.load(f)

    rows = []
    for entry in data:
        m = KEY_PAT.match(entry[F_MODEL])
        if not m:
            raise ValueError(f"Bad key format: {entry[F_MODEL]}")
        project, variant = m.group(1), int(m.group(2))
        rows.append((project, variant,
                     int(entry[F_BEFORE]), int(entry[F_AFTER])))

    df = pd.DataFrame(rows, columns=["project", "variant", "before", "after"])
    before = df.pivot(index="project", columns="variant", values="before").sort_index()
    after  = df.pivot(index="project", columns="variant", values="after").sort_index()
    before = before.reindex(sorted(before.columns), axis=1)
    after  = after.reindex(sorted(after.columns),  axis=1)
    return before, after


def save_pdf(before, after, out_path, title=None):
    mask_missing = before.isna().to_numpy()
    bg = np.where(mask_missing, np.nan, 1.0)

    fig_w = max(8, 0.6 * before.shape[1] + 4)
    fig_h = max(6, 0.45 * before.shape[0] + 2)
    fig, ax = plt.subplots(figsize=(fig_w, fig_h))

    cmap = ListedColormap(["#d9d9d9"])
    cmap.set_bad(color="white")
    ax.imshow(bg, cmap=cmap, vmin=0, vmax=1, aspect="auto")

    ax.set_xticks(np.arange(before.shape[1]))
    ax.set_xticklabels(before.columns.astype(str))
    ax.set_yticks(np.arange(before.shape[0]))
    ax.set_yticklabels(before.index)
    ax.set_xlabel("Constriant")
    if title:
        ax.set_title(title)

    ax.set_xticks(np.arange(-0.5, before.shape[1], 1), minor=True)
    ax.set_yticks(np.arange(-0.5, before.shape[0], 1), minor=True)
    ax.grid(which="minor", color="#bfbfbf", linewidth=1)
    ax.tick_params(which="minor", bottom=False, left=False)

    b = before.to_numpy()
    a = after.to_numpy()
    for i in range(b.shape[0]):
        for j in range(b.shape[1]):
            if not np.isnan(b[i, j]):
                ax.text(j, i, f"{int(b[i, j])} \u2192 {int(a[i, j])}",
                        ha="center", va="center", fontsize=8)

    fig.tight_layout()
    fig.savefig(out_path, format="pdf", bbox_inches="tight")
    plt.close(fig)


for name, path in FILES.items():
    before, after = load_tables(path)
    out = f"{name}_effectiveness.pdf"
    save_pdf(before, after, out, title=name.upper())
    print(f"wrote {out}")