package tests;

import algorithms.*;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class PathsTest {
    @Test
    void detectsCyclesAndValidatesWholeOrders() throws Exception {
        Graph cyclic = Graph.read(Path.of("data/cyclic.txt"));
        var result = Kahn.run(cyclic, new Metrics());
        assertTrue(result.hasCycle());
        assertEquals(0, result.order().length);
        assertThrows(IllegalArgumentException.class, () -> DagShortestPath.run(cyclic, new Metrics()));
        Graph dag = Graph.read(Path.of("data/dag-negative.txt"));
        assertTrue(Kahn.isValidOrder(dag, Kahn.run(dag, new Metrics()).order()));
        assertFalse(Kahn.isValidOrder(dag, new int[]{0, 1, 2, 3, 4, 5, 5}));
        Graph selfLoop = new Graph(1, true);
        selfLoop.addEdge(0, 0, 1);
        assertTrue(Kahn.run(selfLoop, new Metrics()).hasCycle());
    }

    @Test
    void restoresPathsFromAllZeroIndegreeSources() throws Exception {
        Graph graph = Graph.read(Path.of("data/dag-negative.txt"));
        Metrics metrics = new Metrics();
        var result = DagShortestPath.run(graph, metrics);
        assertArrayEquals(new long[]{0, 0, 1, -2, 0, -1, 0}, result.distance());
        assertEquals(List.of(1, 2, 3, 4, 5), result.pathTo(5));
        assertEquals(List.of(6), result.pathTo(6));
        assertEquals(-2, result.distanceSum());
        assertEquals(graph.edgeCount(), metrics.edgeScans);
        assertEquals(graph.edgeCount(), metrics.comparisons);
    }

    @Test
    void agreesWithBellmanFordOnOneHundredNegativeWeightDags() {
        Random random = new Random(37);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + random.nextInt(70);
            Graph graph = new Graph(n, true);
            for (int i = 0; i < 5 * n; i++) {
                int a = random.nextInt(n);
                int b = random.nextInt(n);
                if (a == b) continue;
                graph.addEdge(Math.min(a, b), Math.max(a, b), random.nextInt(61) - 30);
            }
            Metrics metrics = new Metrics();
            var order = Kahn.run(graph, metrics);
            assertFalse(order.hasCycle());
            assertTrue(Kahn.isValidOrder(graph, order.order()));
            assertEquals(graph.edgeCount(), metrics.edgeScans);
            var result = DagShortestPath.run(graph, new Metrics());
            assertArrayEquals(bellmanFord(graph), result.distance());
            int[] inDegree = graph.inDegrees();
            for (int v = 0; v < n; v++) {
                var path = result.pathTo(v);
                assertEquals(0, inDegree[path.get(0)]);
                assertEquals(v, path.get(path.size() - 1));
                for (int i = 1; i < path.size(); i++) {
                    int from = path.get(i - 1);
                    int to = path.get(i);
                    assertTrue(graph.outgoing(from).stream().anyMatch(e -> e.to() == to
                            && result.distance()[from] + e.weight() == result.distance()[to]));
                }
            }
        }
    }

    @Test
    void handlesEmptyIsolatedAndLargeWeightGraphs() {
        assertEquals(0, DagShortestPath.run(new Graph(0, true), new Metrics()).distanceSum());
        assertArrayEquals(new long[4], DagShortestPath.run(new Graph(4, true), new Metrics()).distance());
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 2, Integer.MAX_VALUE);
        assertEquals(2L * Integer.MAX_VALUE, DagShortestPath.run(graph, new Metrics()).distance()[2]);
        assertThrows(IllegalArgumentException.class, () -> Kahn.run(new Graph(2, false), new Metrics()));
    }

    private static long[] bellmanFord(Graph graph) {
        long[] distance = new long[graph.vertexCount()];
        Arrays.fill(distance, Long.MAX_VALUE);
        int[] inDegree = graph.inDegrees();
        for (int v = 0; v < distance.length; v++) if (inDegree[v] == 0) distance[v] = 0;
        for (int pass = 1; pass < distance.length; pass++) {
            boolean changed = false;
            for (var edge : graph.edges()) {
                if (distance[edge.from()] == Long.MAX_VALUE) continue;
                long candidate = distance[edge.from()] + edge.weight();
                if (candidate < distance[edge.to()]) {
                    distance[edge.to()] = candidate;
                    changed = true;
                }
            }
            if (!changed) break;
        }
        return distance;
    }
}
