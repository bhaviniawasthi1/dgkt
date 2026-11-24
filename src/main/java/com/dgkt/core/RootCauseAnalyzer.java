package com.dgkt.core;

import java.util.*;

/**
 * Root-cause logic. The graph only decides WHICH topics to ask about.
 * A wrong answer still updates only its own topic.
 */
public final class RootCauseAnalyzer {
    private RootCauseAnalyzer() {}

    /** True if we still need answers on this topic before we can trust it. */
    private static int missing(StudentModel m, String t) {
        TopicState s = m.state(t);
        if (s.n < Thresholds.N_MIN) return Thresholds.N_MIN - s.n;
        boolean close = Math.abs(s.p - Thresholds.THETA) < Thresholds.DELTA;
        if (close && s.n < Thresholds.CAP) return 1;
        return 0;
    }

    /**
     * One round of the follow-up quiz for a failing topic.
     * Start at the failing topic, look at its direct prerequisites:
     *  - not enough answers (or too close to THETA): ask more questions there;
     *  - weak: go one level deeper;
     *  - OK: stop on this branch.
     * Empty list = quiz is finished for this failing topic.
     */
    public static List<Need> nextRound(ConceptGraph g, StudentModel m, String failing) {
        List<Need> needs = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(failing);
        seen.add(failing);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            for (String q : g.prerequisites(cur)) {
                if (!seen.add(q)) continue;
                int miss = missing(m, q);
                if (miss > 0) needs.add(new Need(q, miss));
                else if (m.status(q) == Status.WEAK) queue.add(q); // go deeper only below weak topics
            }
        }
        return needs;
    }

    /**
     * Root cause = a weak topic that has no weak prerequisite.
     * We only walk through weak topics, starting from the failing one.
     * If no prerequisite is weak, the failing topic itself is the root cause.
     */
    public static Set<String> rootCauses(ConceptGraph g, StudentModel m, String failing) {
        Set<String> weak = new LinkedHashSet<>();
        Deque<String> st = new ArrayDeque<>();
        weak.add(failing);
        st.push(failing);
        while (!st.isEmpty()) {
            for (String q : g.prerequisites(st.pop()))
                if (m.status(q) == Status.WEAK && weak.add(q)) st.push(q);
        }
        Set<String> roots = new LinkedHashSet<>();
        for (String w : weak) {
            boolean hasWeakPrereq = false;
            for (String q : g.prerequisites(w)) if (weak.contains(q)) hasWeakPrereq = true;
            if (!hasWeakPrereq) roots.add(w);
        }
        return roots;
    }
}
