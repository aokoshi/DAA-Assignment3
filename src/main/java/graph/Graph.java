package graph;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public final class Graph {
    private final boolean directed;
    private final List<List<Edge>> adjacency;
    private final List<Edge> edges = new ArrayList<>();
    private final int[] inDegree;

    public Graph(int vertices, boolean directed) {
        if (vertices < 0) throw new IllegalArgumentException("Negative vertex count");
        this.directed = directed;
        adjacency = new ArrayList<>(vertices);
        inDegree = new int[vertices];
        for (int v = 0; v < vertices; v++) adjacency.add(new ArrayList<>());
    }

    public int vertexCount() { return adjacency.size(); }
    public int edgeCount() { return edges.size(); }
    public boolean isDirected() { return directed; }
    public List<Edge> edges() { return Collections.unmodifiableList(edges); }

    public List<Edge> outgoing(int vertex) {
        checkVertex(vertex);
        return Collections.unmodifiableList(adjacency.get(vertex));
    }

    public int[] inDegrees() { return inDegree.clone(); }

    public void addEdge(int from, int to, int weight) {
        checkVertex(from);
        checkVertex(to);
        Edge edge = new Edge(from, to, weight);
        edges.add(edge);
        adjacency.get(from).add(edge);
        inDegree[to]++;
        if (!directed) {
            adjacency.get(to).add(new Edge(to, from, weight));
            inDegree[from]++;
        }
    }

    public Graph reversed() {
        if (!directed) throw new IllegalArgumentException("Reversal requires a directed graph");
        Graph reversed = new Graph(vertexCount(), true);
        for (Edge edge : edges) reversed.addEdge(edge.to(), edge.from(), edge.weight());
        return reversed;
    }

    public void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= vertexCount()) {
            throw new IllegalArgumentException("Vertex outside 0..n-1: " + vertex);
        }
    }

    public static Graph read(Path path) throws IOException {
        try (Scanner input = new Scanner(path)) {
            return read(input);
        }
    }

    public static Graph read(Scanner input) {
        int vertices = input.nextInt();
        int count = input.nextInt();
        if (count < 0) throw new IllegalArgumentException("Negative edge count");
        String kind = input.next();
        if (!kind.equals("directed") && !kind.equals("undirected")) {
            throw new IllegalArgumentException("Expected directed or undirected");
        }
        Graph graph = new Graph(vertices, kind.equals("directed"));
        for (int i = 0; i < count; i++) {
            graph.addEdge(input.nextInt(), input.nextInt(), input.nextInt());
        }
        return graph;
    }
}
