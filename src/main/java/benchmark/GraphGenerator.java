package benchmark;

import graph.Graph;
import java.util.Random;

public final class GraphGenerator {
    public static Graph generate(String kind, int n, int m, long seed) {
        if (!kind.equals("undirected") && !kind.equals("directed") && !kind.equals("dag")) {
            throw new IllegalArgumentException("Graph kind must be undirected, directed or dag");
        }
        if (n < 0 || m < 0 || (n < 2 && m > 0)) {
            throw new IllegalArgumentException("Cannot generate these n and m without self-loops");
        }
        if (kind.equals("undirected") && m < n - 1) {
            throw new IllegalArgumentException("The connected generator requires m >= n-1");
        }
        Random random = new Random(seed);
        Graph graph = new Graph(n, !kind.equals("undirected"));
        int[] rank = new int[n];
        for (int v = 0; v < n; v++) rank[v] = v;
        if (!kind.equals("directed")) {
            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int temporary = rank[i];
                rank[i] = rank[j];
                rank[j] = temporary;
            }
        }
        if (kind.equals("undirected")) {
            for (int i = 1; i < n; i++) {
                graph.addEdge(rank[random.nextInt(i)], rank[i], 1 + random.nextInt(1000));
            }
        }
        while (graph.edgeCount() < m) {
            int a = random.nextInt(n);
            int b = random.nextInt(n);
            // Keep a and redraw only b; this reproduces the assignment's table.
            while (b == a) b = random.nextInt(n);
            if (kind.equals("dag") && rank[a] > rank[b]) {
                int temporary = a;
                a = b;
                b = temporary;
            }
            graph.addEdge(a, b, 1 + random.nextInt(1000));
        }
        return graph;
    }
}
