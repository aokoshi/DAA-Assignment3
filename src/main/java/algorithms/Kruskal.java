package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;
import java.util.ArrayList;
import java.util.List;

public final class Kruskal {
    public static MstResult run(Graph graph, Metrics metrics) {
        if (graph.isDirected()) throw new IllegalArgumentException("MST requires an undirected graph");
        List<Edge> sorted = new ArrayList<>(graph.edges());
        sorted.sort((a, b) -> {
            metrics.comparisons++;
            return Integer.compare(a.weight(), b.weight());
        });
        DisjointSetUnion dsu = new DisjointSetUnion(graph.vertexCount(), metrics);
        List<Edge> chosen = new ArrayList<>();
        long weight = 0;
        for (Edge edge : sorted) {
            if (dsu.components() <= 1) break;
            metrics.edgeScans++;
            // union performs the two finds and rejects edges inside one component.
            if (dsu.union(edge.from(), edge.to())) {
                chosen.add(edge);
                weight += edge.weight();
            }
        }
        return new MstResult(chosen, weight, dsu.components());
    }
}
