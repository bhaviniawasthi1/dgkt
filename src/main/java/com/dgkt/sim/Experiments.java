package com.dgkt.sim;

import com.dgkt.core.*;
import com.dgkt.quiz.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * All experiments of the case study, run through the real QuizEngine.
 * Usage: java com.dgkt.sim.Experiments results.json
 * Everything uses simulated students (made-up data).
 */
public final class Experiments {
    static final List<String> EXAM6 = List.of("dp", "bfs_dfs", "shortest_paths", "bst", "greedy", "backtracking");

    static ConceptGraph graph;
    static QuestionBank bank;

    static final class Cfg {
        List<String> exam = EXAM6;
        int examQ = 4;
        double guess = Bkt.P_G, slip = Bkt.P_S;
        int students = 1000;
        double twoPlantedChance = 0.25;
        Set<String> fixedPlanted = null;       // if set, every student gets this
        String rating = "none";                // none | accurate | wrong
        long seed = 1;
    }

    static final class Stat {
        int students, failingTopics, trueWeak, falseAlarm, missed, exact, naiveExact, trueRootsTotal, trueRootsFound, foundRootsTotal;
        long examQ, followQ, rounds;
        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("students", students);
            m.put("failingTopics", failingTopics);
            m.put("trueWeakFailing", trueWeak);
            m.put("falseAlarms", falseAlarm);
            m.put("missedWeak", missed);
            m.put("dgktExactPct", pct(exact, trueWeak));
            m.put("naiveExactPct", pct(naiveExact, trueWeak));
            m.put("rootRecallPct", pct(trueRootsFound, trueRootsTotal));
            m.put("rootPrecisionPct", pct(trueRootsFound, foundRootsTotal));
            m.put("avgExamQ", round1((double) examQ / students));
            m.put("avgFollowQ", round1((double) followQ / students));
            m.put("avgTotalQ", round1((double) (examQ + followQ) / students));
            m.put("avgRounds", round1((double) rounds / students));
            return m;
        }
    }

    static double pct(int a, int b) { return b == 0 ? 0 : Math.round(1000.0 * a / b) / 10.0; }
    static double round1(double x) { return Math.round(x * 10) / 10.0; }

    static Set<String> pickPlanted(Random r, List<String> ids, double two) {
        Set<String> p = new LinkedHashSet<>();
        p.add(ids.get(r.nextInt(ids.size())));
        if (r.nextDouble() < two) p.add(ids.get(r.nextInt(ids.size())));
        return p;
    }

    /** Plays one student through the engine. Returns the session id. */
    static String play(QuizEngine e, SimulatedStudent s, Cfg c) {
        Map<String, Integer> ratings = null;
        if (!c.rating.equals("none")) {
            ratings = new HashMap<>();
            for (String t : graph.ids()) {
                boolean knows = s.reallyKnows(t);
                boolean good = c.rating.equals("accurate");
                ratings.put(t, (knows == good) ? 8 : 2);
            }
        }
        QuizSession sess = e.start(c.exam, ratings);
        Question q;
        while ((q = e.next(sess.id())) != null) {
            boolean right = s.answer(q.topic());
            e.answer(sess.id(), q.id(), right ? q.correctIndex() : (q.correctIndex() + 1) % 4);
        }
        return sess.id();
    }

    static Stat run(Cfg c) {
        Stat st = new Stat();
        Random pick = new Random(c.seed * 7919);
        QuizEngine e = new QuizEngine(graph, bank, null, c.examQ, new Random(c.seed));
        List<String> ids = new ArrayList<>(graph.ids());
        for (int i = 0; i < c.students; i++) {
            Set<String> planted = c.fixedPlanted != null ? c.fixedPlanted : pickPlanted(pick, ids, c.twoPlantedChance);
            SimulatedStudent s = new SimulatedStudent(graph, planted, c.seed * 100000 + i, c.guess, c.slip);
            String id = play(e, s, c);
            st.students++;
            List<Event> ev = e.events(id);
            for (Event x : ev) { if (x.phase().equals("EXAM")) st.examQ++; else st.followQ++; }
            st.rounds += ev.stream().filter(x -> x.round() > 0).mapToInt(Event::round).max().orElse(0);
            for (String t : e.passedExamTopics(id)) if (!s.reallyKnows(t)) st.missed++;
            for (Map.Entry<String, Set<String>> f : e.rootCauseIds(id).entrySet()) {
                st.failingTopics++;
                if (s.reallyKnows(f.getKey())) { st.falseAlarm++; continue; }
                st.trueWeak++;
                Set<String> truth = s.trueRoots(graph, f.getKey());
                if (f.getValue().equals(truth)) st.exact++;
                if (Set.of(f.getKey()).equals(truth)) st.naiveExact++;
                st.trueRootsTotal += truth.size();
                st.foundRootsTotal += f.getValue().size();
                for (String r : f.getValue()) if (truth.contains(r)) st.trueRootsFound++;
            }
        }
        return st;
    }

    static Map<String, Object> traceMap(QuizEngine e, String id, int seed) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("seed", seed);
        m.put("events", e.events(id));
        m.put("result", e.result(id));
        return m;
    }

    static Map<String, Object> withCfg(Cfg c, Stat s, String label) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("examQ", c.examQ); m.put("theta", Thresholds.THETA); m.put("nMin", Thresholds.N_MIN); m.put("cap", Thresholds.CAP);
        m.put("guess", c.guess); m.put("slip", c.slip);
        m.putAll(s.toMap());
        return m;
    }

    public static void main(String[] args) throws Exception {
        try (InputStream in = Experiments.class.getResourceAsStream("/data/graph.json")) { graph = ConceptGraph.load(in); }
        try (InputStream in = Experiments.class.getResourceAsStream("/data/questions.json")) { bank = QuestionBank.load(in); }
        Map<String, Object> out = new LinkedHashMap<>();
        double th = Thresholds.THETA; int nm = Thresholds.N_MIN, cap = Thresholds.CAP, fn = Thresholds.FAIL_N_MIN; double dl = Thresholds.DELTA;

        // E1: two detailed students (weakness planted in Recursion, exam on DP, BFS/DFS, BST).
        // E1_clean = first seed where every diagnosis is exactly right. E1_error = first seed with a wrong root.
        {
            Cfg c = new Cfg(); c.exam = List.of("dp", "bfs_dfs", "bst");
            boolean haveClean = false, haveError = false;
            for (int seed = 1; seed < 500 && !(haveClean && haveError); seed++) {
                QuizEngine e = new QuizEngine(graph, bank, null, 4, new Random(seed));
                SimulatedStudent s = new SimulatedStudent(graph, Set.of("recursion"), 1000 + seed);
                String id = play(e, s, c);
                Map<String, Set<String>> roots = e.rootCauseIds(id);
                boolean allRight = roots.size() == 3;
                for (Map.Entry<String, Set<String>> f : roots.entrySet())
                    if (!f.getValue().equals(s.trueRoots(graph, f.getKey()))) allRight = false;
                boolean rounds2 = e.events(id).stream().mapToInt(Event::round).max().orElse(0) <= 2;
                boolean wrong = roots.size() == 3 && !allRight && e.events(id).size() < 60;
                if (allRight && rounds2 && !haveClean) { haveClean = true; out.put("E1_clean", traceMap(e, id, seed)); }
                if (wrong && !haveError) { haveError = true; out.put("E1_error", traceMap(e, id, seed)); }
            }
        }
        // E2: planted weakness = DP itself (no prerequisite is weak)
        { Cfg c = new Cfg(); c.exam = List.of("dp", "bst", "greedy"); c.fixedPlanted = Set.of("dp"); c.seed = 2;
          out.put("E2_selfRoot", withCfg(c, run(c), "weak topic is DP itself")); }
        // E3: main batch
        { Cfg c = new Cfg(); c.students = 2000; c.seed = 3;
          out.put("E3_main", withCfg(c, run(c), "main batch, 1 or 2 planted topics"));
          Cfg one = new Cfg(); one.students = 1000; one.seed = 31; one.twoPlantedChance = 0;
          out.put("E3_onePlanted", withCfg(one, run(one), "exactly 1 planted topic"));
          Cfg two = new Cfg(); two.students = 1000; two.seed = 32; two.twoPlantedChance = 1.0;
          out.put("E3_twoPlanted", withCfg(two, run(two), "2 planted topics (usually)")); }
        // E4: sweep
        {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int q : new int[]{3, 4, 5})
                for (double t : new double[]{0.40, 0.50, 0.60})
                    for (int n : new int[]{3, 4}) {
                        Cfg c = new Cfg(); c.examQ = q; c.seed = 4; c.students = 1000;
                        Thresholds.THETA = t; Thresholds.N_MIN = n; Thresholds.CAP = n + 2;
                        rows.add(withCfg(c, run(c), "sweep"));
                    }
            out.put("E4_sweep", rows);
            Thresholds.THETA = th; Thresholds.N_MIN = nm; Thresholds.CAP = cap;
        }
        // E5: model assumes guess .25 / slip .10, but students are different
        {
            List<Map<String, Object>> rows = new ArrayList<>();
            double[][] gs = {{0.25, 0.10}, {0.25, 0.20}, {0.35, 0.10}, {0.35, 0.20}, {0.15, 0.05}};
            for (double[] x : gs) { Cfg c = new Cfg(); c.guess = x[0]; c.slip = x[1]; c.seed = 5; rows.add(withCfg(c, run(c), "noise")); }
            out.put("E5_noise", rows);
        }
        // E6: how many questions does a student face?
        {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int k = 1; k <= 6; k++) { Cfg c = new Cfg(); c.exam = EXAM6.subList(0, k); c.seed = 6; c.students = 500;
                Map<String, Object> m = withCfg(c, run(c), "topics=" + k); m.put("examTopics", k); rows.add(m); }
            out.put("E6_burden", rows);
        }
        // E7: self-rating
        {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (String r : new String[]{"none", "accurate", "wrong"}) { Cfg c = new Cfg(); c.rating = r; c.seed = 7; rows.add(withCfg(c, run(c), "rating " + r)); }
            out.put("E7_rating", rows);
        }
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File(args[0]), out);
        System.out.println("saved " + args[0]);
    }
}
