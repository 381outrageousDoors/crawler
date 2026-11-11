package com.doors.crawler.scan;

import com.doors.crawler.story.StoryRepository;
import jakarta.transaction.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ScanService {
    private static final Logger log = LoggerFactory.getLogger(ScanService.class);
    private static final Pattern LEADING_INT = Pattern.compile("^\\d+");

    private final StoryRepository stories;
    private final ScanStateRepository state;
    private final Clock clock;

    @Value("${scanner.target-url:https://news.ycombinator.com/}") String url;
    @Value("${scanner.min-interval-seconds:30}") int minInterval;
    @Value("${scanner.max-interval-seconds:900}") int maxInterval;

    ScanService(StoryRepository stories, ScanStateRepository state, Clock clock) {
        this.stories = stories;
        this.state = state;
        this.clock = clock;
    }

    /** Called on user click: debounced scan, then restart the backoff window. */
    @Transactional
    public void requestScan() {
        ScanState s = state.lockRow();              // concurrent clicks queue here
        Instant now = clock.instant();
        boolean due = s.getLastScanAt() == null
                || !now.isBefore(s.getLastScanAt().plusSeconds(minInterval));
        if (due) scan(s, now);                      // otherwise: debounced, serve stored rows

        s.setIntervalSeconds(minInterval);          // a click always restarts the backoff
        s.setNextScanAt(s.getLastScanAt().plusSeconds(minInterval));
    }

    /** Background tick: runs a scan when one is due, widening the gap each time. */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void tick() {
        ScanState s = state.lockRow();
        Instant now = clock.instant();
        if (s.getNextScanAt() == null || now.isBefore(s.getNextScanAt())) return;

        scan(s, now);

        int waited = s.getIntervalSeconds();
        if (waited >= maxInterval) {                // reached the cap: go idle until the next click
            s.setNextScanAt(null);
            return;
        }
        int next = Math.min(waited * 2, maxInterval);
        s.setIntervalSeconds(next);
        s.setNextScanAt(now.plusSeconds(next));
    }

    private void scan(ScanState s, Instant now) {
        s.setLastScanAt(now);                       // set even on failure so errors don't cause rapid retries
        List<ParsedStory> parsed;
        try {
            parsed = fetch();
        } catch (IOException e) {
            log.warn("Fetch failed", e);
            return;
        }
        if (parsed.isEmpty()) {                     // markup changed or blocked: don't wipe the table
            log.warn("Parsed 0 stories; skipping update");
            return;
        }
        for (ParsedStory p : parsed) {
            stories.upsert(p.id(), p.rank(), p.title(), p.points(), p.comments(), now);
        }
        stories.deleteNotSeenSince(now);            // anything not in this scan left the front page
    }

    private List<ParsedStory> fetch() throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("hn-tracker (contact@example.com)")
                .timeout(10_000)
                .get();

        List<ParsedStory> out = new ArrayList<>();
        for (Element row : doc.select("tr.athing")) {
            Element rankEl = row.selectFirst("span.rank");
            Element link = row.selectFirst("span.titleline > a");
            if (rankEl == null || link == null) continue;

            Integer points = null, comments = null;
            Element sub = row.nextElementSibling();   // subtext lives in the following <tr>
            if (sub != null) {
                Element score = sub.selectFirst("span.score");
                if (score != null) points = leadingInt(score.text());

                Element last = sub.select("a").last();
                if (last != null) {
                    String t = last.text();
                    if (t.contains("comment")) comments = leadingInt(t);
                    else if (t.contains("discuss")) comments = 0;
                }
            }
            out.add(new ParsedStory(Long.parseLong(row.id()), leadingInt(rankEl.text()),
                    link.text(), link.absUrl("href"), points, comments));
        }
        return out;
    }

    private static Integer leadingInt(String text) {
        Matcher m = LEADING_INT.matcher(text.trim());
        return m.find() ? Integer.valueOf(m.group()) : null;
    }

    record ParsedStory(long id, int rank, String title, String url, Integer points, Integer comments) {}
}