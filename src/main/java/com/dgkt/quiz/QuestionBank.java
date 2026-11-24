package com.dgkt.quiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/** All questions, each tagged to one topic. */
public final class QuestionBank {
    private final Map<String, List<Question>> byTopic = new LinkedHashMap<>();
    private final Map<String, Question> byId = new HashMap<>();

    public static QuestionBank load(InputStream in) throws IOException {
        QuestionBank b = new QuestionBank();
        for (JsonNode n : new ObjectMapper().readTree(in)) {
            List<String> opts = new ArrayList<>();
            n.get("options").forEach(o -> opts.add(o.asText()));
            Question q = new Question(n.get("id").asText(), n.get("concept_id").asText(), n.get("text").asText(),
                    opts, n.get("correct_index").asInt(), n.get("difficulty").asText());
            b.byTopic.computeIfAbsent(q.topic(), k -> new ArrayList<>()).add(q);
            b.byId.put(q.id(), q);
        }
        return b;
    }

    public List<Question> forTopic(String topic) { return byTopic.getOrDefault(topic, List.of()); }
    public Question get(String id) { return byId.get(id); }
}
