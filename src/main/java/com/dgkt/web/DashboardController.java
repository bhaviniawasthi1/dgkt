package com.dgkt.web;

import com.dgkt.core.ConceptGraph;
import com.dgkt.core.Thresholds;
import com.dgkt.store.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/** Data for the dashboard page: one student's history, and simple totals for the whole platform. */
@RestController
@RequestMapping("/api")
public class DashboardController {
    private final ConceptGraph graph;
    private final StudentRepo students;
    private final SessionRepo sessions;
    private final AttemptRepo attempts;
    private final DiagnosisRepo diagnoses;
    private final ObjectMapper mapper;

    public DashboardController(ConceptGraph graph, StudentRepo students, SessionRepo sessions,
                               AttemptRepo attempts, DiagnosisRepo diagnoses, ObjectMapper mapper) {
        this.graph = graph;
        this.students = students;
        this.sessions = sessions;
        this.attempts = attempts;
        this.diagnoses = diagnoses;
        this.mapper = mapper;
    }

    @GetMapping("/students/{code}/dashboard")
    public Map<String, Object> dashboard(@PathVariable String code) {
        String c = Recorder.cleanCode(code);
        List<Attempt> all = attempts.findByStudentCodeOrderByAnsweredAtAsc(c);

        // Latest belief per topic, taken from assessment answers only (practice starts from a fresh model).
        Map<String, Attempt> latest = new HashMap<>();
        Map<String, Integer> perSession = new HashMap<>();
        int right = 0, exam = 0, practiceN = 0;
        for (Attempt a : all) {
            perSession.merge(a.sessionId, 1, Integer::sum);
            if ("PRACTICE".equals(a.phase)) { practiceN++; continue; }
            exam++;
            if (a.correct) right++;
            latest.put(a.topicId, a);
        }
        List<Map<String, Object>> mastery = new ArrayList<>();
        for (String id : graph.ids()) {
            Attempt a = latest.get(id);
            if (a == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("name", graph.name(id));
            m.put("p", Math.round(a.pAfter * 1000) / 1000.0);
            m.put("n", a.nAfter);
            String status = a.nAfter < Thresholds.N_MIN ? "UNVERIFIED" : (a.pAfter >= Thresholds.THETA ? "OK" : "WEAK");
            m.put("status", status);
            mastery.add(m);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (SessionRecord r : sessions.findByStudentCodeOrderByStartedAtDesc(c)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.id);
            m.put("mode", r.mode);
            m.put("startedAt", String.valueOf(r.startedAt));
            m.put("finished", r.finishedAt != null);
            m.put("answered", perSession.getOrDefault(r.id, 0));
            List<String> names = new ArrayList<>();
            if (r.topics != null && !r.topics.isBlank())
                for (String t : r.topics.split(",")) names.add(graph.ids().contains(t) ? graph.name(t) : t);
            m.put("topics", names);
            if (r.resultJson != null) {
                try {
                    JsonNode node = mapper.readTree(r.resultJson);
                    m.put("diagnosis", node.get("diagnosis"));
                    m.put("summary", node.get("summary"));
                } catch (Exception e) { /* old or broken row: just skip the details */ }
            }
            list.add(m);
        }

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("sessions", list.size());
        // Assessment answers only, so the numbers match the mastery bars. Practice is counted on its own.
        totals.put("answers", exam);
        totals.put("accuracy", exam == 0 ? 0.0 : Math.round(1000.0 * right / exam) / 1000.0);
        totals.put("practiceAnswers", practiceN);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("code", c);
        out.put("nickname", students.findById(c).map(s -> s.nickname).orElse(null));
        out.put("totals", totals);
        out.put("mastery", mastery);
        out.put("sessions", list);
        return out;
    }

    /** Totals for the whole platform. No student can be identified from this. */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        List<Map<String, Object>> top = new ArrayList<>();
        for (Object[] row : diagnoses.rootCauseCounts()) {
            String id = String.valueOf(row[0]);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("topic", graph.ids().contains(id) ? graph.name(id) : id);
            m.put("count", ((Number) row[1]).longValue());
            top.add(m);
            if (top.size() == 5) break;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("students", students.count());
        out.put("sessions", sessions.count());
        out.put("answers", attempts.count());
        out.put("topRootCauses", top);
        return out;
    }
}
