"""Build matching Markdown and a five-page PDF from measured benchmark data."""
import json
from html import escape
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak

from analyze_results import load_rows, select

ROOT = Path(__file__).resolve().parents[1]
BLUE = colors.HexColor("#174773")
styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="BodyReport", fontName="Helvetica", fontSize=9, leading=12,
                          spaceAfter=6, alignment=TA_LEFT))
styles.add(ParagraphStyle(name="CellReport", fontName="Helvetica", fontSize=8, leading=10))
styles.add(ParagraphStyle(name="HeadingReport", fontName="Helvetica-Bold", fontSize=13,
                          leading=16, spaceBefore=8, spaceAfter=7, textColor=BLUE))
styles.add(ParagraphStyle(name="TitleReport", fontName="Helvetica-Bold", fontSize=23,
                          leading=27, spaceAfter=10, textColor=BLUE))
story, markdown = [], []


def paragraph(text):
    story.append(Paragraph(escape(text), styles["BodyReport"]))
    markdown.extend([text, ""])


def heading(text, title=False):
    story.append(Paragraph(escape(text), styles["TitleReport" if title else "HeadingReport"]))
    markdown.extend([("# " if title else "## ") + text, ""])


def table(headers, rows, widths):
    cells = [[Paragraph(escape(str(c)), styles["CellReport"]) for c in row] for row in [headers, *rows]]
    result = Table(cells, colWidths=[w * mm for w in widths], hAlign="LEFT", repeatRows=1)
    result.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#e5edf6")),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f5f7fa")]),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LINEBELOW", (0, 0), (-1, 0), 0.7, BLUE),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
    ]))
    story.extend([result, Spacer(1, 5)])
    markdown.append("| " + " | ".join(headers) + " |")
    markdown.append("| " + " | ".join(["---"] * len(headers)) + " |")
    markdown.extend("| " + " | ".join(str(c).replace("|", "/") for c in row) + " |" for row in rows)
    markdown.append("")


def chart(filename, caption):
    story.append(Image(str(ROOT / "results/plots" / filename), width=174 * mm, height=77.05 * mm))
    markdown.extend([f"![{caption}](results/plots/{filename})", ""])
    paragraph(caption)


def page():
    story.append(PageBreak())


def footer(canvas, document):
    canvas.setStrokeColor(colors.HexColor("#d6dee7"))
    canvas.line(18 * mm, 17 * mm, 192 * mm, 17 * mm)
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#526171"))
    canvas.drawString(18 * mm, 12 * mm, "Yelnur Pashanov | SE2523 | DAA Assignment 3")
    canvas.drawRightString(192 * mm, 12 * mm, str(document.page))


