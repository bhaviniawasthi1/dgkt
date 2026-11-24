package com.dgkt.web;

import com.dgkt.quiz.PracticeService;
import com.dgkt.quiz.QuizEngine;
import com.dgkt.store.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Lets the owner wipe all saved data so the next tester starts with a fresh site.
 * If the RESET_PIN setting is not empty, the caller must send the same PIN.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    public record ResetRequest(String pin) {}

    private final StudentRepo students;
    private final SessionRepo sessions;
    private final AttemptRepo attempts;
    private final DiagnosisRepo diagnoses;
    private final QuizEngine engine;
    private final PracticeService practice;
    private final String pin;

    public AdminController(StudentRepo students, SessionRepo sessions, AttemptRepo attempts,
                           DiagnosisRepo diagnoses, QuizEngine engine, PracticeService practice,
                           @Value("${reset.pin:}") String pin) {
        this.students = students;
        this.sessions = sessions;
        this.attempts = attempts;
        this.diagnoses = diagnoses;
        this.engine = engine;
        this.practice = practice;
        this.pin = pin == null ? "" : pin.trim();
    }

    /** Tells the page whether to ask for a PIN. */
    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of("pinRequired", !pin.isEmpty());
    }

    @PostMapping("/reset")
    public Map<String, Object> reset(@RequestBody(required = false) ResetRequest req) {
        if (!pin.isEmpty() && (req == null || !pin.equals(req.pin() == null ? "" : req.pin().trim())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong PIN");
        long a = attempts.count(), s = sessions.count(), st = students.count();
        diagnoses.deleteAllInBatch();
        attempts.deleteAllInBatch();
        sessions.deleteAllInBatch();
        students.deleteAllInBatch();
        engine.clearAll();
        practice.clearAll();
        return Map.of("cleared", true, "students", st, "sessions", s, "answers", a);
    }
}
