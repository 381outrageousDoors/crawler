package com.doors.crawler.scan;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface ScanStateRepository extends JpaRepository<ScanState, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ScanState s where s.id = 1")
    ScanState lockRow();
}
