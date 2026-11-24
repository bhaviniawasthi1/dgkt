package com.dgkt.quiz;

import com.dgkt.core.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;

/**
 * Runs the assessment mode of DGKT:
 *  1. Exam: fixed number of questions on the topics the student chose. No feedback.
 *  2. Follow-up: rounds of extra questions on prerequisites, chosen by RootCauseAnalyzer.
 *  3. Result: root cause for every failing topic.
 * Practice mode (with feedback) is not part of this class.
 */
public final class QuizEngine {
    public static final int EXAM_QUESTIONS_PER_TOPIC = 4; // default

    private final ConceptGraph graph;
    private final QuestionBank bank;
    private final Path logFile; // one JSON line per answer, may be null
    private final Map<String, QuizSession> sessions = new HashMap<>();
    private final int examPerTopic;
    private final Random rnd;

    public QuizEngine(ConceptGraph graph, QuestionBank bank, Path logFile) {
        this(graph, bank, logFile, EXAM_QUESTIONS_PER_TOPIC, new Random());
    }

    /** Longer form: used by the case study to change the exam size and to fix the random seed. */
    public QuizEngine(ConceptGraph graph, QuestionBank bank, Path logFile, int examPerTopic, Random rnd) {
        this.graph = graph;
        this.bank = bank;
        this.logFile = logFile;
        this.examPerTopic = examPerTopic;
        this.rnd = rnd;
    }

    /** Forget all running sessions and empty the answer log. Used when the owner clears the demo data. */
    public synchronized void clearAll() {
        sessions.clear();
        if (logFile == null) return;
        try { Files.deleteIfExists(logFile); } catch (IOException e) { /* the log is only a copy, ignore */ }
    }

    public synchronized QuizSession start(List<String> topics, Map<String, Integer> selfRatings) {
        if (topics == null || topics.isEmpty()) throw new IllegalArgumentException("Choose at least one topic");
        for (String t : topics)
            if (!graph.ids().contains(t)) throw new IllegalArgumentException("Unknown topic " + t);
        QuizSession s = new QuizSession(graph, new ArrayList<>(new LinkedHashSet<>(topics)));
        if (selfRatings != null)
            selfRatings.forEach((t, r) -> {
                if (graph.ids().contains(t) && r != null && r >= 1 && r <= 10) s.model.setSelfRating(t, r);
            });
        List<Question> exam = new ArrayList<>();
        for (String t : s.examTopics) {
            List<Question> pool = new ArrayList<>(bank.forTopic(t));
            Collections.shuffle(pool, rnd);
            for (int i = 0; i < Math.min(examPerTopic, pool.size()); i++) exam.add(pool.get(i));
        }
        Collections.shuffle(exam, rnd);
        s.queue.addAll(exam);
        sessions.put(s.id, s);
        return s;
    }

    public synchronized QuizSession session(String id) {
        QuizSession s = sessions.get(id);
        if (s == null) throw new NoSuchElementException("Unknown session");
        return s;
    }

    /** The question to show now, or null when the assessment is finished. */
    public synchronized Question next(String sessionId) {
        QuizSession s = session(sessionId);
        if (s.current != null) return s.current;
        while (s.current == null && s.phase != Phase.DONE) {
            if (s.queue.isEmpty()) advance(s);
            else s.current = s.queue.poll();
        }
        return s.current;
    }

    /** Saves the answer. No feedback is given (no learning, P(T) = 0). */
    public synchronized void answer(String sessionId, String questionId, int choice) {
        QuizSession s = session(sessionId);
        if (s.current == null || !s.current.id().equals(questionId))
            throw new IllegalArgumentException("This is not the current question");
        Question q = s.current;
        boolean correct = choice == q.correctIndex();
        double before = s.model.state(q.topic()).p;
        s.model.observe(q.topic(), correct, false);
        TopicState after = s.model.state(q.topic());
        s.events.add(new Event(s.events.size() + 1, s.phase.name(), s.round, q.topic(), correct, before, after.p, after.n));
        s.usedIds.add(q.id());
        if (s.phase == Phase.EXAM) s.examAnswered++; else s.followUpAnswered++;
        s.current = null;
        log(s, q, choice, correct);
    }

