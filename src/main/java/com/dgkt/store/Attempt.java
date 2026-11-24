package com.dgkt.store;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One answered question, with the belief p before and after. */
@Entity
@Table(name = "attempts")
public class Attempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String sessionId;
    public String studentCode;
    public int seq;
    public String phase;           // EXAM, FOLLOW_UP or PRACTICE
    public int roundNo;
    public String topicId;
    public String questionId;
    public int choice;
    public boolean correct;
    public double pBefore;
    public double pAfter;
    public int nAfter;
    public Instant answeredAt;

    public Attempt() {}
}
