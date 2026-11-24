package com.dgkt.web;

import com.dgkt.quiz.Event;
import com.dgkt.quiz.Phase;
import com.dgkt.quiz.Question;
import com.dgkt.quiz.QuizEngine;
import com.dgkt.quiz.QuizSession;
import com.dgkt.store.Recorder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/sessions")
public class QuizController {
    public record StartRequest(List<String> topics, Map<String, Integer> ratings, String studentCode, String nickname) {}
    public record AnswerRequest(String questionId, int choice) {}

    private final QuizEngine engine;
    private final Recorder recorder;

    public QuizController(QuizEngine engine, Recorder recorder) {
        this.engine = engine;
        this.recorder = recorder;
    }

    @PostMapping
    public Map<String, Object> start(@RequestBody StartRequest req) {
        try {
            QuizSession s = engine.start(req.topics(), req.ratings());
            String code = recorder.ensureStudent(req.studentCode(), req.nickname());
            recorder.startSession(s.id(), code, "ASSESSMENT", req.topics());
            return Map.of("sessionId", s.id());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** The correct answer is never sent to the browser. */
    @GetMapping("/{id}/next")
    public Map<String, Object> next(@PathVariable String id) {
        try {
            Question q = engine.next(id);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("phase", engine.session(id).phase().name());
            if (q == null) { out.put("done", true); return out; }
            out.put("done", false);
            out.put("questionId", q.id());
            out.put("text", q.text());
            out.put("options", q.options());
            return out;
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/{id}/answer")
    public Map<String, Object> answer(@PathVariable String id, @RequestBody AnswerRequest req) {
        try {
            engine.answer(id, req.questionId(), req.choice());
            List<Event> events = engine.events(id);
            recorder.attempt(id, events.get(events.size() - 1), req.questionId(), req.choice());
            return Map.of("saved", true);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/{id}/result")
    public Map<String, Object> result(@PathVariable String id) {
        try {
            if (engine.session(id).phase() != Phase.DONE) engine.next(id); // may finish the assessment
            if (engine.session(id).phase() != Phase.DONE)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Assessment not finished");
            Map<String, Object> result = engine.result(id);
            recorder.finish(id, result);
            return result;
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
