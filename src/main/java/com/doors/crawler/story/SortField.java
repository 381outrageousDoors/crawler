package com.doors.crawler.story;

public enum SortField {
    RANK, TITLE, POINTS, COMMENTS;

    static SortField fromParam(String p) {
        return switch (p == null ? "" : p) {
            case "title" -> TITLE;
            case "points" -> POINTS;
            case "comments" -> COMMENTS;
            default -> RANK;
        };
    }

    String param() {
        return name().toLowerCase();
    }

    String label() {
        return switch (this) {
            case RANK -> "rank";
            case TITLE -> "title";
            case POINTS -> "points";
            case COMMENTS -> "comment_count";
        };
    }
}
