package com.dgkt.core;

import java.util.*;

/** BKT state of one student for every topic of a subject. */
public final class StudentModel {
    private final Map<String, TopicState> states = new LinkedHashMap<>();

    public StudentModel(ConceptGraph g) {
        for (String id : g.ids()) states.put(id, new TopicState());
    }

    /** Optional start value from a self-rating 1..10. It is NOT evidence, so n stays 0. */
    public void setSelfRating(String topic, int rating) {
        states.get(topic).p = Math.max(0.1, Math.min(0.9, rating / 10.0));
    }

    /** @param learning true in practice mode (feedback given), false in exam/follow-up. */
    public void observe(String topic, boolean correct, boolean learning) {
        TopicState s = states.get(topic);
        s.p = Bkt.update(s.p, correct, learning ? Bkt.P_T : 0.0);
        s.n++;
    }

    public TopicState state(String topic) { return states.get(topic); }

    public Status status(String topic) {
        TopicState s = states.get(topic);
        if (s.n < Thresholds.N_MIN) return Status.UNVERIFIED;
        return s.p >= Thresholds.THETA ? Status.OK : Status.WEAK;
    }

    /** Exam rule: a topic fails if it has at least 2 answers and p is below THETA. */
    public boolean isFailing(String topic) {
        TopicState s = states.get(topic);
        return s.n >= Thresholds.FAIL_N_MIN && s.p < Thresholds.THETA;
    }
}
