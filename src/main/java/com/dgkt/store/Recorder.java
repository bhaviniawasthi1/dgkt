package com.dgkt.store;

import com.dgkt.quiz.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Saves what happens in a quiz to the database.
 * Saving must never break the quiz, so every method catches its own errors.
 */
@Service
public class Recorder {
    private final StudentRepo students;
    private final SessionRepo sessions;
    private final AttemptRepo attempts;
    private final DiagnosisRepo diagnoses;
    private final ObjectMapper mapper;

    public Recorder(StudentRepo students, SessionRepo sessions, AttemptRepo attempts,
                    DiagnosisRepo diagnoses, ObjectMapper mapper) {
        this.students = students;
        this.sessions = sessions;
        this.attempts = attempts;
        this.diagnoses = diagnoses;
        this.mapper = mapper;
    }

    /** Only letters, digits, - and _ , at most 64 characters. Empty becomes "anonymous". */
    public static String cleanCode(String code) {
        if (code == null) return "anonymous";
        String c = code.replaceAll("[^A-Za-z0-9_-]", "");
        if (c.length() > 64) c = c.substring(0, 64);
        return c.isEmpty() ? "anonymous" : c;
    }

    /** Creates the student if new. Returns the clean code. */
    public String ensureStudent(String code, String nickname) {
        String c = cleanCode(code);
        try {
            Student s = students.findById(c).orElse(null);
            String nick = nickname == null ? null : nickname.trim();
            if (nick != null && nick.length() > 40) nick = nick.substring(0, 40);
            if (s == null) {
                s = new Student();
                s.code = c;
                s.createdAt = Instant.now();
                s.nickname = nick;
                students.save(s);
            } else if (nick != null && !nick.isEmpty() && !nick.equals(s.nickname)) {
                s.nickname = nick;
                students.save(s);
            }
        } catch (Exception e) {
            System.err.println("Recorder.ensureStudent failed: " + e.getMessage());
        }
        return c;
    }

    public void startSession(String sessionId, String studentCode, String mode, Collection<String> topics) {
        try {
            SessionRecord r = new SessionRecord();
            r.id = sessionId;
            r.studentCode = studentCode;
            r.mode = mode;
            r.topics = topics == null ? "" : String.join(",", topics);
            if (r.topics.length() > 1000) r.topics = r.topics.substring(0, 1000);
            r.startedAt = Instant.now();
            sessions.save(r);
        } catch (Exception e) {
            System.err.println("Recorder.startSession failed: " + e.getMessage());
        }
    }

    /** One answer of an assessment (exam or follow-up). */
    public void attempt(String sessionId, Event e, String questionId, int choice) {
        attempt(sessionId, e.seq(), e.phase(), e.round(), e.topic(), questionId, choice,
                e.correct(), e.pBefore(), e.pAfter(), e.nAfter());
    }

    /** One answer, all fields given. Used by practice mode too. */
    public void attempt(String sessionId, int seq, String phase, int roundNo, String topicId, String questionId,
                        int choice, boolean correct, double pBefore, double pAfter, int nAfter) {
        try {
            SessionRecord s = sessions.findById(sessionId).orElse(null);
            Attempt a = new Attempt();
            a.sessionId = sessionId;
            a.studentCode = s == null ? "anonymous" : s.studentCode;
            a.seq = seq;
            a.phase = phase;
            a.roundNo = roundNo;
            a.topicId = topicId;
            a.questionId = questionId;
            a.choice = choice;
            a.correct = correct;
            a.pBefore = pBefore;
            a.pAfter = pAfter;
            a.nAfter = nAfter;
            a.answeredAt = Instant.now();
            attempts.save(a);
        } catch (Exception ex) {
            System.err.println("Recorder.attempt failed: " + ex.getMessage());
        }
    }

    /** Saves the final result of an assessment once. Safe to call again. */
    @SuppressWarnings("unchecked")
    public void finish(String sessionId, Map<String, Object> result) {
        try {
            SessionRecord r = sessions.findById(sessionId).orElse(null);
            if (r == null || r.finishedAt != null) return;
            r.finishedAt = Instant.now();
            r.examQuestions = ((Number) result.get("examQuestions")).intValue();
            r.followUpQuestions = ((Number) result.get("followUpQuestions")).intValue();
            r.roundsUsed = ((Number) result.get("rounds")).intValue();
            r.resultJson = mapper.writeValueAsString(result);
            sessions.save(r);
            List<Map<String, Object>> diag = (List<Map<String, Object>>) result.get("diagnosis");
            if (diag != null) {
                for (Map<String, Object> d : diag) {
                    String failing = String.valueOf(d.get("failingId"));
                    List<String> roots = (List<String>) d.get("rootIds");
                    if (roots == null) continue;
                    for (String root : roots) {
                        Diagnosis row = new Diagnosis();
                        row.sessionId = sessionId;
                        row.failingTopic = failing;
                        row.rootTopic = root;
                        diagnoses.save(row);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Recorder.finish failed: " + e.getMessage());
        }
    }
}
