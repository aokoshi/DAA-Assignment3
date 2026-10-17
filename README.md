# DAA Assignment 3 - City Network & Job Scheduler

**Student:** Yelnur Pashanov, SE2523  
**Language:** Java 17  
**Release:** `main`, tag `v1.0`  
**Repository:** https://github.com/aokoshi/DAA-Assignment3

The project implements DSU, Kruskal, Prim, Kosaraju, Kahn and shortest paths in a DAG.
All graph algorithms use one adjacency-list `Graph` class. There is no external graph library.

## Build and test

Requirements: JDK 17 or newer and Maven 3.9 or newer. Set `JAVA_HOME` to the JDK and
make `mvn` available on `PATH`. In IntelliJ IDEA, open `pom.xml`, choose JDK 17 and run
the Maven `verify` goal from the Maven panel.

```text
mvn clean verify
```

This runs 17 JUnit 5 tests and builds `target/daa-assignment3-1.0.jar`.
The tests include 100 MST cross-checks, exhaustive small spanning trees, SCC vs BFS,
100 negative-weight DAGs vs Bellman-Ford, restored paths and a 100,000-vertex path.

## Choose your own input

Run without arguments for interactive input:

```text
java -jar target/daa-assignment3-1.0.jar
```

Choose `manual`, enter your own vertex count `n`, edge count `m` and graph direction,
then enter exactly `m` edges. Alternatively choose `file` and enter a file path.
The file format is:

```text
n m directed|undirected
u v weight
... exactly m edge lines ...
```

Vertices are `0..n-1`, weights are signed Java `int` values, and totals use `long`.
Parallel edges, self-loops, isolated vertices, empty graphs and disconnected graphs
are supported. The `generate` command excludes self-loops and permits parallel edges;
its undirected mode creates a connected graph, so it needs `m >= n-1`.

Examples:

```text
java -jar target/daa-assignment3-1.0.jar analyze data/connected.txt
java -jar target/daa-assignment3-1.0.jar analyze data/disconnected.txt
java -jar target/daa-assignment3-1.0.jar analyze data/cyclic.txt
java -jar target/daa-assignment3-1.0.jar analyze data/dag-negative.txt 5
java -jar target/daa-assignment3-1.0.jar generate dag 50 200 data/my-dag.txt
java -jar target/daa-assignment3-1.0.jar analyze data/my-dag.txt 49
```

The optional final argument of `analyze` selects the target for path restoration;
otherwise it uses `n-1`. Small graphs also print edges, component IDs, orders and
distance arrays. Expected supplied-example results:

| File | Expected result |
| --- | --- |
| `connected.txt` | MST weight 10, 1 component |
| `disconnected.txt` | Forest weight 8, 3 components, 3 selected edges |
| `cyclic.txt` | 3 SCCs; cycle reported; no DAG order or shortest paths |
| `dag-negative.txt` | Distances `[0,0,1,-2,0,-1,0]`; sum -2; path to 5 is `[1,2,3,4,5]` |

## Official benchmark

```text
java -Xmx2g -jar target/daa-assignment3-1.0.jar benchmark
```

The required sizes are 1,000, 3,162, 10,000, 31,623 and 100,000, each with `m=4n`
and `m=20n`. Each pair produces an undirected graph for MST, a directed graph for
SCC, and a DAG for Kahn/shortest paths. Each graph starts with a fresh `Random(42)`.
When random endpoints coincide, only the second endpoint is redrawn.

Five runs per algorithm/graph produce 50 CSV rows in `results/results.csv`:

```text
algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result
```

Time is the median of five runs with no explicit warmup. Counters must agree across
runs. The benchmark fails if the MST weights differ, an order is invalid, a forest
has the wrong number of edges, scan identities fail or any reference value differs.
It writes the CSV only after every check succeeds. All ten reference triples were
reproduced exactly. The Java/OS/heap/run settings are saved beside the CSV.

For a quicker demonstration, choose your own sizes, densities and odd run count:

```text
java -Xmx2g -jar target/daa-assignment3-1.0.jar benchmark-custom 100,300,1000 4,20 3 results/demo.csv
```

The assignment fixes the official input count; it does **not** require the student
to choose that count. Custom input is available in addition to the official run.

## Plots, report and one-command reproduction

The final report is [REPORT.pdf](REPORT.pdf) (5 pages), with the same content in
[REPORT.md](REPORT.md). The three PNG charts are in `results/plots/`.

For regeneration, also install Python 3.10 or newer and the plotting/PDF packages:

```text
python -m pip install -r tools/requirements.txt
python tools/reproduce.py
```

After prerequisites are installed, the second command builds the project, runs the
tests and complete benchmark, and regenerates all analysis, plots and report files.
Timings will change; graph results and algorithm counters should reproduce under
the documented Java 17 implementation. To analyze the already supplied CSV only:

```text
python tools/analyze_results.py
python tools/build_report.py
```

`results/normalized.csv` contains every size's normalized counters.
`results/analysis.json` contains fitted log-log slopes and ratio ranges.

## Source map

| Location | Responsibility |
| --- | --- |
| `src/main/java/graph` | One directed/undirected graph class, edges and file input |
| `src/main/java/algorithms/DisjointSetUnion.java` | Union by size, full path compression, component count |
| `src/main/java/algorithms/Kruskal.java`, `Prim.java` | Minimum spanning forests |
| `src/main/java/algorithms/Kosaraju.java` | Two iterative DFS passes and component IDs |
| `src/main/java/algorithms/Kahn.java` | Topological order, cycle detection and order validation |
| `src/main/java/algorithms/DagShortestPath.java` | All zero-in-degree sources, negative edges and parents |
| `src/main/java/metrics` | Counters incremented inside algorithm operations |
| `src/main/java/benchmark` | Exact seeded generator, median runs and automatic checks |
| `src/main/java/app/Main.java` | File/manual input, generation, analysis and benchmark commands |
| `src/test/java/tests` | JUnit 5 cross-checks and edge-case tests |
| `data` | Four small input files used in tests |
| `tools` | Reproduction, plotting and report generation |

## Measurement conventions and specification notes

- `dsu_ops = find calls + successful unions`; internal parent traversals are excluded.
- Kruskal counts post-sort candidate scans, Prim counts both adjacency directions,
  Kosaraju counts its two DFS passes, Kahn counts processed DAG edges, and DAG shortest
  paths count relaxations. Reversal construction, loading and validation are excluded
  from scan counters. DAG time includes its Kahn preprocessing.
- `comparisons` counts sorting/heap weight comparisons and DAG candidate-distance
  comparisons; `max_depth` records the largest explicit DFS stack occupancy.
- Lazy Prim uses an `O(E)` heap, so the precise multigraph bound is `O(V+E log(E+1))`;
  the familiar `O(E log V)` bound applies to connected simple graphs.
- The deliverables' phrase "all four generated graphs" conflicts with the detailed
  ten-pair/50-row specification. The detailed specification is followed.
- Kruskal's sorting log factor appears in comparisons, not in its post-sort scans.
  The report gives measured slopes and explains early stopping rather than forcing
  the counters to an expected slope.
- The equality sign in Lecture 6's Kruskal pseudocode is a typo: only different DSU
  components may be joined. Optional Tarjan and condensation bonuses are not included.

## Git workflow and submission

The repository retains `feature/dsu`, `feature/mst`, `feature/scc`, `feature/paths`
and `feature/metrics`. Features were implemented in small, purpose-specific commits
and merged into working `main`; the completed release is tagged `v1.0`.
