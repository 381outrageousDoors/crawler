package com.doors.crawler.story;

/** Title word count: whitespace-separated tokens containing at least one letter or digit. */
public final class WordCount {

    private WordCount() {
    }

    public static int of(String title) {
        if (title == null || title.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String token : title.strip().split("\\s+")) {
            if (token.chars().anyMatch(Character::isLetterOrDigit)) {
                count++;
            }
        }
        return count;
    }
}
