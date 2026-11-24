package com.dgkt.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttemptRepo extends JpaRepository<Attempt, Long> {
    List<Attempt> findByStudentCodeOrderByAnsweredAtAsc(String studentCode);
}
