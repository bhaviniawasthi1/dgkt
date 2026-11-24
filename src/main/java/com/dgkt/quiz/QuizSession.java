package com.dgkt.quiz;

import com.dgkt.core.ConceptGraph;
import com.dgkt.core.StudentModel;

import java.util.*;

/** One student's assessment: exam first, then follow-up rounds. */
public final class QuizSession {
    final String id = UUID.randomUUID().toString();
    final StudentModel model;
    final List<String> examTopics;
    final Deque<Question> queue = new ArrayDeque<>();
    final Set<String> usedIds = new HashSet<>();
    final Set<String> failing = new LinkedHashSet<>();
    final List<Event> events = new ArrayList<>();
    Phase phase = Phase.EXAM;
    Question current;
    int examAnswered = 0, followUpAnswered = 0, round = 0;

    QuizSession(ConceptGraph g, List<String> examTopics) {
        this.model = new StudentModel(g);
        this.examTopics = examTopics;
    }

    public String id() { return id; }
    public Phase phase() { return phase; }
}