def main():
    rows = load_rows()
    analysis = json.loads((ROOT / "results/analysis.json").read_text(encoding="utf-8"))
    heading("City Network & Job Scheduler", title=True)
    paragraph("Design and Analysis of Algorithms - Assignment 3. Yelnur Pashanov, SE2523. Release v1.0.")
    heading("1. Design and complexity")
    paragraph("One adjacency-list Graph stores int edge weights, directed/undirected mode, logical input edges and cached in-degrees. Algorithms use arrays, ArrayList, ArrayDeque and PriorityQueue; no graph library or design-pattern framework is used. Totals and distances use long to avoid int overflow. MST returns a forest on disconnected input. SCC uses explicit stacks, and Kahn rejects cycles.")
    table(["Operation", "Time", "Extra space", "Reason"], [
        ["DSU makeSet", "O(V)", "O(V)", "Initialize parent and size arrays."],
        ["DSU find", "O(alpha(V)) amortized", "O(1) per call", "Iterative full path compression; shared DSU storage is O(V)."],
        ["DSU union", "O(alpha(V)) amortized", "O(1) per call", "Two finds; attach the smaller tree below the larger root."],
        ["Kruskal", "O(V + E log(E+1) + E alpha(V))", "O(V + E)", "Copy and sort E edges, then test edges with DSU."],
        ["Prim (lazy heap)", "O(V + E log(E+1))", "O(V + E)", "Scan adjacency entries and insert/remove at most O(E) queue items."],
        ["Kosaraju", "O(V + E)", "O(V + E)", "Two iterative DFS passes plus a reversed graph."],
        ["Kahn", "O(V + E)", "O(V)", "Each vertex leaves the queue once; every DAG edge reduces one in-degree."],
        ["DAG shortest paths", "O(V + E)", "O(V)", "Kahn order, source initialization and one relaxation pass."],
    ], [34, 40, 30, 70])
    paragraph("Extra space excludes the input graph and includes returned arrays/lists. Lazy Prim can store parallel edges, so log E is the precise bound here; for a simple connected graph E <= V(V-1)/2 and this is the usual O(E log V). With both DSU heuristics, alpha is the inverse Ackermann function, which grows extremely slowly. This is an amortized sequence bound, not a worst-case constant-time promise for each find.")
    heading("Counter definitions and input control")
    paragraph("dsu_ops counts find invocations plus successful unions; it excludes parent-pointer steps. edge_scans counts Kruskal's post-sort candidates, Prim's adjacency entries, the two Kosaraju DFS passes, Kahn's queue-processing edges, or DAG relaxations. Graph loading, reversal construction and validation are outside the scan counters. DAG timing includes Kahn preprocessing; its own scan counter covers only relaxation. comparisons counts weight-comparator calls in sorting/heap operations or candidate-distance comparisons in DAG relaxation. max_depth is the maximum occupied explicit DFS stack; zero means no DFS stack was used.")
    paragraph("The official benchmark has fixed sizes and densities, rather than a student-selected input count. Interactive manual input accepts arbitrary n and m, generate writes a custom graph, and benchmark-custom accepts user-selected size/density lists. These extra modes do not replace the required 50-row run.")

    page()
    heading("2. Correctness and algorithm choice")
    heading("Cut property: Kruskal and Prim")
    paragraph("Consider a cut that respects the forest already chosen. Let e be a minimum-weight edge crossing this cut, and take an MST T containing the chosen forest. If T omits e, adding e creates a cycle containing another crossing edge f. Since weight(e) <= weight(f), replacing f with e preserves connectivity and cannot increase the total weight. Kruskal applies this exchange argument to the DSU components, while Prim applies it to visited versus unvisited vertices. Repeating safe additions yields an MST in each connected component, so both methods return minimum spanning forests even when the input is disconnected.")
    heading("Induction on the topological order")
    paragraph("Every edge of a DAG goes from an earlier vertex to a later vertex in a valid topological order. Initialize exactly the zero-in-degree vertices to distance zero, as if a virtual super-source had zero-weight edges to them. For the induction step, assume that all predecessors of the current vertex already have their optimal distances. Each predecessor has relaxed its outgoing edge, so every possible final edge of a path to the current vertex has already been considered. The current distance is therefore optimal and its outgoing edges can be relaxed once, recording a parent whenever a distance improves. Negative weights are safe because a DAG has no cycle and no path can return to an already processed vertex; in a general graph negative edges invalidate Dijkstra's greedy rule, and a reachable negative cycle can make a finite shortest distance undefined.")
    table(["Situation", "Choice and reason"], [
        ["Sparse undirected graph / edge list", "Kruskal: sort the available edge list and use DSU for cheap cycle rejection."],
        ["Adjacency-list network / grow from a district", "Prim: frontier edges are natural; compare heap work with sorting before choosing on dense input."],
        ["Very dense simple graph / matrix already given", "A matrix Prim variant can use O(V^2); this project implements the required PriorityQueue variant."],
        ["Directed graph may contain cycles / deadlocks", "Kosaraju: find mutually reachable groups; a multi-vertex SCC or a singleton self-loop signals a cycle."],
        ["Need only a job order or cycle detection", "Kahn: queue zero-in-degree jobs and reject an incomplete order."],
        ["DAG routing, possibly negative weights", "Topological shortest paths: O(V+E), including all source jobs. General negative graphs need a method such as Bellman-Ford."],
    ], [64, 110])
    heading("Validation")
    table(["Tests", "Independent evidence"], [
        ["DSU", "1,000 random unions vs simple labels; component counts and exact counter calls."],
        ["MST", "100 connected random graphs: equal weights and V-1 edges; 25 tiny cases vs exhaustive spanning trees."],
        ["SCC", "50 random graphs (one with V=200): compare mutual BFS reachability; verify exactly 2E DFS scans."],
        ["Topology / paths", "Cycles rejected; order validates every edge; 100 random negative-weight DAGs vs Bellman-Ford; parent paths checked."],
        ["Safety / inputs / benchmark", "100,000-vertex path reaches explicit depth 100,000; four input files, edge cases, reference and custom-run tests. All 17 JUnit tests pass."],
    ], [43, 131])

    page()
    heading("3. Reproducible measurements")
    paragraph("Each graph kind starts with a fresh java.util.Random(42). Shuffle order, tree-edge draws and weight draws follow the specification; when b equals a, only b is redrawn. The ten (n,m) pairs create 30 graphs and 50 algorithm/graph rows. Each algorithm runs five times on the same graph; the CSV stores the median time and internally counted, repeat-checked counters. Generation and result validation are outside timed sections. All ten reference triples match exactly, and Kruskal/Prim agree automatically on every run set.")
    reference = []
    for n in [1000, 3162, 10000, 31623, 100000]:
        for density in ["sparse", "dense"]:
            k = next(r for r in select(rows, "Kruskal", density) if r["n"] == n)
            s = next(r for r in select(rows, "Kosaraju", density) if r["n"] == n)
            d = next(r for r in select(rows, "DagShortestPath", density) if r["n"] == n)
            reference.append([f'{n:,}', f'{k["m"]:,}', f'{k["result"]:,}', s["result"], f'{d["result"]:,}'])
    table(["n", "m", "MST weight", "SCCs", "DAG distance sum"], reference, [28, 32, 38, 24, 52])
    heading("Counters divided by complexity formulas")
    ratios = []
    for algorithm in ["Kruskal", "Prim", "Kosaraju", "Kahn", "DagShortestPath"]:
        values = [next(a for a in analysis if a["algorithm"] == algorithm and a["density"] == d) for d in ["sparse", "dense"]]
        ratios.append([algorithm, "comparisons / (E log2 E)" if algorithm in ["Kruskal", "Prim"] else "scans / (V+E)",
                       *[f'{a["ratio_min"]:.3f}-{a["ratio_max"]:.3f}' for a in values]])
    krows = [r for r in rows if r["algorithm"] == "Kruskal"]
    dsu_ratios = [r["dsu_ops"] / r["edge_scans"] for r in krows]
    ratios.append(["DSU inside Kruskal", "dsu_ops / scanned edge", f'{min(dsu_ratios):.3f}-{max(dsu_ratios):.3f}', "both densities"])
    table(["Algorithm", "Normalization", "Sparse range", "Dense range"], ratios, [40, 59, 38, 37])
    paragraph("Traversal ratios are exactly constant at each density. Sort/heap ratios remain of comparable size rather than being numerically identical; finite samples, repeated weights (only 1..1000) and adaptive Java sorting affect them. For Kruskal, dsu_ops = 2*scans + accepted_edges, so the call ratio above follows from counted operations. normalized.csv also records dsu_ops/E, which varies with early stopping. Call counters cannot measure alpha(V) itself, and a finite plot supports rather than proves an asymptotic bound.")
    times = [r["time_ms"] for r in rows]
    largest_k = select(rows, "Kruskal", "dense")[-1]["time_ms"]
    largest_p = select(rows, "Prim", "dense")[-1]["time_ms"]
    heading("Wall-clock time")
    paragraph(f"Measured medians range from {min(times):.3f} to {max(times):.3f} ms on Java 17.0.20.1, Windows 11 amd64, with a 2 GiB heap. At V=100,000 and E=2,000,000, Kruskal took {largest_k:.3f} ms and lazy Prim {largest_p:.3f} ms. There is no explicit warmup; JIT compilation, garbage collection and background load especially affect small cases. Different hardware and JVMs will change timings, so these observations are secondary to the operation counters. The runtime details are saved beside the CSV.")

    page()
    heading("4. Operation growth and scans per edge")
    chart("operations_vs_n.png", "Figure 1. Log-log operation growth; solid lines are sparse and dashed lines are dense. Operations are the sum of three different counters, an illustrative work proxy rather than a time model. Kosaraju and DAG relaxation overlap because each contributes 2E to this sum.")
    chart("scans_per_edge.png", "Figure 2. Prim and Kosaraju scan 2E entries, while Kahn and DAG relaxation scan E on these DAGs. Overlapping lines have the same measured values. Kruskal's fraction changes because it stops after V-1 accepted edges, although it sorts the complete edge list first.")

    page()
    heading("5. Kruskal's edge share and fitted slopes")
    chart("kruskal_edge_share.png", "Figure 3. Kruskal examines about 77.1%-99.9% of sparse edges and 20.7%-37.2% of dense edges after sorting. The saving is in DSU candidate processing, not in sorting. Lazy Prim still scans both adjacency directions and performs heap operations; density alone does not determine a winner.")
    slopes = []
    for algorithm in ["Kruskal", "Prim", "Kosaraju", "Kahn", "DagShortestPath"]:
        values = [next(a for a in analysis if a["algorithm"] == algorithm and a["density"] == d) for d in ["sparse", "dense"]]
        slopes.append([algorithm, *[f'{a["scan_slope"]:.3f}' for a in values],
                       " / ".join(f'{a["comparison_slope"]:.3f}' for a in values) if values[0]["comparison_slope"] is not None else "not applicable"])
    table(["Algorithm", "Scan slope sparse", "Scan slope dense", "Comparison slopes sparse / dense"], slopes, [38, 39, 38, 59])
    paragraph("Slopes are least-squares fits of ln(counter) against ln(n), separately at each density, using all five sizes. Linear traversals give 1.000; Kruskal's scan slopes are 1.055 and 1.119. The specification attributes the latter to sorting, but post-sort edge_scans do not count sort comparisons: early stopping explains the changing scan fraction. Sorting's worst-case E log E factor belongs to comparisons. The measured comparison slopes and normalized ranges are reported as observed, including the effect of a fixed 1,000-value weight domain.")
    heading("Specification notes and reproduction")
    paragraph("The deliverables section's phrase 'all four generated graphs' conflicts with the detailed ten-pair benchmark and 50-row requirement; this project follows the detailed specification. The Lecture 6 Kruskal pseudocode has an equality typo: an edge is accepted only when its endpoints are in different DSU components. Optional Tarjan/condensation bonuses are outside this core submission. After installing the documented prerequisites, python tools/reproduce.py rebuilds, tests, benchmarks and regenerates these plots and both report formats.")
    paragraph("Sources: DAA_Assignment3_Graphs.pdf, sections 1-10 and rubric; Lecture06 - Disjoint Sets and MST.pdf, slides 3-13. Lecture 5 - Hashing and Trees was reviewed but is not the algorithmic basis for this assignment. Repository: https://github.com/aokoshi/DAA-Assignment3 (main, tag v1.0). AI acknowledgement: OpenAI Codex assisted with the implementation, tests, measurements and report; the student must review and understand the submitted work.")
    (ROOT / "REPORT.md").write_text("\n".join(markdown), encoding="utf-8")
    output = ROOT / "REPORT.pdf"
    document = SimpleDocTemplate(str(output), pagesize=A4, rightMargin=18 * mm, leftMargin=18 * mm,
                                 topMargin=17 * mm, bottomMargin=22 * mm,
                                 title="DAA Assignment 3 - City Network & Job Scheduler",
                                 author="Yelnur Pashanov, SE2523")
    document.build(story, onFirstPage=footer, onLaterPages=footer)
    print("Created REPORT.md and REPORT.pdf.")


if __name__ == "__main__":
    main()
