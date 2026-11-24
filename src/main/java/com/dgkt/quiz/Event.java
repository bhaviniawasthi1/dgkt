package com.dgkt.quiz;

/** One answered question: which topic, right or wrong, and how the belief p moved. */
public record Event(int seq, String phase, int round, String topic, boolean correct,
                    double pBefore, double pAfter, int nAfter) {}
