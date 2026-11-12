package com.doors.crawler.story;

import com.doors.crawler.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class StoryRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-11-11T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);

    @Autowired StoryRepository stories;

    @Test
    void upsertUpdatesRowAndPreservesFirstSeenAt() {
        stories.upsert(100L, 1, "Old Title", 10, 5, T0);
        stories.upsert(100L, 3, "New Title", 20, 7, T1);

        Story s = stories.findById(100L).orElseThrow();
        assertThat(s.getRank()).isEqualTo(3);
        assertThat(s.getTitle()).isEqualTo("New Title");
        assertThat(s.getPoints()).isEqualTo(20);
        assertThat(s.getCommentCount()).isEqualTo(7);
        assertThat(s.getFirstSeenAt()).isEqualTo(T0);
        assertThat(s.getLastSeenAt()).isEqualTo(T1);
    }

    @Test
    void upsertComputesWordCount() {
        stories.upsert(9L, 1, "Ask HN — why do people code", 1, 1, T0);

        assertThat(stories.findById(9L).orElseThrow().getWordCount()).isEqualTo(6);
    }

    @Test
    void deleteNotSeenSinceKeepsRowsSeenExactlyAtTheBoundary() {
        stories.upsert(201L, 1, "Stale", 1, 1, T0);
        stories.upsert(202L, 2, "Fresh", 2, 2, T1);

        stories.deleteNotSeenSince(T1);

        assertThat(stories.findAll()).extracting(Story::getId).containsExactly(202L);
    }
}
