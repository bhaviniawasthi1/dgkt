package com.dgkt.quiz;

import com.dgkt.core.ConceptGraph;
import com.dgkt.core.Status;
import com.dgkt.core.StudentModel;
import com.dgkt.core.TopicState;

import java.util.*;

/**
 * Practice mode (small version). The student picks ONE topic and gets one question
 * at a time. After every answer the correct option is shown, and BKT is updated with
 * learning switched on (P(T) > 0), because feedback can teach.
 * This is separate from the assessment mode, which gives no feedback.
 */
public final class PracticeService {
    /** What the student sees after one answer. */
    public record Feedback(boolean correct, int correctIndex, double pBefore, double pAfter,
                           int answered, String status, String message) {}

    public static final class PracticeSession {
        final String id = UUID.randomUUID().toString();
        final String topic;
        final StudentModel model;
        final Set<String> used = new HashSet<>();
        Question current;
        int answered = 0;

        PracticeSession(ConceptGraph g, String topic) {
            this.topic = topic;
            this.model = new StudentModel(g);
        }

        public String id() { return id; }
        public String topic() { return topic; }
        public int answered() { return answered; }
    }

    private final ConceptGraph graph;
    private final QuestionBank bank;
    private final Random rnd;
    private final Map<String, PracticeSession> sessions = new HashMap<>();

    public PracticeService(ConceptGraph graph, QuestionBank bank) {
        this(graph, bank, new Random());
    }

    public PracticeService(ConceptGraph graph, QuestionBank bank, Random rnd) {
        this.graph = graph;
        this.bank = bank;
        this.rnd = rnd;
    }

    /** Forget all running practice sessions (used when the owner clears the demo data). */
    public synchronized void clearAll() { sessions.clear(); }

    public synchronized PracticeSession start(String topic) {
        if (topic == null || !graph.ids().contains(topic))
            throw new IllegalArgumentException("Unknown topic");
        PracticeSession s = new PracticeSession(graph, topic);
        sessions.put(s.id, s);
        return s;
    }

    public synchronized PracticeSession session(String id) {
        PracticeSession s = sessions.get(id);
        if (s == null) throw new NoSuchElementException("Unknown practice session");
        return s;
    }

    /** Next question of the topic. Questions do not repeat until all of them were used once. */
    public synchronized Question next(String id) {
        PracticeSession s = session(id);
        if (s.current != null) return s.current;
        List<Question> free = new ArrayList<>();
        for (Question q : bank.forTopic(s.topic)) if (!s.used.contains(q.id())) free.add(q);
        if (free.isEmpty()) {
            s.used.clear();
            free.addAll(bank.forTopic(s.topic));
        }
        if (free.isEmpty()) return null;
        s.current = free.get(rnd.nextInt(free.size()));
        return s.current;
    }

    public synchronized Feedback answer(String id, String questionId, int choice) {
        PracticeSession s = session(id);
        if (s.current == null || !s.current.id().equals(questionId))
            throw new IllegalArgumentException("This is not the current question");
        Question q = s.current;
        boolean correct = choice == q.correctIndex();
        double before = s.model.state(s.topic).p;
        s.model.observe(s.topic, correct, true); // learning is ON in practice mode
        TopicState st = s.model.state(s.topic);
        s.used.add(q.id());
        s.answered++;
        s.current = null;
        Status status = s.model.status(s.topic);
        return new Feedback(correct, q.correctIndex(), before, st.p, s.answered, status.name(), message(status, st));
    }

    private static String message(Status status, TopicState st) {
        if (status == Status.UNVERIFIED) return "Keep going. At least 3 answers are needed to judge this topic.";
        if (status == Status.WEAK) return "Still weak. Read the correct answers and try again.";
        if (st.p >= 0.85) return "Looks strong. You can try another topic.";
        return "Getting better. A few more right answers will make it stronger.";
    }
}
