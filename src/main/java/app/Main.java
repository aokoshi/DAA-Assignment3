package app;

import algorithms.*;
import benchmark.Benchmark;
import benchmark.GraphGenerator;
import graph.Graph;
import metrics.Metrics;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;

public final class Main {
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                interactive();
                return;
            }
            switch (args[0]) {
                case "analyze" -> {
                    if (args.length < 2 || args.length > 3) throw new IllegalArgumentException("Use analyze <file> [target]");
                    Graph graph = Graph.read(Path.of(args[1]));
                    analyze(graph, args.length == 3 ? Integer.parseInt(args[2]) : graph.vertexCount() - 1);
                }
                case "generate" -> {
                    if (args.length != 5) throw new IllegalArgumentException("Use generate <kind> <n> <m> <file>");
                    Graph graph = GraphGenerator.generate(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), 42);
                    Path output = Path.of(args[4]).toAbsolutePath();
                    Files.createDirectories(output.getParent());
                    try (var writer = Files.newBufferedWriter(output)) {
                        writer.write(graph.vertexCount() + " " + graph.edgeCount() + " "
                                + (graph.isDirected() ? "directed" : "undirected") + "\n");
                        for (var edge : graph.edges()) writer.write(edge.from() + " " + edge.to() + " " + edge.weight() + "\n");
                    }
                    System.out.println("Generated " + output);
                }
                case "benchmark" -> {
                    if (args.length != 1) throw new IllegalArgumentException("Official benchmark takes no arguments");
                    Benchmark.runDefault();
                }
                case "benchmark-custom" -> {
                    if (args.length != 5) throw new IllegalArgumentException("Use benchmark-custom <sizes> <densities> <odd-runs> <csv>");
                    Benchmark.run(parseList(args[1]), parseList(args[2]), Integer.parseInt(args[3]), Path.of(args[4]), false);
                }
                case "help", "--help" -> usage();
                default -> throw new IllegalArgumentException("Unknown command; run help");
            }
        } catch (IOException | IllegalArgumentException | NoSuchElementException exception) {
            System.err.println("Error: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static void interactive() throws IOException {
        Scanner input = new Scanner(System.in);
        System.out.print("Input mode (file/manual): ");
        String mode = input.nextLine().trim();
        Graph graph;
        if (mode.equals("file")) {
            System.out.print("Graph file: ");
            graph = Graph.read(Path.of(input.nextLine().trim()));
        } else if (mode.equals("manual")) {
            System.out.println("Choose n and m: enter n m directed|undirected, then m lines u v w.");
            graph = Graph.read(input);
        } else {
            throw new IllegalArgumentException("Expected file or manual");
        }
        analyze(graph, graph.vertexCount() - 1);
    }

    private static void analyze(Graph graph, int target) {
        if (graph.vertexCount() > 0) graph.checkVertex(target);
        System.out.printf("Vertices: %d, edges: %d, directed: %s%n", graph.vertexCount(), graph.edgeCount(), graph.isDirected());
        if (!graph.isDirected()) {
            var kruskal = Kruskal.run(graph, new Metrics());
            var prim = Prim.run(graph, new Metrics());
            if (kruskal.totalWeight() != prim.totalWeight()) throw new IllegalStateException("MST algorithms disagree");
            System.out.println("Kruskal forest weight: " + kruskal.totalWeight());
            System.out.println("Prim forest weight: " + prim.totalWeight());
            System.out.println("Components: " + kruskal.components() + "; selected edges: " + kruskal.edges().size());
            if (graph.vertexCount() <= 30) System.out.println("Kruskal edges: " + kruskal.edges());
        } else {
            Metrics metrics = new Metrics();
            var scc = Kosaraju.run(graph, metrics);
            System.out.println("Strongly connected components: " + scc.count());
            System.out.println("Maximum explicit stack depth: " + metrics.maxDepth);
            if (graph.vertexCount() <= 30) System.out.println("Component IDs: " + Arrays.toString(scc.componentOf()));
            var topological = Kahn.run(graph, new Metrics());
            if (topological.hasCycle()) {
                System.out.println("Cycle detected: no topological order or DAG shortest paths.");
                return;
            }
            if (!Kahn.isValidOrder(graph, topological.order())) throw new IllegalStateException("Invalid order");
            System.out.println("Valid topological order length: " + topological.order().length);
            if (graph.vertexCount() <= 30) System.out.println("Order: " + Arrays.toString(topological.order()));
            var paths = DagShortestPath.run(graph, new Metrics());
            System.out.println("Sum of distances from all zero-indegree sources: " + paths.distanceSum());
            if (graph.vertexCount() <= 30) System.out.println("Distances: " + Arrays.toString(paths.distance()));
            if (graph.vertexCount() > 0) System.out.println("Path to " + target + ": " + paths.pathTo(target)
                    + "; distance: " + paths.distance()[target]);
        }
    }

    private static int[] parseList(String value) {
        return Arrays.stream(value.split(",")).mapToInt(Integer::parseInt).toArray();
    }

    private static void usage() {
        System.out.println("No arguments: interactive file or manual input with your own n and m.");
        System.out.println("analyze <file> [target]");
        System.out.println("generate <undirected|directed|dag> <n> <m> <file>");
        System.out.println("benchmark");
        System.out.println("benchmark-custom <n1,n2,...> <m/n1,m/n2,...> <odd-runs> <csv>");
    }
}
