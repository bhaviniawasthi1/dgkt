package com.dgkt.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DiagnosisRepo extends JpaRepository<Diagnosis, Long> {
    /** Which topics are found most often as a root cause: rows of [topicId, count]. */
    @Query("select d.rootTopic, count(d) from Diagnosis d group by d.rootTopic order by count(d) desc")
    List<Object[]> rootCauseCounts();
}
