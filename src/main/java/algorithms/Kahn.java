package algorithms;

import graph.Graph;
import metrics.Metrics;
import java.util.ArrayDeque;
import java.util.Arrays;

public final class Kahn {
    public static TopologicalResult run(Graph graph, Metrics metrics) {
        if (!graph.isDirected()) throw new IllegalArgumentException("Topological order requires a directed graph");
        int n = graph.vertexCount();
        int[] inDegree = graph.inDegrees();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int v = 0; v < n; v++) if (inDegree[v] == 0) queue.add(v);
        int[] order = new int[n];
        int size = 0;
        while (!queue.isEmpty()) {
            int vertex = queue.remove();
            order[size++] = vertex;
            for (var edge : graph.outgoing(vertex)) {
                metrics.edgeScans++;
                if (--inDegree[edge.to()] == 0) queue.add(edge.to());
            }
        }
        if (size != n) return new TopologicalResult(new int[0], true);
        return new TopologicalResult(order, false);
    }

    public static boolean isValidOrder(Graph graph, int[] order) {
        if (!graph.isDirected() || order.length != graph.vertexCount()) return false;
        int[] position = new int[graph.vertexCount()];
        Arrays.fill(position, -1);
        for (int i = 0; i < order.length; i++) {
            int v = order[i];
            if (v < 0 || v >= position.length || position[v] != -1) return false;
            position[v] = i;
        }
        for (var edge : graph.edges()) if (position[edge.from()] >= position[edge.to()]) return false;
        return true;
    }
}
