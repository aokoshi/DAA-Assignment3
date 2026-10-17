package tests;

import algorithms.*;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class MstTest {
    @Test
    void agreeOnOneHundredConnectedGraphs() {
        Random random = new Random(31);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + random.nextInt(79);
            Graph graph = new Graph(n, false);
            for (int v = 1; v < n; v++) graph.addEdge(random.nextInt(v), v, random.nextInt(41) - 20);
            for (int i = 0; i < 5 * n; i++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(41) - 20);
            }
            MstResult kruskal = Kruskal.run(graph, new Metrics());
            MstResult prim = Prim.run(graph, new Metrics());
            assertEquals(kruskal.totalWeight(), prim.totalWeight());
            assertEquals(n - 1, kruskal.edges().size());
            assertEquals(n - 1, prim.edges().size());
            assertEquals(1, kruskal.components());
            assertEquals(1, prim.components());
            DisjointSetUnion dsu = new DisjointSetUnion(n, new Metrics());
            for (var edge : kruskal.edges()) assertTrue(dsu.union(edge.from(), edge.to()));
        }
    }

    @Test
    void handlesFilesForestsSelfLoopsAndParallelEdges() throws Exception {
        Graph connected = Graph.read(Path.of("data/connected.txt"));
        assertEquals(10, Kruskal.run(connected, new Metrics()).totalWeight());
        Graph graph = Graph.read(Path.of("data/disconnected.txt"));
        for (MstResult result : new MstResult[]{Kruskal.run(graph, new Metrics()), Prim.run(graph, new Metrics())}) {
            assertEquals(8, result.totalWeight());
            assertEquals(3, result.components());
            assertEquals(3, result.edges().size());
        }
        assertEquals(0, Prim.run(new Graph(0, false), new Metrics()).components());
        assertEquals(0, Kruskal.run(new Graph(1, false), new Metrics()).totalWeight());
        assertThrows(IllegalArgumentException.class, () -> Prim.run(new Graph(2, true), new Metrics()));
        assertThrows(IllegalArgumentException.class, () -> Kruskal.run(new Graph(2, true), new Metrics()));
    }

    @Test
    void agreesWithExhaustiveSpanningTreeSearch() {
        Random random = new Random(7);
        for (int trial = 0; trial < 25; trial++) {
            Graph graph = new Graph(5, false);
            for (int v = 1; v < 5; v++) graph.addEdge(v - 1, v, random.nextInt(20) - 10);
            for (int i = 0; i < 4; i++) graph.addEdge(random.nextInt(5), random.nextInt(5), random.nextInt(20) - 10);
            long best = Long.MAX_VALUE;
            for (int mask = 0; mask < (1 << graph.edgeCount()); mask++) {
                if (Integer.bitCount(mask) != 4) continue;
                DisjointSetUnion dsu = new DisjointSetUnion(5, new Metrics());
                long weight = 0;
                for (int i = 0; i < graph.edgeCount(); i++) {
                    if ((mask & (1 << i)) == 0) continue;
                    var edge = graph.edges().get(i);
                    dsu.union(edge.from(), edge.to());
                    weight += edge.weight();
                }
                if (dsu.components() == 1) best = Math.min(best, weight);
            }
            assertEquals(best, Kruskal.run(graph, new Metrics()).totalWeight());
            assertEquals(best, Prim.run(graph, new Metrics()).totalWeight());
        }
    }
}
