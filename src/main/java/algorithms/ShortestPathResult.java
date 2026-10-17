package algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ShortestPathResult(long[] distance, int[] parent) {
    public List<Integer> pathTo(int vertex) {
        if (vertex < 0 || vertex >= distance.length) throw new IllegalArgumentException("Invalid target");
        if (distance[vertex] == Long.MAX_VALUE) return List.of();
        List<Integer> path = new ArrayList<>();
        for (int v = vertex; v != -1; v = parent[v]) path.add(v);
        Collections.reverse(path);
        return path;
    }

    public long distanceSum() {
        long sum = 0;
        for (long value : distance) {
            if (value == Long.MAX_VALUE) throw new IllegalStateException("Unreachable DAG vertex");
            sum += value;
        }
        return sum;
    }
}
