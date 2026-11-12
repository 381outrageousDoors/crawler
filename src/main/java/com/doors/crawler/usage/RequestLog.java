package com.doors.crawler.usage;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

/** One row per web request, written by UsageService; named RequestLog to avoid clashing with HttpServletRequest. */
@Entity
@Data
@Table(name = "request")
public class RequestLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    Instant requestedAt;
    String filter;        // FilterSpec.describe(), null when no filter active
    String browser;       // coarse parsed name
    String userAgent;     // raw UA, truncated to 512 chars
    String endpoint;      // "index" | "scan" | "stories"
    String filterField;   // "none" | "word" | "points" | "comments"
    String filterOp;      // "gt" | "gte" | "lt" | "lte" | "eq"
    Integer filterValue;
    String sortField;     // "rank" | "title" | "points" | "comments"
    boolean sortDesc;
    String preset;        // "ask1" | "ask2" | null
    Integer resultCount;
    Long durationMs;      // view-build time, ms
}
