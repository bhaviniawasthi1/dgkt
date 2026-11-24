package com.dgkt.web;

import com.dgkt.core.ConceptGraph;
import com.dgkt.quiz.PracticeService;
import com.dgkt.quiz.Question;
import com.dgkt.store.Recorder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/** Practice mode: one topic, feedback after every answer. */
@RestController
@RequestMapping("/api/practice")
public class PracticeController {
    public record StartRequest(String topic, String studentCode, String nickname) {}
    public record AnswerRequest(String questionId, int choice) {}

    private final PracticeService practice;
    private final Recorder recorder;
    private final ConceptGraph graph;

    public PracticeController(PracticeService practice, Recorder recorder, ConceptGraph graph) {
        this.practice = practice;
        this.recorder = recorder;
        this.graph = graph;
    }

    @PostMapping
    public Map<String, Object> start(@RequestBody StartRequest req) {
        try {
            PracticeService.PracticeSession s = practice.start(req.topic());
            String code = recorder.ensureStudent(req.studentCode(), req.nickname());
            recorder.startSession(s.id(), code, "PRACTICE", List.of(req.topic()));
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("sessionId", s.id());
            out.put("topic", req.topic());
            out.put("topicName", graph.name(req.topic()));
            return out;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** The correct answer is not sent here. It comes back after the student answers. */
    @GetMapping("/{id}/next")
    public Map<String, Object> next(@PathVariable String id) {
        try {
            Question q = practice.next(id);
            if (q == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No questions for this topic");
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("questionId", q.id());
            out.put("text", q.text());
            out.put("options", q.options());
            return out;
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/{id}/answer")
    public PracticeService.Feedback answer(@PathVariable String id, @RequestBody AnswerRequest req) {
        try {
            String topic = practice.session(id).topic();
            PracticeService.Feedback f = practice.answer(id, req.questionId(), req.choice());
            recorder.attempt(id, f.answered(), "PRACTICE", 0, topic, req.questionId(), req.choice(),
                    f.correct(), f.pBefore(), f.pAfter(), f.answered());
            return f;
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
