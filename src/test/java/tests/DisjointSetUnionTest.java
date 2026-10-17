package tests;

import algorithms.DisjointSetUnion;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DisjointSetUnionTest {
    @Test
    void randomUnionsAgreeWithSimpleLabels() {
        int n = 80;
        int[] labels = new int[n];
        for (int i = 0; i < n; i++) labels[i] = i;
        DisjointSetUnion dsu = new DisjointSetUnion(n, new Metrics());
        Random random = new Random(15);
        for (int step = 0; step < 1000; step++) {
            int a = random.nextInt(n);
            int b = random.nextInt(n);
            boolean different = labels[a] != labels[b];
            assertEquals(different, dsu.union(a, b));
            int old = labels[b];
            int replacement = labels[a];
            for (int i = 0; i < n; i++) if (labels[i] == old) labels[i] = replacement;
            assertEquals(Arrays.stream(labels).distinct().count(), dsu.components());
            for (int i = 0; i < n; i++) {
                assertEquals(labels[a] == labels[i], dsu.find(a) == dsu.find(i));
            }
        }
    }

    @Test
    void countsFindCallsAndOnlySuccessfulUnions() {
        Metrics metrics = new Metrics();
        DisjointSetUnion dsu = new DisjointSetUnion(3, metrics);
        assertTrue(dsu.union(0, 1));
        assertFalse(dsu.union(1, 0));
        dsu.find(1);
        assertEquals(5, metrics.findCalls);
        assertEquals(1, metrics.successfulUnions);
        assertEquals(6, metrics.dsuOps());
        dsu.makeSet(0);
        assertEquals(0, dsu.components());
        assertThrows(IllegalArgumentException.class, () -> dsu.find(0));
    }
}
