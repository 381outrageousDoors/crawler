package com.doors.crawler.story;

public enum FilterOp {
    GT, GTE, LT, LTE, EQ;

    static FilterOp fromParam(String p) {
        return switch (p == null ? "" : p) {
            case "gte" -> GTE;
            case "lt" -> LT;
            case "lte" -> LTE;
            case "eq" -> EQ;
            default -> GT;
        };
    }

    public boolean test(int actual, int expected) {
        return switch (this) {
            case GT -> actual > expected;
            case GTE -> actual >= expected;
            case LT -> actual < expected;
            case LTE -> actual <= expected;
            case EQ -> actual == expected;
        };
    }

    String param() {
        return name().toLowerCase();
    }

    String symbol() {
        return switch (this) {
            case GT -> ">";
            case GTE -> ">=";
            case LT -> "<";
            case LTE -> "<=";
            case EQ -> "=";
        };
    }
}
