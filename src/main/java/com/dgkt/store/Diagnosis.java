package com.dgkt.store;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One finding of a finished assessment: failing topic and one of its root causes. */
@Entity
@Table(name = "diagnoses")
public class Diagnosis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String sessionId;
    public String failingTopic;
    public String rootTopic;

    public Diagnosis() {}
}
