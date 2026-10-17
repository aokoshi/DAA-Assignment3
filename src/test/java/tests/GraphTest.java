package tests;

import graph.Graph;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class GraphTest {
    @Test
    void storesParallelEdgesSelfLoopsAndIsolatedVertices() {
        Graph graph = new Graph(4, false);
        graph.addEdge(0, 1, 5);
        graph.addEdge(0, 1, 2);
        graph.addEdge(2, 2, -1);
        assertEquals(3, graph.edgeCount());
        assertEquals(2, graph.outgoing(0).size());
        assertEquals(2, graph.outgoing(2).size());
        assertTrue(graph.outgoing(3).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(0, 4, 0));
    }

    @Test
    void loadsAllRequiredExampleKinds() throws Exception {
        assertFalse(Graph.read(Path.of("data/connected.txt")).isDirected());
        assertEquals(6, Graph.read(Path.of("data/disconnected.txt")).vertexCount());
        assertTrue(Graph.read(Path.of("data/cyclic.txt")).isDirected());
        assertEquals(7, Graph.read(Path.of("data/dag-negative.txt")).vertexCount());
    }

    @Test
    void reversesEdgesAndRejectsMalformedHeaders() {
        Graph graph = Graph.read(new Scanner("3 2 directed 0 1 -2 2 1 4"));
        assertArrayEquals(new int[]{0, 2, 0}, graph.inDegrees());
        assertEquals(2, graph.reversed().outgoing(1).size());
        assertThrows(IllegalArgumentException.class,
                () -> Graph.read(new Scanner("3 0 other")));
        assertThrows(IllegalArgumentException.class, () -> new Graph(-1, true));
    }
}
