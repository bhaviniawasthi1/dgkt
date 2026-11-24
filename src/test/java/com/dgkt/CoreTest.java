package com.dgkt;

import com.dgkt.core.*;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CoreTest {
    private ConceptGraph graph() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/data/graph.json")) {
            return ConceptGraph.load(in);
        }
    }

    @Test
    void bayesStep() {
        assertEquals(0.6067, Bkt.posterior(0.30, true), 1e-3);
        assertEquals(0.0541, Bkt.posterior(0.30, false), 1e-3);
    }

    @Test
    void noLearningWithoutFeedback() {
        assertEquals(Bkt.posterior(0.30, true), Bkt.update(0.30, true, 0.0), 1e-12);
    }

    @Test
    void graphIsValid() throws Exception {
        ConceptGraph g = graph();
        assertEquals(18, g.ids().size());
        assertTrue(g.ancestors("dp").containsAll(Set.of("recursion", "memoization", "hashing", "arrays")));
        assertTrue(g.descendants("recursion").contains("shortest_paths"));
    }

    @Test
    void rootCauseIsRecursion() throws Exception {
        ConceptGraph g = graph();
        StudentModel m = new StudentModel(g);
        set(m, "dp", 0.02, 2);
        set(m, "recursion", 0.03, 4);
        set(m, "memoization", 0.02, 4);
        set(m, "hashing", 0.95, 4);
        set(m, "arrays", 0.95, 4);
        assertTrue(RootCauseAnalyzer.nextRound(g, m, "dp").isEmpty());
        assertEquals(Set.of("recursion"), RootCauseAnalyzer.rootCauses(g, m, "dp"));
    }

    @Test
    void asksOnlyUnverifiedPrerequisites() throws Exception {
        ConceptGraph g = graph();
        StudentModel m = new StudentModel(g);
        set(m, "dp", 0.02, 2);
        List<Need> round = RootCauseAnalyzer.nextRound(g, m, "dp");
        assertEquals(2, round.size()); // recursion and memoization, 3 questions each
        assertEquals(3, round.get(0).count());
    }

    private static void set(StudentModel m, String t, double p, int n) {
        m.state(t).p = p;
        m.state(t).n = n;
    }
}
