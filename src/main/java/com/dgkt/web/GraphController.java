package com.dgkt.web;

import com.dgkt.core.ConceptGraph;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/** First API endpoint, only to check that the app starts: GET /api/graph */
@RestController
@RequestMapping("/api")
public class GraphController {
    private final ConceptGraph graph;

    public GraphController(ConceptGraph graph) { this.graph = graph; }

    @GetMapping("/graph")
    public List<Map<String, Object>> graph() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String id : graph.ids()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("name", graph.name(id));
            m.put("prerequisites", graph.prerequisites(id));
            m.put("reason", graph.reason(id));
            out.add(m);
        }
        return out;
    }
}
