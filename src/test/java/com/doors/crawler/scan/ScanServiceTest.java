package com.doors.crawler.scan;

import com.doors.crawler.TestcontainersConfiguration;
import com.doors.crawler.story.Story;
import com.doors.crawler.story.StoryRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ScanServiceTest {

    private static final Instant T0 = Instant.parse("2026-11-11T10:00:00Z");

    private static final StubHnServer SERVER;

    static {
        try {
            SERVER = StubHnServer.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void scannerTarget(DynamicPropertyRegistry registry) {
        registry.add("scanner.target-url", SERVER::url);
    }

    @AfterAll
    static void stopServer() {
        SERVER.close();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(T0);
        }
    }

    @Autowired ScanService scanService;
    @Autowired StoryRepository stories;
    @Autowired ScanStateRepository state;
    @Autowired MutableClock clock;

    @BeforeEach
    void freezeTimeAndResetServer() {
        clock.set(T0);
        SERVER.reset();
    }

    // --- fixtures ---------------------------------------------------------

    private static String page(String... rows) {
        return "<html><body><table>" + String.join("", rows) + "</table></body></html>";
    }

    private static String story(long id, int rank, String title, String href, String subtext) {
        return "<tr class='athing submission' id='" + id + "'>"
                + "<td class='title'><span class='rank'>" + rank + ".</span></td>"
                + "<td class='votelinks'>&#9650;</td>"
                + "<td class='title'><span class='titleline'><a href='" + href + "'>" + title + "</a></span></td>"
                + "</tr>"
                + (subtext == null ? ""
                : "<tr><td colspan='2'></td><td class='subtext'>" + subtext + "</td></tr>");
    }

    private static final String STORY_A = story(4556001, 1, "First Story", "https://example.com/one",
            "<span class='score' id='score_4556001'>123 points</span> by "
                    + "<a href='user?id=alice'>alice</a> 2 hours ago | <a href='item?id=4556001'>45&#160;comments</a>");
    private static final String STORY_B = story(4556002, 2, "Company X is hiring", "https://example.com/job",
            "by <a href='user?id=bob'>bob</a> 3 hours ago | <a href='item?id=4556002'>discuss</a>");
    private static final String STORY_C = story(4556003, 3, "Ask HN: How do you test crawlers", "item?id=4556003",
            "<span class='score' id='score_4556003'>7 points</span> by "
                    + "<a href='user?id=carol'>carol</a> 1 hour ago | <a href='item?id=4556003'>1&#160;comment</a>");

    private static final String PAGE_ABC = page(STORY_A, STORY_B, STORY_C);
    private static final String PAGE_AB = page(STORY_A, STORY_B);
    private static final String PAGE_BC = page(STORY_B, STORY_C);

    // --- requestScan: persistence ------------------------------------------

    @Test
    void requestScanPersistsFrontPageStories() {
        SERVER.serve(PAGE_ABC);

        scanService.requestScan();

        List<Story> rows = stories.findAllByOrderByRankAsc();
        assertThat(rows).hasSize(3);

        Story a = rows.get(0);
        assertThat(a.getId()).isEqualTo(4556001L);
        assertThat(a.getRank()).isEqualTo(1);
        assertThat(a.getTitle()).isEqualTo("First Story");
        assertThat(a.getPoints()).isEqualTo(123);
        assertThat(a.getCommentCount()).isEqualTo(45);
        assertThat(a.getFirstSeenAt()).isEqualTo(T0);

        // job posting: no score span, "discuss" link
        Story b = rows.get(1);
        assertThat(b.getPoints()).isNull();
        assertThat(b.getCommentCount()).isZero();

        // singular "1 comment" parses, Ask-HN relative href resolves against the target URL
        Story c = rows.get(2);
        assertThat(c.getPoints()).isEqualTo(7);
        assertThat(c.getCommentCount()).isEqualTo(1);
    }

    @Test
    void requestScanStampsStateAndSchedulesNextScan() {
        SERVER.serve(PAGE_ABC);

        scanService.requestScan();

        ScanState s = state.findById(1).orElseThrow();
        assertThat(s.getLastScanAt()).isEqualTo(T0);
        assertThat(s.getNextScanAt()).isEqualTo(T0.plusSeconds(30));
        assertThat(s.getIntervalSeconds()).isEqualTo(30);
    }

    // --- requestScan: debounce and backoff reset ----------------------------

    @Test
    void secondClickInsideMinIntervalIsDebounced() {
        SERVER.serve(PAGE_ABC);
        scanService.requestScan();
        assertThat(SERVER.hits()).isEqualTo(1);

        clock.advanceSeconds(29);
        scanService.requestScan();
        assertThat(SERVER.hits()).isEqualTo(1);
        assertThat(state.findById(1).orElseThrow().getLastScanAt()).isEqualTo(T0);

        clock.advanceSeconds(1);
        scanService.requestScan();
        assertThat(SERVER.hits()).isEqualTo(2);
    }

    @Test
    void clickResetsWidenedBackoffToMinimum() {
        ScanState s = state.findById(1).orElseThrow();
        s.setIntervalSeconds(900);
        SERVER.serve(PAGE_ABC);

        scanService.requestScan();

        assertThat(s.getIntervalSeconds()).isEqualTo(30);
        assertThat(s.getNextScanAt()).isEqualTo(T0.plusSeconds(30));
    }

    // --- tick: idle, backoff, cap --------------------------------------------

    @Test
    void tickStaysIdleUntilFirstClick() {
        scanService.tick();

        assertThat(SERVER.hits()).isZero();
        assertThat(state.findById(1).orElseThrow().getLastScanAt()).isNull();
    }

    @Test
    void tickScansWhenDueAndDoublesInterval() {
        ScanState s = state.findById(1).orElseThrow();
        s.setNextScanAt(T0);
        s.setIntervalSeconds(30);
        SERVER.serve(PAGE_ABC);

        scanService.tick();

        assertThat(SERVER.hits()).isEqualTo(1);
        assertThat(s.getIntervalSeconds()).isEqualTo(60);
        assertThat(s.getNextScanAt()).isEqualTo(T0.plusSeconds(60));
    }

    @Test
    void tickAtCapGoesIdleUntilNextClick() {
        ScanState s = state.findById(1).orElseThrow();
        s.setNextScanAt(T0);
        s.setIntervalSeconds(900);
        SERVER.serve(PAGE_ABC);

        scanService.tick();

        assertThat(SERVER.hits()).isEqualTo(1);
        assertThat(s.getNextScanAt()).isNull();
    }

    // --- failure modes ---------------------------------------------------------

    @Test
    void failedFetchStampsLastScanAndKeepsTable() {
        SERVER.failWith(500);

        scanService.requestScan();

        // lastScanAt set even on failure so errors don't cause rapid retries
        assertThat(state.findById(1).orElseThrow().getLastScanAt()).isEqualTo(T0);
        assertThat(stories.count()).isZero();
    }

    @Test
    void emptyParseDoesNotWipeStoredStories() {
        SERVER.serve(PAGE_ABC);
        scanService.requestScan();
        assertThat(stories.count()).isEqualTo(3);

        clock.advanceSeconds(60);
        SERVER.serve("<html><body>blocked</body></html>");
        scanService.requestScan();

        assertThat(stories.count()).isEqualTo(3);
    }

    // --- front-page rotation -----------------------------------------------------

    @Test
    void storiesLeavingFrontPageAreDeletedAndSurvivorsKeepFirstSeenAt() {
        SERVER.serve(PAGE_AB);
        scanService.requestScan();

        clock.advanceSeconds(60);
        SERVER.serve(PAGE_BC);
        scanService.requestScan();

        assertThat(stories.findAllByOrderByRankAsc())
                .extracting(Story::getId)
                .containsExactly(4556002L, 4556003L);

        Story survivor = stories.findById(4556002L).orElseThrow();
        assertThat(survivor.getFirstSeenAt()).isEqualTo(T0);
        assertThat(survivor.getLastSeenAt()).isEqualTo(T0.plusSeconds(60));

        Story newcomer = stories.findById(4556003L).orElseThrow();
        assertThat(newcomer.getFirstSeenAt()).isEqualTo(T0.plusSeconds(60));
    }
}
