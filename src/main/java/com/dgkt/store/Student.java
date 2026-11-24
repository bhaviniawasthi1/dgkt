package com.dgkt.store;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A student. No password: the code is a random id made by the browser, the nickname is optional. */
@Entity
@Table(name = "students")
public class Student {
    @Id
    public String code;
    public String nickname;
    public Instant createdAt;

    public Student() {}
}
