package com.doors.crawler.story;

import com.doors.crawler.TestcontainersConfiguration;
import com.doors.crawler.scan.ScanService;
import com.doors.crawler.usage.RequestLog;
import com.doors.crawler.usage.RequestLogRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class StoryControllerTest {

    private static final Instant T0 = Instant.parse("2026-11-11T10:00:00Z");

    private static final String FIREFOX_UA =
            "Mozilla/5.0 (X11; Linux x86_64; rv:132.0) Gecko/20100101 Firefox/132.0";
    private static final String CHROME_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/131.0.0.0 Safari/537.36";

    @Autowired MockMvc mvc;
    @Autowired StoryRepository stories;
    @Autowired RequestLogRepository requests;

    // web layer only: scan behaviour itself is covered by ScanServiceTest
    @MockitoBean ScanService scanService;

    @BeforeEach
    void seedFrontPage() {
        stories.upsert(1L, 1, "Top Story", 100, 42, T0);
        stories.upsert(2L, 2, "Second Story", 50, 7, T0);
    }

    // extra fixtures for filter/sort tests: titles of 7, 6 and 5 words
    private void seedFilterFixtures() {
        stories.upsert(3L, 3, "Ask HN: what is your favorite editor", 30, 90, T0);
        stories.upsert(4L, 4, "Ask HN — why do people code", 20, 55, T0);
        stories.upsert(5L, 5, "Five words in this title", 10, 3, T0);
    }

    private Document fetch(String uri) throws Exception {
        String html = mvc.perform(get(uri))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Jsoup.parse(html);
    }

    private static java.util.List<String> titles(Document view) {
        return view.select("#story-view tbody tr").asList().stream()
                .map(tr -> tr.child(1).text())
                .toList();
    }

    @Test
    void indexRendersStoredStoriesIntoTable() throws Exception {
        when(scanService.lastScanAt()).thenReturn(T0);

        Document page = fetch("/");
        assertThat(page.select("#story-view tbody tr")).hasSize(2);
        assertThat(page.text()).contains("Top Story", "100");
        assertThat(page.text()).contains("Last scanned: 2026-11-11 10:00:00 UTC");
        // without htmx loaded the scan button silently degrades to a native GET /
        assertThat(page.select("script[src*=htmx]")).isNotEmpty();
        // Ask presets sit next to Scan now and target the view directly
        assertThat(page.select("button[hx-get*='preset=ask1']")).isNotEmpty();
        assertThat(page.select("button[hx-get*='preset=ask2']")).isNotEmpty();
    }

    @Test
    void storiesEndpointReturnsViewWithControls() throws Exception {
        Document view = fetch("/stories");

        assertThat(view.selectFirst("#story-view")).isNotNull();
        assertThat(view.select("#story-view tbody tr")).hasSize(2);
        assertThat(view.selectFirst("#story-view").attr("hx-get")).isEqualTo("/stories");
        assertThat(view.select("select[name=field]")).isNotEmpty();
        assertThat(view.select("select[name=op]")).isNotEmpty();
        assertThat(view.select("input[name=value][value=5]")).isNotEmpty();
        assertThat(view.select("select[name=sort]")).isNotEmpty();
        assertThat(view.select("select[name=dir]")).isNotEmpty();
        assertThat(view.select("button[name=preset][value=clear]")).isNotEmpty();
        assertThat(view.select("#filters button[type=submit]:not([name])")).isNotEmpty();
        assertThat(view.selectFirst("#filters").attr("hx-trigger")).isEqualTo("submit");
    }

    @Test
    void scanTriggersScanAndReturnsFreshRows() throws Exception {
        when(scanService.lastScanAt()).thenReturn(T0);

        String html = mvc.perform(post("/scan"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Document view = Jsoup.parse(html);
        assertThat(view.select("#story-view tbody tr")).hasSize(2);
        assertThat(view.selectFirst("#story-view").attr("hx-get")).isEqualTo("/stories");
        assertThat(view.text()).contains("Last scanned: 2026-11-11 10:00:00 UTC");
        verify(scanService).requestScan();
    }

    @Test
    void emptyFrontPageRendersNoRows() throws Exception {
        stories.deleteAll();

        Document view = fetch("/stories");
        assertThat(view.select("#story-view tbody tr")).isEmpty();
    }

    @Test
    void presetAskOneFiltersAndFillsForm() throws Exception {
        seedFilterFixtures();

        Document view = fetch("/stories?preset=ask1");
        assertThat(titles(view))
                .containsExactly("Ask HN: what is your favorite editor", "Ask HN — why do people code");
        assertThat(view.selectFirst("#story-view").attr("hx-get"))
                .isEqualTo("/stories?field=word&op=gt&value=5&sort=comments&dir=desc");
        assertThat(view.select("select[name=field] option[value=word][selected]")).isNotEmpty();
        assertThat(view.select("select[name=op] option[value=gt][selected]")).isNotEmpty();
        assertThat(view.select("select[name=sort] option[value=comments][selected]")).isNotEmpty();
        assertThat(view.select("select[name=dir] option[value=desc][selected]")).isNotEmpty();
        assertThat(view.selectFirst("input[name=value]").attr("value")).isEqualTo("5");
        assertThat(view.select("#status").text())
                .endsWith("Filter: word_count > 5 · Sorted by comment_count desc");
    }

    @Test
    void presetAskTwoFiltersByPoints() throws Exception {
        seedFilterFixtures();

        Document view = fetch("/stories?preset=ask2");
        assertThat(titles(view))
                .containsExactly("Top Story", "Second Story", "Five words in this title");
        assertThat(view.selectFirst("#story-view").attr("hx-get"))
                .contains("field=word&op=lte&value=5&sort=points&dir=desc");
    }

    @Test
    void clearPresetResetsView() throws Exception {
        seedFilterFixtures();

        Document view = fetch("/stories?preset=clear&field=points&op=gt&value=60");
        assertThat(titles(view)).hasSize(5);
        assertThat(view.selectFirst("#story-view").attr("hx-get")).isEqualTo("/stories");
        assertThat(view.select("select[name=field] option[value=none][selected]")).isNotEmpty();
    }

    @Test
    void explicitParamsFilterAndSort() throws Exception {
        seedFilterFixtures();

        Document view = fetch("/stories?field=points&op=gt&value=60&sort=title&dir=asc");
        assertThat(titles(view)).containsExactly("Top Story");
        assertThat(view.selectFirst("#story-view").attr("hx-get"))
                .isEqualTo("/stories?field=points&op=gt&value=60&sort=title&dir=asc");
    }

    @Test
    void invalidValueFallsBackToFive() throws Exception {
        seedFilterFixtures();

        Document view = fetch("/stories?field=points&op=eq&value=abc");
        assertThat(view.select("#story-view tbody tr")).isEmpty();
        assertThat(view.selectFirst("input[name=value]").attr("value")).isEqualTo("5");
    }

    @Test
    void statusLineDescribesActiveView() throws Exception {
        // unstubbed mock: lastScanAt() is null
        assertThat(fetch("/stories").select("#status").text()).isEqualTo("Not scanned yet");
        assertThat(fetch("/stories?preset=ask2").select("#status").text())
                .isEqualTo("Not scanned yet · Filter: word_count <= 5 · Sorted by points desc");
        assertThat(fetch("/stories?sort=title&dir=desc").select("#status").text())
                .isEqualTo("Not scanned yet · Sorted by title desc");
    }

    @Test
    void indexRequestIsLogged() throws Exception {
        mvc.perform(get("/").header("User-Agent", FIREFOX_UA))
                .andExpect(status().isOk());

        java.util.List<RequestLog> rows = requests.findAll();
        assertThat(rows).hasSize(1);
        RequestLog row = rows.get(0);
        assertThat(row.getEndpoint()).isEqualTo("index");
        assertThat(row.getBrowser()).isEqualTo("Firefox");
        assertThat(row.getFilter()).isNull();
        assertThat(row.getFilterField()).isEqualTo("none");
        assertThat(row.getResultCount()).isEqualTo(2); // seedFrontPage seeds two stories
    }

    @Test
    void storiesRequestIsLogged() throws Exception {
        seedFilterFixtures();
        mvc.perform(get("/stories?field=word&op=gt&value=6&sort=points&dir=desc")
                        .header("User-Agent", CHROME_UA))
                .andExpect(status().isOk());

        java.util.List<RequestLog> rows = requests.findAll();
        assertThat(rows).hasSize(1);
        RequestLog row = rows.get(0);
        assertThat(row.getEndpoint()).isEqualTo("stories");
        assertThat(row.getBrowser()).isEqualTo("Chrome");
        assertThat(row.getFilterField()).isEqualTo("word");
        assertThat(row.getFilterOp()).isEqualTo("gt");
        assertThat(row.getFilterValue()).isEqualTo(6);
        assertThat(row.getSortField()).isEqualTo("points");
        assertThat(row.isSortDesc()).isTrue();
        assertThat(row.getResultCount()).isEqualTo(1);
        assertThat(row.getFilter()).contains("word_count > 6");
        assertThat(row.getDurationMs()).isNotNull().isGreaterThanOrEqualTo(0L);
    }

    @Test
    void scanRequestIsLogged() throws Exception {
        mvc.perform(post("/scan"))
                .andExpect(status().isOk());

        java.util.List<RequestLog> rows = requests.findAll();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getEndpoint()).isEqualTo("scan");
    }
}
