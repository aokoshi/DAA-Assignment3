package algorithms;

import metrics.Metrics;

public final class DisjointSetUnion {
    private int[] parent;
    private int[] size;
    private int components;
    private final Metrics metrics;

    public DisjointSetUnion(int n, Metrics metrics) {
        this.metrics = metrics;
        makeSet(n);
    }

    public void makeSet(int n) {
        if (n < 0) throw new IllegalArgumentException("Negative set count");
        parent = new int[n];
        size = new int[n];
        components = n;
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }
    }

    public int find(int x) {
        if (x < 0 || x >= parent.length) throw new IllegalArgumentException("Invalid element: " + x);
        metrics.findCalls++;
        while (x != parent[x]) x = parent[x];
        return x;
    }

    public boolean union(int x, int y) {
        int a = find(x);
        int b = find(y);
        if (a == b) return false;
        if (size[a] < size[b]) {
            int temporary = a;
            a = b;
            b = temporary;
        }
        parent[b] = a;
        size[a] += size[b];
        components--;
        metrics.successfulUnions++;
        return true;
    }

    public int components() { return components; }
}
