package tests;

import algorithms.*;
import benchmark.Benchmark;
import benchmark.GraphGenerator;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkTest {
    @Test
    void reproducesSmallestSparseAndDenseReferenceValues() {
        int[] counts = {4000, 20000};
        long[] weights = {147970, 30363};
        int[] components = {27, 1};
        long[] distances = {396274, 252411};
        for (int i = 0; i < counts.length; i++) {
            var undirected = GraphGenerator.generate("undirected", 1000, counts[i], 42);
            assertEquals(weights[i], Kruskal.run(undirected, new Metrics()).totalWeight());
            assertEquals(weights[i], Prim.run(undirected, new Metrics()).totalWeight());
            assertEquals(components[i], Kosaraju.run(GraphGenerator.generate("directed", 1000, counts[i], 42), new Metrics()).count());
            assertEquals(distances[i], DagShortestPath.run(GraphGenerator.generate("dag", 1000, counts[i], 42), new Metrics()).distanceSum());
        }
    }

    @Test
    void customSizesProduceFiveRowsPerCombination(@TempDir Path directory) throws Exception {
        Path output = directory.resolve("custom.csv");
        Benchmark.run(new int[]{20, 50}, new int[]{4, 20}, 3, output, false);
        var rows = Files.readAllLines(output);
        assertEquals(21, rows.size());
        assertEquals("algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result", rows.get(0));
        assertTrue(rows.stream().skip(1).allMatch(row -> row.split(",").length == 10));
        assertThrows(IllegalArgumentException.class,
                () -> Benchmark.run(new int[]{20}, new int[]{4}, 2, output, false));
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.generate("dag", 1, 1, 42));
    }
}
