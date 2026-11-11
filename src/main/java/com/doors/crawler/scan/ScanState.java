package com.doors.crawler.scan;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
@Table(name="scan_state")
public class ScanState {
    @Id
    Integer id = 1;
    Instant lastScanAt;
    Instant nextScanAt;
    int intervalSeconds = 30;
}
