package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;
import java.util.Arrays;
import java.util.List;

public final class Kosaraju {
    public static SccResult run(Graph graph, Metrics metrics) {
        if (!graph.isDirected()) throw new IllegalArgumentException("SCC requires a directed graph");
        int n = graph.vertexCount();
        boolean[] visited = new boolean[n];
        int[] finish = new int[n];
        int finishCount = 0;
        int[] stack = new int[n];
        int[] nextEdge = new int[n];
        for (int start = 0; start < n; start++) {
            if (visited[start]) continue;
            int depth = 1;
            stack[0] = start;
            nextEdge[0] = 0;
            visited[start] = true;
            metrics.recordDepth(depth);
            while (depth > 0) {
                int vertex = stack[depth - 1];
                List<Edge> edges = graph.outgoing(vertex);
                if (nextEdge[depth - 1] == edges.size()) {
                    finish[finishCount++] = vertex;
                    depth--;
                } else {
                    Edge edge = edges.get(nextEdge[depth - 1]++);
                    metrics.edgeScans++;
                    if (!visited[edge.to()]) {
                        visited[edge.to()] = true;
                        stack[depth] = edge.to();
                        nextEdge[depth] = 0;
                        depth++;
                        metrics.recordDepth(depth);
                    }
                }
            }
        }
        Graph reversed = graph.reversed();
        int[] component = new int[n];
        Arrays.fill(component, -1);
        int count = 0;
        for (int i = finishCount - 1; i >= 0; i--) {
            int start = finish[i];
            if (component[start] != -1) continue;
            int depth = 1;
            stack[0] = start;
            component[start] = count;
            metrics.recordDepth(depth);
            while (depth > 0) {
                int vertex = stack[--depth];
                for (Edge edge : reversed.outgoing(vertex)) {
                    metrics.edgeScans++;
                    if (component[edge.to()] == -1) {
                        component[edge.to()] = count;
                        stack[depth++] = edge.to();
                        metrics.recordDepth(depth);
                    }
                }
            }
            count++;
        }
        return new SccResult(component, count);
    }
}
