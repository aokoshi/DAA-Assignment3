package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public final class Prim {
    public static MstResult run(Graph graph, Metrics metrics) {
        if (graph.isDirected()) throw new IllegalArgumentException("MST requires an undirected graph");
        boolean[] visited = new boolean[graph.vertexCount()];
        PriorityQueue<Edge> queue = new PriorityQueue<>((a, b) -> {
            metrics.comparisons++;
            return Integer.compare(a.weight(), b.weight());
        });
        List<Edge> chosen = new ArrayList<>();
        long weight = 0;
        int components = 0;
        // Start at 0, then restart at the first unvisited vertex for a forest.
        for (int start = 0; start < graph.vertexCount(); start++) {
            if (visited[start]) continue;
            components++;
            visited[start] = true;
            addOutgoing(graph, start, visited, queue, metrics);
            while (!queue.isEmpty()) {
                Edge edge = queue.remove();
                if (visited[edge.to()]) continue;
                visited[edge.to()] = true;
                chosen.add(edge);
                weight += edge.weight();
                addOutgoing(graph, edge.to(), visited, queue, metrics);
            }
        }
        return new MstResult(chosen, weight, components);
    }

    private static void addOutgoing(Graph graph, int vertex, boolean[] visited,
                                    PriorityQueue<Edge> queue, Metrics metrics) {
        for (Edge edge : graph.outgoing(vertex)) {
            metrics.edgeScans++;
            if (!visited[edge.to()]) queue.add(edge);
        }
    }
}
