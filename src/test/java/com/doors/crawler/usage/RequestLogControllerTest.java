package com.doors.crawler.usage;

import com.doors.crawler.TestcontainersConfiguration;
import com.doors.crawler.scan.ScanService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class RequestLogControllerTest {

    private static final Instant T0 = Instant.parse("2026-11-11T10:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired RequestLogRepository requests;

    // web layer only: keep the scheduled scanner off the network during tests
    @MockitoBean ScanService scanService;

    private void seed(Instant at, String preset, String filter,
                      Integer count, Long ms, String endpoint, String browser) {
        RequestLog r = new RequestLog();
        r.setRequestedAt(at);
        r.setPreset(preset);
        r.setFilter(filter);
        r.setResultCount(count);
        r.setDurationMs(ms);
        r.setEndpoint(endpoint);
        r.setBrowser(browser);
        requests.save(r);
    }

    private Document fetch(String uri) throws Exception {
        String html = mvc.perform(get(uri))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Jsoup.parse(html);
    }

    private static List<Element> rows(Document view) {
        return view.select("#request-view tbody tr").asList();
    }

    @Test
    void requestsPageRendersRowsNewestFirst() throws Exception {
        seed(T0, null, null, 30, 12L, "index", "Firefox");
        seed(T0.plusSeconds(60), "ask1", null, 2, 7L, "stories", "Chrome");

        Document page = fetch("/requests");
        List<Element> rows = rows(page);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).child(0).text()).isEqualTo("2026-11-11 10:01:00");
        assertThat(rows.get(0).text()).contains("stories", "Chrome");
        assertThat(rows.get(1).child(0).text()).isEqualTo("2026-11-11 10:00:00");
    }

    @Test
    void presetColumnPrefersPresetThenFilterThenDash() throws Exception {
        seed(T0.plusSeconds(120), "ask1", "Filter: word_count > 5 · Sorted by comment_count desc",
                2, 7L, "stories", "Chrome");
        seed(T0.plusSeconds(60), null, "Filter: points > 60", 4, 9L, "stories", "Firefox");
        seed(T0, null, null, 30, 12L, "index", "Firefox");

        List<Element> rows = rows(fetch("/requests"));
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).child(1).text()).isEqualTo("ask1");
        assertThat(rows.get(1).child(1).text()).isEqualTo("Filter: points > 60");
        assertThat(rows.get(2).child(1).text()).isEqualTo("—");
    }

    @Test
    void requestsDataReturnsFragmentOnly() throws Exception {
        seed(T0, null, null, 30, 12L, "index", "Firefox");

        String html = mvc.perform(get("/requests/data"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("id=\"request-view\"").doesNotContain("<html");
    }

    @Test
    void capsAtLatestHundred() throws Exception {
        for (int i = 1; i <= 101; i++) {
            seed(T0.plusSeconds(i), null, null, i, (long) i, "stories", i == 1 ? "oldest" : "b" + i);
        }

        Document page = fetch("/requests");
        assertThat(rows(page)).hasSize(100);
        assertThat(page.text()).doesNotContain("oldest");
    }

    @Test
    void requestsPageIsNotLogged() throws Exception {
        mvc.perform(get("/requests")).andExpect(status().isOk());

        assertThat(requests.count()).isZero();
    }
}
