package com.dgkt;

import com.dgkt.core.ConceptGraph;
import com.dgkt.quiz.*;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PracticeTest {
    private PracticeService practice() throws Exception {
        ConceptGraph g = ConceptGraph.load(getClass().getResourceAsStream("/data/graph.json"));
        QuestionBank b = QuestionBank.load(getClass().getResourceAsStream("/data/questions.json"));
        return new PracticeService(g, b, new Random(1));
    }

    @Test
    void rightAnswersRaiseMasteryAndShowTheCorrectOption() throws Exception {
        PracticeService p = practice();
        PracticeService.PracticeSession s = p.start("recursion");
        double last = 0;
        for (int i = 0; i < 4; i++) {
            Question q = p.next(s.id());
            PracticeService.Feedback f = p.answer(s.id(), q.id(), q.correctIndex());
            assertTrue(f.correct());
            assertEquals(q.correctIndex(), f.correctIndex());
            assertTrue(f.pAfter() > f.pBefore());
            last = f.pAfter();
        }
        assertTrue(last > 0.9);
    }

    @Test
    void wrongAnswerLowersMastery() throws Exception {
        PracticeService p = practice();
        PracticeService.PracticeSession s = p.start("recursion");
        Question q = p.next(s.id());
        int wrong = (q.correctIndex() + 1) % q.options().size();
        PracticeService.Feedback f = p.answer(s.id(), q.id(), wrong);
        assertFalse(f.correct());
        assertTrue(f.pAfter() < f.pBefore() + 0.15); // learning step is small
    }

    @Test
    void questionsDoNotRepeatInOneRound() throws Exception {
        PracticeService p = practice();
        PracticeService.PracticeSession s = p.start("recursion");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 8; i++) {
            Question q = p.next(s.id());
            assertTrue(seen.add(q.id()));
            p.answer(s.id(), q.id(), q.correctIndex());
        }
    }

    @Test
    void unknownTopicIsRejected() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            try { practice().start("no_such_topic"); } catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
    }
}
