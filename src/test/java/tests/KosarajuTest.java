package tests;

import algorithms.Kosaraju;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class KosarajuTest {
    @Test
    void agreesWithBfsReachabilityInBothDirections() {
        Random random = new Random(22);
        for (int trial = 0; trial < 50; trial++) {
            int n = trial == 0 ? 200 : 1 + random.nextInt(60);
            Graph graph = new Graph(n, true);
            for (int i = 0; i < 3 * n; i++) graph.addEdge(random.nextInt(n), random.nextInt(n), 1);
            boolean[][] reachable = new boolean[n][];
            for (int v = 0; v < n; v++) reachable[v] = bfs(graph, v);
            Metrics metrics = new Metrics();
            var result = Kosaraju.run(graph, metrics);
            for (int a = 0; a < n; a++) {
                for (int b = 0; b < n; b++) {
                    assertEquals(reachable[a][b] && reachable[b][a], result.componentOf()[a] == result.componentOf()[b]);
                }
            }
            assertEquals(2L * graph.edgeCount(), metrics.edgeScans);
        }
    }

    @Test
    void survivesAPathOfOneHundredThousandVertices() {
        Graph path = new Graph(100_000, true);
        for (int v = 1; v < path.vertexCount(); v++) path.addEdge(v - 1, v, 1);
        Metrics metrics = new Metrics();
        assertEquals(100_000, Kosaraju.run(path, metrics).count());
        assertEquals(100_000, metrics.maxDepth);
        assertEquals(199_998, metrics.edgeScans);
    }

    @Test
    void handlesCyclicExampleAndEmptyGraph() throws Exception {
        assertEquals(3, Kosaraju.run(Graph.read(Path.of("data/cyclic.txt")), new Metrics()).count());
        assertEquals(0, Kosaraju.run(new Graph(0, true), new Metrics()).count());
        assertEquals(4, Kosaraju.run(new Graph(4, true), new Metrics()).count());
        assertThrows(IllegalArgumentException.class, () -> Kosaraju.run(new Graph(2, false), new Metrics()));
    }

    private static boolean[] bfs(Graph graph, int source) {
        boolean[] visited = new boolean[graph.vertexCount()];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        visited[source] = true;
        queue.add(source);
        while (!queue.isEmpty()) {
            for (var edge : graph.outgoing(queue.remove())) {
                if (!visited[edge.to()]) {
                    visited[edge.to()] = true;
                    queue.add(edge.to());
                }
            }
        }
        return visited;
    }
}
