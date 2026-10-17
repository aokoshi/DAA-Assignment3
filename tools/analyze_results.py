"""Generate the three required plots and normalized counters from the real CSV."""
import csv
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ALGORITHMS = ["Kruskal", "Prim", "Kosaraju", "Kahn", "DagShortestPath"]
COLORS = ["#2563a6", "#da7028", "#239176", "#8c59ad", "#c5445c"]


def load_rows():
    with (ROOT / "results/results.csv").open(encoding="utf-8") as stream:
        rows = list(csv.DictReader(stream))
    if len(rows) != 50:
        raise ValueError("The official CSV must contain 50 data rows")
    for row in rows:
        for key in ["n", "m", "dsu_ops", "edge_scans", "comparisons", "max_depth", "result"]:
            row[key] = int(row[key])
        row["time_ms"] = float(row["time_ms"])
    return rows


def select(rows, algorithm, density):
    return sorted([r for r in rows if r["algorithm"] == algorithm and r["graph"].endswith(density)],
                  key=lambda r: r["n"])


def slope(rows, counter):
    x = [math.log(r["n"]) for r in rows]
    y = [math.log(r[counter]) for r in rows]
    x_mean, y_mean = sum(x) / len(x), sum(y) / len(y)
    return sum((a - x_mean) * (b - y_mean) for a, b in zip(x, y)) / sum((a - x_mean) ** 2 for a in x)


def normalized(row):
    if row["algorithm"] in ["Kruskal", "Prim"]:
        return row["comparisons"] / (row["m"] * math.log2(row["m"]))
    return row["edge_scans"] / (row["n"] + row["m"])


def make_plot(rows, filename, title, ylabel, value, log_y=False, kruskal_only=False):
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    fig, ax = plt.subplots(figsize=(10.5, 4.65), layout="constrained")
    for algorithm, color in zip(ALGORITHMS, COLORS):
        if kruskal_only and algorithm != "Kruskal":
            continue
        for density, style, marker in [("sparse", "-", "o"), ("dense", "--", "s")]:
            points = select(rows, algorithm, density)
            ax.plot([r["n"] for r in points], [value(r) for r in points], color=color,
                    linestyle=style, marker=marker, markersize=5, linewidth=1.6,
                    label=f"{algorithm} / {density}", alpha=0.85)
    ax.set_xscale("log")
    if log_y:
        ax.set_yscale("log")
    ax.set(title=title, xlabel="Vertices n (log scale)", ylabel=ylabel)
    ax.grid(True, which="both", color="#d9dfe7", linewidth=0.6, alpha=0.7)
    ax.spines[["top", "right"]].set_visible(False)
    ax.legend(fontsize=8, ncol=1 if kruskal_only else 2, loc="best")
    if kruskal_only:
        ax.set_ylim(0, 1.1)
    fig.savefig(ROOT / "results/plots" / filename, dpi=180, facecolor="white")
    plt.close(fig)


def main():
    rows = load_rows()
    (ROOT / "results/plots").mkdir(parents=True, exist_ok=True)
    make_plot(rows, "operations_vs_n.png", "Measured operation counters vs graph size",
              "dsu_ops + edge_scans + comparisons (log scale)",
              lambda r: r["dsu_ops"] + r["edge_scans"] + r["comparisons"], log_y=True)
    make_plot(rows, "scans_per_edge.png", "Edge scans per input edge",
              "edge_scans / m", lambda r: r["edge_scans"] / r["m"])
    make_plot(rows, "kruskal_edge_share.png", "Kruskal: fraction examined after sorting",
              "edge_scans / m", lambda r: r["edge_scans"] / r["m"], kruskal_only=True)
    analysis = []
    for algorithm in ALGORITHMS:
        for density in ["sparse", "dense"]:
            points = select(rows, algorithm, density)
            ratios = [normalized(r) for r in points]
            analysis.append({"algorithm": algorithm, "density": density,
                             "scan_slope": slope(points, "edge_scans"),
                             "comparison_slope": slope(points, "comparisons") if algorithm in ["Kruskal", "Prim"] else None,
                             "ratio_min": min(ratios), "ratio_max": max(ratios)})
    (ROOT / "results/analysis.json").write_text(json.dumps(analysis, indent=2) + "\n", encoding="utf-8")
    with (ROOT / "results/normalized.csv").open("w", newline="", encoding="utf-8") as stream:
        writer = csv.writer(stream)
        writer.writerow(["algorithm", "graph", "n", "comparisons_per_m_log2_m", "scans_per_n_plus_m",
                         "dsu_ops_per_m", "dsu_ops_per_scanned_edge"])
        for r in rows:
            writer.writerow([r["algorithm"], r["graph"], r["n"],
                             f'{r["comparisons"] / (r["m"] * math.log2(r["m"])):.6f}',
                             f'{r["edge_scans"] / (r["n"] + r["m"]):.6f}',
                             f'{r["dsu_ops"] / r["m"]:.6f}',
                             f'{r["dsu_ops"] / r["edge_scans"]:.6f}'])
    print("Created three plots, analysis.json and normalized.csv from 50 measured rows.")


if __name__ == "__main__":
    main()
