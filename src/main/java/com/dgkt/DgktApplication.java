package com.dgkt;

import com.dgkt.core.ConceptGraph;
import com.dgkt.quiz.PracticeService;
import com.dgkt.quiz.QuestionBank;
import com.dgkt.quiz.QuizEngine;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

@SpringBootApplication
public class DgktApplication {
    public static void main(String[] args) {
        SpringApplication.run(DgktApplication.class, args);
    }

    /** The dependency graph is loaded once at start-up. */
    @Bean
    public ConceptGraph conceptGraph() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/data/graph.json")) {
            return ConceptGraph.load(in);
        }
    }

    @Bean
    public QuestionBank questionBank() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/data/questions.json")) {
            return QuestionBank.load(in);
        }
    }

    /** Every answer is also appended to data/attempts.jsonl (kept for the report). */
    @Bean
    public QuizEngine quizEngine(ConceptGraph graph, QuestionBank bank) {
        return new QuizEngine(graph, bank, Path.of("data", "attempts.jsonl"));
    }

    /** Practice mode (with feedback), kept in memory. */
    @Bean
    public PracticeService practiceService(ConceptGraph graph, QuestionBank bank) {
        return new PracticeService(graph, bank);
    }
}
