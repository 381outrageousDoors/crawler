package com.doors.crawler.story;

import java.util.Comparator;
import java.util.List;

/** Immutable filter/sort view of the story table, parsed from request params. */
public record FilterSpec(FilterField field, FilterOp op, int value, SortField sort, boolean desc) {

    public static FilterSpec fromParams(String field, String op, String value,
                                        String sort, String dir, String preset) {
        if ("ask1".equals(preset)) {
            return new FilterSpec(FilterField.WORD, FilterOp.GT, 5, SortField.COMMENTS, true);
        }
        if ("ask2".equals(preset)) {
            return new FilterSpec(FilterField.WORD, FilterOp.LTE, 5, SortField.POINTS, true);
        }
        int v;
        try {
            v = Integer.parseInt(value);
        } catch (NumberFormatException | NullPointerException e) {
            v = 5;
        }
        return new FilterSpec(FilterField.fromParam(field), FilterOp.fromParam(op), v,
                SortField.fromParam(sort), "desc".equals(dir));
    }

    /** Filter then sort a copy; the input list is never mutated. */
    public List<Story> apply(List<Story> in) {
        return in.stream()
                .filter(s -> field == FilterField.NONE || op.test(actual(s), value))
                .sorted(comparator())
                .toList();
    }

    private int actual(Story s) {
        return switch (field) {
            case WORD -> s.getWordCount() == null ? 0 : s.getWordCount();
            case POINTS -> s.getPoints() == null ? 0 : s.getPoints();
            case COMMENTS -> s.getCommentCount() == null ? 0 : s.getCommentCount();
            case NONE -> 0;
        };
    }

    private Comparator<Story> comparator() {
        Comparator<Story> c = switch (sort) {
            case RANK -> Comparator.comparingInt(Story::getRank);
            case TITLE -> Comparator.comparing(Story::getTitle, Comparator.nullsFirst(Comparator.naturalOrder()));
            case POINTS -> Comparator.comparingInt(s -> s.getPoints() == null ? 0 : s.getPoints());
            case COMMENTS -> Comparator.comparingInt(s -> s.getCommentCount() == null ? 0 : s.getCommentCount());
        };
        return desc ? c.reversed() : c;
    }

    /** Human-readable summary of the active view; null when the view is the default. */
    public String describe() {
        String sortPart = "Sorted by " + sort.label() + " " + (desc ? "desc" : "asc");
        if (field == FilterField.NONE) {
            if (sort == SortField.RANK && !desc) {
                return null;
            }
            return sortPart;
        }
        return "Filter: " + field.label() + " " + op.symbol() + " " + value + " · " + sortPart;
    }

    /** Poll URL carrying the current view so the 30 s poll keeps the filter alive. */
    public String pollUrl() {
        if (field == FilterField.NONE && sort == SortField.RANK && !desc) {
            return "/stories";
        }
        return "/stories?field=" + fieldParam() + "&op=" + opParam() + "&value=" + value
                + "&sort=" + sortParam() + "&dir=" + (desc ? "desc" : "asc");
    }

    public String fieldParam() {
        return field.param();
    }

    public String opParam() {
        return op.param();
    }

    public String sortParam() {
        return sort.param();
    }
}
