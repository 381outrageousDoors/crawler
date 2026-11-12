package com.doors.crawler.usage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// insert-only log; analytics are ad-hoc SQL
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {
    List<RequestLog> findTop100ByOrderByRequestedAtDesc();
}
