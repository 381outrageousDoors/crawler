package com.doors.crawler.story;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WordCountTest {

    @Test
    void countsWhitespaceSeparatedTokens() {
        assertThat(WordCount.of("Ask HN: what is your favorite editor")).isEqualTo(7);
        assertThat(WordCount.of("Top Story")).isEqualTo(2);
        assertThat(WordCount.of("Five words in this title")).isEqualTo(5);
    }

    @Test
    void skipsSymbolOnlyTokens() {
        assertThat(WordCount.of("Ask HN — why do people code")).isEqualTo(6);
        assertThat(WordCount.of(":: !! ??")).isEqualTo(0);
    }

    @Test
    void nullAndEmptyAreZero() {
        assertThat(WordCount.of(null)).isZero();
        assertThat(WordCount.of("")).isZero();
    }
}
