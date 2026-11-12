package com.doors.crawler.story;

public enum FilterField {
    NONE, WORD, POINTS, COMMENTS;

    static FilterField fromParam(String p) {
        return switch (p == null ? "" : p) {
            case "word" -> WORD;
            case "points" -> POINTS;
            case "comments" -> COMMENTS;
            default -> NONE;
        };
    }

    String param() {
        return name().toLowerCase();
    }

    String label() {
        return switch (this) {
            case WORD -> "word_count";
            case POINTS -> "points";
            case COMMENTS -> "comment_count";
            case NONE -> "none";
        };
    }
}
