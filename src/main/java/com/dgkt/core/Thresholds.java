package com.dgkt.core;

/** My own choices (defaults chosen with the simulated sweep, see Experiments) (not from a paper). Not final on purpose: the case study sweeps them. */
public final class Thresholds {
    public static double THETA = 0.50;   // p below this = weak
    public static int N_MIN = 3;         // answers needed before we trust a topic's p
    public static int FAIL_N_MIN = 2;    // answers needed to call a topic "failing" in the exam
    public static double DELTA = 0.10;   // |p - THETA| below this = too close to decide
    public static int CAP = 5;           // max answers per topic in the follow-up

    private Thresholds() {}
}
