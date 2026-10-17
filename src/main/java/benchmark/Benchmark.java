package benchmark;

import algorithms.*;
import graph.Graph;
import metrics.Metrics;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class Benchmark {
    public static final int[] SIZES = {1000, 3162, 10000, 31623, 100000};
    public static final int[] DENSITIES = {4, 20};
    private static final long[] MST = {147970, 30363, 468911, 97589, 1486139,
            309550, 4715515, 968517, 15012094, 3056681};
    private static final long[] SCC = {27, 1, 137, 1, 373, 1, 1188, 1, 3923, 1};
    private static final long[] DAG = {396274, 252411, 1271911, 723157, 3941828,
            2381342, 12551891, 7376612, 40096539, 23512087};

    private record Measurement(double medianMs, Metrics metrics, long result) { }

    public static void runDefault() throws IOException {
        run(SIZES, DENSITIES, 5, Path.of("results/results.csv"), true);
    }

    public static void run(int[] sizes, int[] densities, int runs, Path output,
                           boolean checkReference) throws IOException {
        if (runs <= 0 || runs % 2 == 0) throw new IllegalArgumentException("Use a positive odd run count");
        if (sizes.length == 0 || densities.length == 0) throw new IllegalArgumentException("Empty benchmark selection");
        if (checkReference && (!Arrays.equals(sizes, SIZES) || !Arrays.equals(densities, DENSITIES))) {
            throw new IllegalArgumentException("Reference checking requires the official sizes and densities");
        }
        List<String> rows = new ArrayList<>();
        rows.add("algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result");
        int reference = 0;
        for (int n : sizes) {
            if (n < 2) throw new IllegalArgumentException("Benchmark sizes must be at least 2");
            for (int density : densities) {
                if (density < 1) throw new IllegalArgumentException("Density must be positive");
                int m = Math.multiplyExact(n, density);
                String label = density == 4 ? "sparse" : density == 20 ? "dense" : "d" + density;
                Graph graph = GraphGenerator.generate("undirected", n, m, 42);
                Measurement kruskal = measure("Kruskal", graph, runs);
                Measurement prim = measure("Prim", graph, runs);
                require(kruskal.result == prim.result, "Kruskal and Prim weights disagree");
                addRow(rows, "Kruskal", "undirected_" + label, n, m, kruskal);
                addRow(rows, "Prim", "undirected_" + label, n, m, prim);

                graph = GraphGenerator.generate("directed", n, m, 42);
                Measurement kosaraju = measure("Kosaraju", graph, runs);
                addRow(rows, "Kosaraju", "directed_" + label, n, m, kosaraju);

                graph = GraphGenerator.generate("dag", n, m, 42);
                Measurement kahn = measure("Kahn", graph, runs);
                Measurement paths = measure("DagShortestPath", graph, runs);
                addRow(rows, "Kahn", "dag_" + label, n, m, kahn);
                addRow(rows, "DagShortestPath", "dag_" + label, n, m, paths);
                if (checkReference) {
                    require(kruskal.result == MST[reference], "MST reference mismatch at " + n + "/" + m);
                    require(kosaraju.result == SCC[reference], "SCC reference mismatch at " + n + "/" + m);
                    require(paths.result == DAG[reference], "DAG reference mismatch at " + n + "/" + m);
                }
                System.out.printf("%d / %d: MST=%d, SCC=%d, DAG distance sum=%d%n",
                        n, m, kruskal.result, kosaraju.result, paths.result);
                reference++;
            }
        }
        Path absolute = output.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        // Publish a CSV only after every agreement, order and reference check succeeds.
        Files.write(absolute, rows);
        Files.writeString(absolute.resolveSibling(output.getFileName() + ".environment.txt"),
                "Java: " + System.getProperty("java.version") + "\nVM: " + System.getProperty("java.vm.name")
                + "\nOS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch")
                + "\nProcessors: " + Runtime.getRuntime().availableProcessors()
                + "\nMax heap bytes: " + Runtime.getRuntime().maxMemory()
                + "\nRuns: " + runs + " (median); no explicit warmup\nSeed: new Random(42) per graph"
                + "\nSizes: " + Arrays.toString(sizes) + "\nDensities: " + Arrays.toString(densities) + "\n");
        System.out.println("Validated " + (rows.size() - 1) + " CSV rows: " + output);
    }

    private static Measurement measure(String algorithm, Graph graph, int runs) {
        double[] times = new double[runs];
        Metrics firstMetrics = null;
        long firstResult = 0;
        for (int run = 0; run < runs; run++) {
            Metrics metrics = new Metrics();
            long started = System.nanoTime();
            long result;
            switch (algorithm) {
                case "Kruskal", "Prim" -> {
                    MstResult forest = algorithm.equals("Kruskal")
                            ? Kruskal.run(graph, metrics) : Prim.run(graph, metrics);
                    times[run] = (System.nanoTime() - started) / 1_000_000.0;
                    require(forest.components() == 1, "Generated MST graph is not connected");
                    require(forest.edges().size() == graph.vertexCount() - 1, "Wrong MST edge count");
                    result = forest.totalWeight();
                }
                case "Kosaraju" -> {
                    result = Kosaraju.run(graph, metrics).count();
                    times[run] = (System.nanoTime() - started) / 1_000_000.0;
                    require(metrics.edgeScans == 2L * graph.edgeCount(), "Kosaraju must scan 2m DFS edges");
                }
                case "Kahn" -> {
                    TopologicalResult topological = Kahn.run(graph, metrics);
                    times[run] = (System.nanoTime() - started) / 1_000_000.0;
                    require(!topological.hasCycle() && Kahn.isValidOrder(graph, topological.order()), "Invalid DAG order");
                    require(metrics.edgeScans == graph.edgeCount(), "Kahn must scan m edges on a DAG");
                    result = topological.order().length;
                }
                case "DagShortestPath" -> {
                    ShortestPathResult paths = DagShortestPath.run(graph, metrics);
                    times[run] = (System.nanoTime() - started) / 1_000_000.0;
                    result = paths.distanceSum();
                    require(metrics.edgeScans == graph.edgeCount(), "DAG relaxation must scan m edges");
                }
                default -> throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
            }
            if (run == 0) {
                firstMetrics = metrics;
                firstResult = result;
            } else {
                require(result == firstResult && metrics.dsuOps() == firstMetrics.dsuOps()
                        && metrics.edgeScans == firstMetrics.edgeScans
                        && metrics.comparisons == firstMetrics.comparisons
                        && metrics.maxDepth == firstMetrics.maxDepth, "Non-reproducible counters or result");
            }
        }
        Arrays.sort(times);
        return new Measurement(times[runs / 2], firstMetrics, firstResult);
    }

    private static void addRow(List<String> rows, String algorithm, String graph, int n, int m,
                               Measurement measurement) {
        Metrics metrics = measurement.metrics;
        rows.add(String.format(Locale.ROOT, "%s,%s,%d,%d,%.6f,%d,%d,%d,%d,%d", algorithm, graph,
                n, m, measurement.medianMs, metrics.dsuOps(), metrics.edgeScans,
                metrics.comparisons, metrics.maxDepth, measurement.result));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
