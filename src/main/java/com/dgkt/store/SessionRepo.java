package com.dgkt.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRepo extends JpaRepository<SessionRecord, String> {
    List<SessionRecord> findByStudentCodeOrderByStartedAtDesc(String studentCode);
}