    /** Called when the queue is empty: move to the next phase or the next round. */
    private void advance(QuizSession s) {
        if (s.phase == Phase.EXAM) {
            for (String t : s.examTopics) if (s.model.isFailing(t)) s.failing.add(t);
            s.phase = Phase.FOLLOW_UP;
        }
        if (s.failing.isEmpty() || s.round >= 20) { s.phase = Phase.DONE; return; }
        Map<String, Integer> need = new LinkedHashMap<>();
        for (String f : s.failing)
            for (Need n : RootCauseAnalyzer.nextRound(graph, s.model, f)) need.merge(n.topic(), n.count(), Math::max);
        List<Question> next = new ArrayList<>();
        for (Map.Entry<String, Integer> e : need.entrySet()) {
            List<Question> free = new ArrayList<>();
            for (Question q : bank.forTopic(e.getKey())) if (!s.usedIds.contains(q.id())) free.add(q);
            Collections.shuffle(free, rnd);
            for (int i = 0; i < Math.min(e.getValue(), free.size()); i++) next.add(free.get(i)); // pool empty -> skip
        }
        if (next.isEmpty()) { s.phase = Phase.DONE; return; }
        s.round++;
        s.queue.addAll(next);
    }

    /** Every answer of the session, in order, with the belief before and after. */
    public synchronized List<Event> events(String sessionId) { return List.copyOf(session(sessionId).events); }

    /** Failing topics of a finished session, each with its root causes (topic ids). */
    public synchronized Map<String, Set<String>> rootCauseIds(String sessionId) {
        QuizSession s = session(sessionId);
        Map<String, Set<String>> out = new LinkedHashMap<>();
        for (String f : s.failing) out.put(f, RootCauseAnalyzer.rootCauses(graph, s.model, f));
        return out;
    }

    /** Exam topics that were NOT marked failing in a finished session. */
    public synchronized Set<String> passedExamTopics(String sessionId) {
        QuizSession s = session(sessionId);
        Set<String> out = new LinkedHashSet<>(s.examTopics);
        out.removeAll(s.failing);
        return out;
    }

    /** Final report. Only call when the phase is DONE. */
    public synchronized Map<String, Object> result(String sessionId) {
        QuizSession s = session(sessionId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("phase", s.phase.name());
        out.put("examQuestions", s.examAnswered);
        out.put("followUpQuestions", s.followUpAnswered);
        out.put("rounds", s.round);

        List<Map<String, Object>> topics = new ArrayList<>();
        for (String t : graph.ids()) {
            TopicState st = s.model.state(t);
            if (st.n == 0) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t); m.put("name", graph.name(t)); m.put("p", Math.round(st.p * 1000) / 1000.0);
            m.put("n", st.n); m.put("status", s.model.status(t).name());
            topics.add(m);
        }
        out.put("topics", topics);

        List<Map<String, Object>> diag = new ArrayList<>();
        for (String f : s.failing) {
            Set<String> roots = RootCauseAnalyzer.rootCauses(graph, s.model, f);
            List<String> rootNames = new ArrayList<>();
            for (String r : roots) rootNames.add(graph.name(r));
            String joined = String.join(" and ", rootNames);
            boolean self = roots.size() == 1 && roots.contains(f);
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("failingId", f);
            d.put("failing", graph.name(f));
            d.put("rootIds", new ArrayList<>(roots));
            d.put("roots", rootNames);
            d.put("message", self
                ? graph.name(f) + " looks weak by itself. Its prerequisites look fine, so revise " + graph.name(f) + " directly."
                : "Your " + graph.name(f) + " answers are weak. The root cause looks like " + joined
                  + ", which " + graph.name(f) + " depends on. Revise " + joined + " first.");
            diag.add(d);
        }
        out.put("diagnosis", diag);
        out.put("summary", diag.isEmpty() ? "No weak topic found in this exam." : diag.size() + " failing topic(s) analysed.");
        return out;
    }

    private void log(QuizSession s, Question q, int choice, boolean correct) {
        if (logFile == null) return;
        try {
            Files.createDirectories(logFile.toAbsolutePath().getParent());
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(logFile,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
                w.printf("{\"session\":\"%s\",\"phase\":\"%s\",\"question\":\"%s\",\"topic\":\"%s\",\"choice\":%d,\"correct\":%b,\"time\":\"%s\"}%n",
                        s.id, s.phase, q.id(), q.topic(), choice, correct, java.time.Instant.now());
            }
        } catch (IOException e) { /* logging must never break the quiz */ }
    }
}
