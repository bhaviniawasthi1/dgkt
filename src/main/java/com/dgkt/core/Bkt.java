package com.dgkt.core;

/**
 * Bayesian Knowledge Tracing for ONE topic.
 * Hidden state: the student knows the topic (1) or not (0).
 * p = our belief that the student knows it.
 */
public final class Bkt {
    public static final double P_L0 = 0.30; // start belief (no information)
    public static final double P_T = 0.10;  // chance of learning after an attempt
    public static final double P_G = 0.25;  // guess: right answer without knowing (4-option MCQ)
    public static final double P_S = 0.10;  // slip: wrong answer although knowing

    private Bkt() {}

    /** Bayes step: belief after seeing one answer. */
    public static double posterior(double p, boolean correct) {
        if (correct) {
            double num = p * (1 - P_S);
            return num / (num + (1 - p) * P_G);
        }
        double num = p * P_S;
        return num / (num + (1 - p) * (1 - P_G));
    }

    /**
     * Full update: Bayes step, then the learning step.
     * @param pT chance of learning from this attempt. Use 0 when the student gets
     *           no feedback (exam, follow-up quiz), use P_T in practice mode.
     */
    public static double update(double p, boolean correct, double pT) {
        double post = posterior(p, correct);
        return post + (1 - post) * pT;
    }
}
