package com.dgkt.sim;

import com.dgkt.core.Bkt;
import com.dgkt.core.ConceptGraph;

import java.util.*;

/**
 * A made-up student for the case study.
 * Simple rule: the student really does not know the "planted" topics, and also
 * not any topic that needs a planted topic (the weakness flows downwards).
 * Knows -> correct with chance 1 - slip. Does not know -> correct with chance guess.
 */
public final class SimulatedStudent {
    private final Set<String> unknown = new LinkedHashSet<>();
    private final Set<String> planted;
    private final Random rnd;
    private final double guess, slip;

    public SimulatedStudent(ConceptGraph g, Set<String> planted, long seed) {
        this(g, planted, seed, Bkt.P_G, Bkt.P_S);
    }

    /** The student's real guess/slip chances can differ from the values the model assumes. */
    public SimulatedStudent(ConceptGraph g, Set<String> planted, long seed, double guess, double slip) {
        this.guess = guess;
        this.slip = slip;
        this.planted = new LinkedHashSet<>(planted);
        this.rnd = new Random(seed);
        for (String t : planted) {
            unknown.add(t);
            unknown.addAll(g.descendants(t));
        }
    }

    public boolean answer(String topic) {
        double pCorrect = unknown.contains(topic) ? guess : 1 - slip;
        return rnd.nextDouble() < pCorrect;
    }

    public boolean reallyKnows(String topic) { return !unknown.contains(topic); }
    public Set<String> planted() { return planted; }

    /** True root causes for a topic: planted topics at or below it with no planted prerequisite. */
    public Set<String> trueRoots(ConceptGraph g, String topic) {
        Set<String> chain = new LinkedHashSet<>(g.ancestors(topic));
        chain.add(topic);
        Set<String> out = new LinkedHashSet<>();
        for (String x : planted) {
            if (!chain.contains(x)) continue;
            boolean plantedAbove = false;
            for (String a : g.ancestors(x)) if (planted.contains(a)) plantedAbove = true;
            if (!plantedAbove) out.add(x);
        }
        return out;
    }
}
