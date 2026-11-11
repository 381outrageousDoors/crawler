package com.doors.crawler.story;

import com.doors.crawler.TestcontainersConfiguration;
import com.doors.crawler.scan.ScanService;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class StoryControllerTest {

    private static final Instant T0 = Instant.parse("2026-11-11T10:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired StoryRepository stories;

    // web layer only: scan behaviour itself is covered by ScanServiceTest
    @MockitoBean ScanService scanService;

    @BeforeEach
    void seedFrontPage() {
        stories.upsert(1L, 1, "Top Story", 100, 42, T0);
        stories.upsert(2L, 2, "Second Story", 50, 7, T0);
    }

    @Test
    void indexRendersStoredStoriesIntoTable() throws Exception {
        String html = mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Document page = Jsoup.parse(html);
        assertThat(page.select("tbody#stories tr")).hasSize(2);
        assertThat(page.text()).contains("Top Story", "100");
        // without htmx loaded the scan button silently degrades to a native GET /
        assertThat(page.select("script[src*=htmx]")).isNotEmpty();
    }

    @Test
    void storiesEndpointReturnsBareFragmentRows() throws Exception {
        String html = mvc.perform(get("/stories"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // htmx swaps these into <tbody id="stories">, so parse them in that context
        assertThat(Jsoup.parse("<table><tbody>" + html + "</tbody></table>").select("tr")).hasSize(2);
        assertThat(html).doesNotContain("<html");
    }

    @Test
    void scanTriggersScanAndReturnsFreshRows() throws Exception {
        String html = mvc.perform(post("/scan"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(Jsoup.parse("<table><tbody>" + html + "</tbody></table>").select("tr")).hasSize(2);
        verify(scanService).requestScan();
    }

    @Test
    void emptyFrontPageRendersNoRows() throws Exception {
        stories.deleteAll();

        mvc.perform(get("/stories"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<tr"))));
    }
}
