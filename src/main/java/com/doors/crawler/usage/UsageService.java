package com.doors.crawler.usage;

import com.doors.crawler.story.FilterSpec;
import org.springframework.stereotype.Service;

import java.time.Clock;

/** Records one request-log row per page request; timestamp from the injected Clock so tests control it. */
@Service
public class UsageService {

    private static final int USER_AGENT_MAX = 512;

    private final RequestLogRepository requests;
    private final Clock clock;

    UsageService(RequestLogRepository requests, Clock clock) {
        this.requests = requests;
        this.clock = clock;
    }

    public void record(String endpoint, FilterSpec spec, String preset, String userAgent,
                       int resultCount, long durationMs) {
        RequestLog log = new RequestLog();
        log.setRequestedAt(clock.instant());
        log.setFilter(spec.describe());
        log.setBrowser(browserFrom(userAgent));
        log.setUserAgent(truncate(userAgent));
        log.setEndpoint(endpoint);
        log.setFilterField(spec.fieldParam());
        log.setFilterOp(spec.opParam());
        log.setFilterValue(spec.value());
        log.setSortField(spec.sortParam());
        log.setSortDesc(spec.desc());
        log.setPreset(preset);
        log.setResultCount(resultCount);
        log.setDurationMs(durationMs);
        requests.save(log);
    }

    private static String truncate(String ua) {
        if (ua == null) {
            return null;
        }
        return ua.length() > USER_AGENT_MAX ? ua.substring(0, USER_AGENT_MAX) : ua;
    }

    /** Coarse browser name. Order matters: Edge UAs contain "Chrome/" and "Safari/", Chrome UAs contain "Safari/". */
    static String browserFrom(String ua) {
        if (ua == null || ua.isBlank()) {
            return "Unknown";
        }
        if (ua.contains("Edg/")) {
            return "Edge";
        }
        if (ua.contains("OPR/")) {
            return "Opera";
        }
        if (ua.contains("Firefox/") || ua.contains("FxiOS/")) {
            return "Firefox";
        }
        if (ua.contains("Chrome/") || ua.contains("Chromium/")) {
            return "Chrome";
        }
        if (ua.contains("Safari/")) {
            return "Safari";
        }
        return "Other";
    }
}
