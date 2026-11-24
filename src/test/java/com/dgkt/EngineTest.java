package com.dgkt;

import com.dgkt.core.ConceptGraph;
import com.dgkt.quiz.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EngineTest {
    private QuizEngine engine() throws Exception {
        ConceptGraph g = ConceptGraph.load(getClass().getResourceAsStream("/data/graph.json"));
        QuestionBank b = QuestionBank.load(getClass().getResourceAsStream("/data/questions.json"));
        return new QuizEngine(g, b, null);
    }

    @Test
    void everyTopicHasEightQuestions() throws Exception {
        ConceptGraph g = ConceptGraph.load(getClass().getResourceAsStream("/data/graph.json"));
        QuestionBank b = QuestionBank.load(getClass().getResourceAsStream("/data/questions.json"));
        for (String t : g.ids()) assertEquals(8, b.forTopic(t).size(), t);
    }

    @Test
    void allCorrectStudentHasNoWeakTopic() throws Exception {
        QuizEngine e = engine();
        QuizSession s = e.start(List.of("dp"), null);
        Question q;
        int n = 0;
        while ((q = e.next(s.id())) != null) { e.answer(s.id(), q.id(), q.correctIndex()); n++; }
        assertEquals(4, n);
        assertTrue(((List<?>) e.result(s.id()).get("diagnosis")).isEmpty());
    }

    @Test
    void allWrongInDpAndRecursionPointsToRecursion() throws Exception {
        QuizEngine e = engine();
        QuizSession s = e.start(List.of("dp"), null);
        Set<String> seen = new HashSet<>();
        Question q;
        while ((q = e.next(s.id())) != null) {
            assertTrue(seen.add(q.id()), "question repeated");
            boolean weakTopic = q.topic().equals("dp") || q.topic().equals("recursion") || q.topic().equals("memoization");
            e.answer(s.id(), q.id(), weakTopic ? (q.correctIndex() + 1) % 4 : q.correctIndex());
        }
        List<?> diag = (List<?>) e.result(s.id()).get("diagnosis");
        assertEquals(1, diag.size());
        assertEquals(List.of("Recursion"), ((Map<?, ?>) diag.get(0)).get("roots"));
    }
}
