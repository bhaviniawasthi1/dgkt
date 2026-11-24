package com.dgkt.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/** Dependency graph (DAG). Arrow A -> B means "A is needed to learn B". */
public final class ConceptGraph {
    private final Map<String, String> names = new LinkedHashMap<>();
    private final Map<String, List<String>> prereqs = new LinkedHashMap<>();
    private final Map<String, String> reasons = new LinkedHashMap<>();

    public static ConceptGraph load(InputStream in) throws IOException {
        JsonNode root = new ObjectMapper().readTree(in);
        ConceptGraph g = new ConceptGraph();
        root.fields().forEachRemaining(e -> {
            List<String> pre = new ArrayList<>();
            e.getValue().get("prerequisites").forEach(x -> pre.add(x.asText()));
            g.names.put(e.getKey(), e.getValue().get("name").asText());
            g.reasons.put(e.getKey(), e.getValue().has("reason") ? e.getValue().get("reason").asText() : "");
            g.prereqs.put(e.getKey(), pre);
        });
        g.validate();
        return g;
    }

    /** Checks that every prerequisite exists and that there is no cycle. */
    private void validate() {
        for (Map.Entry<String, List<String>> e : prereqs.entrySet())
            for (String p : e.getValue())
                if (!prereqs.containsKey(p))
                    throw new IllegalStateException("Unknown prerequisite " + p + " of " + e.getKey());
        Map<String, Integer> color = new HashMap<>(); // 1 = in progress, 2 = done
        for (String id : prereqs.keySet()) dfs(id, color);
    }

    private void dfs(String id, Map<String, Integer> color) {
        Integer c = color.get(id);
        if (c != null) {
            if (c == 1) throw new IllegalStateException("Cycle at " + id);
            return;
        }
        color.put(id, 1);
        for (String p : prereqs.get(id)) dfs(p, color);
        color.put(id, 2);
    }

    public Set<String> ids() { return Collections.unmodifiableSet(prereqs.keySet()); }
    public String name(String id) { return names.get(id); }
    public String reason(String id) { return reasons.get(id); }
    public List<String> prerequisites(String id) { return prereqs.get(id); }

    /** All direct and indirect prerequisites. */
    public Set<String> ancestors(String id) {
        Set<String> out = new LinkedHashSet<>();
        Deque<String> st = new ArrayDeque<>(prereqs.get(id));
        while (!st.isEmpty()) {
            String x = st.pop();
            if (out.add(x)) st.addAll(prereqs.get(x));
        }
        return out;
    }

    /** All topics that directly or indirectly need this topic. */
    public Set<String> descendants(String id) {
        Set<String> out = new LinkedHashSet<>();
        for (String x : prereqs.keySet())
            if (ancestors(x).contains(id)) out.add(x);
        return out;
    }
}
