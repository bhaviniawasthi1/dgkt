package com.dgkt.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One quiz run. mode is ASSESSMENT (exam + follow-up) or PRACTICE. */
@Entity
@Table(name = "quiz_sessions")
public class SessionRecord {
    @Id
    public String id;
    public String studentCode;
    public String mode;
    @Column(length = 1000)
    public String topics;          // chosen topic ids, comma separated
    public Instant startedAt;
    public Instant finishedAt;
    public int examQuestions;
    public int followUpQuestions;
    public int roundsUsed;
    @Column(length = 20000)
    public String resultJson;      // the full result shown to the student

    public SessionRecord() {}
}
