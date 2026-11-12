package com.doors.crawler.usage;

import org.springframework.data.jpa.repository.JpaRepository;

// insert-only log; analytics are ad-hoc SQL
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {
}
