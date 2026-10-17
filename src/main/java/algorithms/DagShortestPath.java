package algorithms;

import graph.Graph;
import metrics.Metrics;
import java.util.Arrays;

public final class DagShortestPath {
    public static ShortestPathResult run(Graph graph, Metrics metrics) {
        TopologicalResult topological = Kahn.run(graph, new Metrics());
        if (topological.hasCycle()) throw new IllegalArgumentException("Shortest paths require a DAG");
        int n = graph.vertexCount();
        long[] distance = new long[n];
        int[] parent = new int[n];
        Arrays.fill(distance, Long.MAX_VALUE);
        Arrays.fill(parent, -1);
        int[] inDegree = graph.inDegrees();
        for (int v = 0; v < n; v++) if (inDegree[v] == 0) distance[v] = 0;
        for (int vertex : topological.order()) {
            for (var edge : graph.outgoing(vertex)) {
                metrics.edgeScans++;
                long candidate = distance[vertex] + edge.weight();
                metrics.comparisons++;
                if (candidate < distance[edge.to()]) {
                    distance[edge.to()] = candidate;
                    parent[edge.to()] = vertex;
                }
            }
        }
        return new ShortestPathResult(distance, parent);
    }
}
