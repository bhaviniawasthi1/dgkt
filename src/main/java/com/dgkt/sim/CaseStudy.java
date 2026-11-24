package com.dgkt.sim;

import com.dgkt.core.*;

import java.io.InputStream;
import java.util.*;

/**
 * Case study runner.
 *   java com.dgkt.sim.CaseStudy            -> one detailed example + batch results
 * Everything here is simulated data. It tests the logic, it does NOT prove the idea on real students.
 */
public final class CaseStudy {
    static final List<String> EXAM = List.of("dp", "bfs_dfs", "shortest_paths", "bst", "greedy", "backtracking");
    static int EXAM_QUESTIONS_PER_TOPIC = 4;

    /** Result of one student going through exam + follow-up. */
    static final class Run {
        Set<String> failing = new LinkedHashSet<>();
        Map<String, Set<String>> roots = new LinkedHashMap<>();
        int followUpQuestions = 0;
        int rounds = 0;
    }

    static Run run(ConceptGraph g, StudentModel m, SimulatedStudent s, boolean verbose) {
        Run r = new Run();
        // Step 2: exam (no feedback -> pT = 0)
        for (String t : EXAM)
            for (int i = 0; i < EXAM_QUESTIONS_PER_TOPIC; i++) m.observe(t, s.answer(t), false);
        for (String t : EXAM) if (m.isFailing(t)) r.failing.add(t);
        if (verbose) {
            System.out.println("== After exam ==");
            for (String t : EXAM) line(g, m, t);
            System.out.println("Failing topics: " + names(g, r.failing));
        }
        // Step 4: follow-up quiz in rounds, for each failing topic
        for (String f : r.failing) {
            for (int round = 1; round <= 20; round++) {
                List<Need> needs = RootCauseAnalyzer.nextRound(g, m, f);
                if (needs.isEmpty()) break;
                r.rounds++;
                if (verbose) System.out.println("\n-- Follow-up for " + g.name(f) + ", round " + round + " --");
                for (Need nd : needs) {
                    for (int i = 0; i < nd.count(); i++) {
                        boolean ok = s.answer(nd.topic());
                        m.observe(nd.topic(), ok, false);
                        r.followUpQuestions++;
                    }
                    if (verbose) line(g, m, nd.topic());
                }
            }
            r.roots.put(f, RootCauseAnalyzer.rootCauses(g, m, f));
        }
        return r;
    }

    static void line(ConceptGraph g, StudentModel m, String t) {
        TopicState st = m.state(t);
        System.out.printf("  %-22s n=%d  p=%.3f  %s%n", g.name(t), st.n, st.p, m.status(t));
    }

    static String names(ConceptGraph g, Collection<String> ids) {
        List<String> out = new ArrayList<>();
        for (String i : ids) out.add(g.name(i));
        return out.toString();
    }


    static final class Result {
        int N, failing, trulyWeak, falseAlarm, missed, dgktOk, naiveOk; long fq, rounds;
        String report() {
            return String.format(
                "Failing topics found: %d (true weak: %d, false alarms: %d)%n"
              + "Weak exam topics missed (passed by luck): %d%n"
              + "Root cause correct, DGKT:                %.1f%%%n"
              + "Root cause correct, naive (blame topic): %.1f%%%n"
              + "Avg follow-up questions per student: %.1f, avg rounds: %.1f",
                failing, trulyWeak, falseAlarm, missed, 100.0 * dgktOk / trulyWeak, 100.0 * naiveOk / trulyWeak,
                (double) fq / N, (double) rounds / N);
        }
    }

    static Result batch(ConceptGraph g, int n) {
        Result x = new Result(); x.N = n;
        Random pick = new Random(42);
        List<String> ids = new ArrayList<>(g.ids());
        for (int i = 0; i < n; i++) {
            Set<String> planted = new LinkedHashSet<>();
            planted.add(ids.get(pick.nextInt(ids.size())));
            if (pick.nextDouble() < 0.25) planted.add(ids.get(pick.nextInt(ids.size())));
            SimulatedStudent s = new SimulatedStudent(g, planted, 1000 + i);
            Run r = run(g, new StudentModel(g), s, false);
            x.fq += r.followUpQuestions; x.rounds += r.rounds;
            for (String t : EXAM) if (!s.reallyKnows(t) && !r.failing.contains(t)) x.missed++;
            for (String f : r.failing) {
                x.failing++;
                if (s.reallyKnows(f)) { x.falseAlarm++; continue; }
                x.trulyWeak++;
                Set<String> truth = s.trueRoots(g, f);
                if (r.roots.get(f).equals(truth)) x.dgktOk++;
                if (Set.of(f).equals(truth)) x.naiveOk++;
            }
        }
        return x;
    }

    /** Try different exam sizes and thresholds. */
    static void sweep(ConceptGraph g) {
        System.out.println("exam_q n_min theta | falseAlarm  missed  DGKT%  naive%  followUpQ");
        for (int q : new int[]{3, 4, 5, 6})
            for (double th : new double[]{0.40, 0.50, 0.60})
                for (int nm : new int[]{3, 4}) {
                    EXAM_QUESTIONS_PER_TOPIC = q; Thresholds.THETA = th; Thresholds.N_MIN = nm; Thresholds.CAP = nm + 2;
                    Result r = batch(g, 1000);
                    System.out.printf("%6d %5d %5.2f | %9d %7d %6.1f %7.1f %9.1f%n", q, nm, th, r.falseAlarm, r.missed,
                        100.0 * r.dgktOk / r.trulyWeak, 100.0 * r.naiveOk / r.trulyWeak, (double) r.fq / r.N);
                }
    }

    public static void main(String[] args) throws Exception {
        ConceptGraph g;
        try (InputStream in = CaseStudy.class.getResourceAsStream("/data/graph.json")) { g = ConceptGraph.load(in); }

        if (args.length > 0 && args[0].equals("sweep")) { sweep(g); return; }

        // ---- Example: one student, planted weakness = Recursion ----
        System.out.println("##### EXAMPLE STUDENT (planted weak topic: Recursion) #####");
        SimulatedStudent demo = new SimulatedStudent(g, Set.of("recursion"), 7);
        Run d = run(g, new StudentModel(g), demo, true);
        System.out.println("\nDGKT root cause:");
        for (var e : d.roots.entrySet())
            System.out.println("  failing " + g.name(e.getKey()) + " -> root " + names(g, e.getValue()));
        System.out.println("Follow-up questions used: " + d.followUpQuestions + " in " + d.rounds + " rounds\n");

        Result r0 = batch(g, 2000);
        System.out.println("##### BATCH: 2000 simulated students (made-up data) #####");
        System.out.println(r0.report());
    }
}
