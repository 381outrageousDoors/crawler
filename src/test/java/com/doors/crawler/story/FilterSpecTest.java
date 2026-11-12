package com.doors.crawler.story;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FilterSpecTest {

    private static Story story(long id, int rank, String title, Integer points, Integer comments, Integer words) {
        Story s = new Story();
        s.setId(id);
        s.setRank(rank);
        s.setTitle(title);
        s.setPoints(points);
        s.setCommentCount(comments);
        s.setWordCount(words);
        return s;
    }

    private static List<Story> apply(FilterSpec spec, Story... stories) {
        return spec.apply(List.of(stories));
    }

    @Test
    void filterOpTruthTable() {
        assertThat(FilterOp.GT.test(6, 5)).isTrue();
        assertThat(FilterOp.GT.test(5, 5)).isFalse();
        assertThat(FilterOp.GTE.test(5, 5)).isTrue();
        assertThat(FilterOp.LT.test(4, 5)).isTrue();
        assertThat(FilterOp.LTE.test(5, 5)).isTrue();
        assertThat(FilterOp.EQ.test(5, 5)).isTrue();
        assertThat(FilterOp.EQ.test(6, 5)).isFalse();
    }

    @Test
    void fromParamsDefaultsWhenEverythingNull() {
        FilterSpec spec = FilterSpec.fromParams(null, null, null, null, null, null);
        assertThat(spec.field()).isEqualTo(FilterField.NONE);
        assertThat(spec.op()).isEqualTo(FilterOp.GT);
        assertThat(spec.value()).isEqualTo(5);
        assertThat(spec.sort()).isEqualTo(SortField.RANK);
        assertThat(spec.desc()).isFalse();
    }

    @Test
    void fromParamsUnknownStringsFallBackToDefaults() {
        FilterSpec spec = FilterSpec.fromParams("bogus", "nope", "abc", "weird", "sideways", "nope");
        assertThat(spec.field()).isEqualTo(FilterField.NONE);
        assertThat(spec.op()).isEqualTo(FilterOp.GT);
        assertThat(spec.value()).isEqualTo(5);
        assertThat(spec.sort()).isEqualTo(SortField.RANK);
        assertThat(spec.desc()).isFalse();
    }

    @Test
    void fromParamsKeepsValidIndividualParamsIncludingNegativeValue() {
        FilterSpec spec = FilterSpec.fromParams("points", "gte", "-3", "title", "desc", null);
        assertThat(spec.field()).isEqualTo(FilterField.POINTS);
        assertThat(spec.op()).isEqualTo(FilterOp.GTE);
        assertThat(spec.value()).isEqualTo(-3);
        assertThat(spec.sort()).isEqualTo(SortField.TITLE);
        assertThat(spec.desc()).isTrue();
    }

    @Test
    void presetAskOneOverridesOtherParams() {
        FilterSpec spec = FilterSpec.fromParams("points", "lt", "99", "title", "asc", "ask1");
        assertThat(spec.field()).isEqualTo(FilterField.WORD);
        assertThat(spec.op()).isEqualTo(FilterOp.GT);
        assertThat(spec.value()).isEqualTo(5);
        assertThat(spec.sort()).isEqualTo(SortField.COMMENTS);
        assertThat(spec.desc()).isTrue();
    }

    @Test
    void presetAskTwoOverridesOtherParams() {
        FilterSpec spec = FilterSpec.fromParams("comments", "eq", "9", "rank", "asc", "ask2");
        assertThat(spec.field()).isEqualTo(FilterField.WORD);
        assertThat(spec.op()).isEqualTo(FilterOp.LTE);
        assertThat(spec.value()).isEqualTo(5);
        assertThat(spec.sort()).isEqualTo(SortField.POINTS);
        assertThat(spec.desc()).isTrue();
    }

    @Test
    void presetBogusFallsBackToIndividualParams() {
        FilterSpec spec = FilterSpec.fromParams("comments", "eq", "9", "rank", "asc", "bogus");
        assertThat(spec.field()).isEqualTo(FilterField.COMMENTS);
        assertThat(spec.op()).isEqualTo(FilterOp.EQ);
        assertThat(spec.value()).isEqualTo(9);
    }

    @Test
    void presetClearResetsToDefault() {
        FilterSpec spec = FilterSpec.fromParams("points", "gt", "9", "title", "desc", "clear");
        assertThat(spec.field()).isEqualTo(FilterField.NONE);
        assertThat(spec.op()).isEqualTo(FilterOp.GT);
        assertThat(spec.value()).isEqualTo(5);
        assertThat(spec.sort()).isEqualTo(SortField.RANK);
        assertThat(spec.desc()).isFalse();
        assertThat(spec.pollUrl()).isEqualTo("/stories");
        assertThat(spec.describe()).isNull();
    }

    @Test
    void applyWordFilterKeepsOnlyLongTitles() {
        Story shortOne = story(1, 1, "Top Story", 100, 42, 2);
        Story edge = story(2, 2, "Five words in this title", 10, 3, 5);
        Story longOne = story(3, 3, "Ask HN: what is your favorite editor", 30, 90, 7);
        assertThat(apply(new FilterSpec(FilterField.WORD, FilterOp.GT, 5, SortField.RANK, false),
                shortOne, edge, longOne)).containsExactly(longOne);
    }

    @Test
    void applyPointsFilterLte() {
        Story a = story(1, 1, "a", 10, 0, 1);
        Story b = story(2, 2, "b", 50, 0, 1);
        Story c = story(3, 3, "c", 51, 0, 1);
        assertThat(apply(new FilterSpec(FilterField.POINTS, FilterOp.LTE, 50, SortField.RANK, false), a, b, c))
                .containsExactly(a, b);
    }

    @Test
    void applyCommentsFilterEq() {
        Story a = story(1, 1, "a", 0, 3, 1);
        Story b = story(2, 2, "b", 0, 7, 1);
        assertThat(apply(new FilterSpec(FilterField.COMMENTS, FilterOp.EQ, 7, SortField.RANK, false), a, b))
                .containsExactly(b);
    }

    @Test
    void applyTreatsNullNumericFieldsAsZero() {
        Story nulls = story(1, 1, "nulls", null, null, null);
        assertThat(apply(new FilterSpec(FilterField.POINTS, FilterOp.LTE, 0, SortField.RANK, false), nulls))
                .containsExactly(nulls);
        assertThat(apply(new FilterSpec(FilterField.COMMENTS, FilterOp.EQ, 0, SortField.RANK, false), nulls))
                .containsExactly(nulls);
        assertThat(apply(new FilterSpec(FilterField.WORD, FilterOp.LTE, 0, SortField.RANK, false), nulls))
                .containsExactly(nulls);
        assertThat(apply(new FilterSpec(FilterField.POINTS, FilterOp.GT, 0, SortField.RANK, false), nulls))
                .isEmpty();
    }

    @Test
    void applySortsByTitleAscAndDesc() {
        Story cherry = story(1, 2, "Cherry", 0, 0, 1);
        Story apple = story(2, 1, "Apple", 0, 0, 1);
        assertThat(apply(new FilterSpec(FilterField.NONE, FilterOp.GT, 5, SortField.TITLE, false), cherry, apple))
                .containsExactly(apple, cherry);
        assertThat(apply(new FilterSpec(FilterField.NONE, FilterOp.GT, 5, SortField.TITLE, true), cherry, apple))
                .containsExactly(cherry, apple);
    }

    @Test
    void applySortTitlePutsNullFirst() {
        Story nullTitle = story(1, 1, null, 0, 0, 0);
        Story apple = story(2, 2, "Apple", 0, 0, 1);
        assertThat(apply(new FilterSpec(FilterField.NONE, FilterOp.GT, 5, SortField.TITLE, false), apple, nullTitle))
                .containsExactly(nullTitle, apple);
    }

    @Test
    void applySortsByCommentsDesc() {
        Story low = story(1, 1, "low", 0, 3, 1);
        Story high = story(2, 2, "high", 0, 90, 1);
        assertThat(apply(new FilterSpec(FilterField.NONE, FilterOp.GT, 5, SortField.COMMENTS, true), low, high))
                .containsExactly(high, low);
    }

    @Test
    void applySortsByRankAscByDefault() {
        Story second = story(2, 2, "second", 0, 0, 1);
        Story first = story(1, 1, "first", 0, 0, 1);
        assertThat(apply(new FilterSpec(FilterField.NONE, FilterOp.GT, 5, SortField.RANK, false), second, first))
                .containsExactly(first, second);
    }

    @Test
    void applyNeverMutatesInput() {
        Story a = story(1, 1, "a", 10, 0, 1);
        Story b = story(2, 2, "b", 20, 0, 1);
        List<Story> input = new ArrayList<>(List.of(a, b));
        new FilterSpec(FilterField.NONE, FilterOp.GT, 5, SortField.POINTS, true).apply(input);
        assertThat(input).containsExactly(a, b);
    }

    @Test
    void pollUrlIsPlainStoriesForDefaultView() {
        assertThat(FilterSpec.fromParams(null, null, null, null, null, null).pollUrl()).isEqualTo("/stories");
    }

    @Test
    void pollUrlCarriesAskOneParams() {
        FilterSpec ask1 = FilterSpec.fromParams(null, null, null, null, null, "ask1");
        assertThat(ask1.pollUrl()).isEqualTo("/stories?field=word&op=gt&value=5&sort=comments&dir=desc");
    }

    @Test
    void describeIsNullWithoutFilter() {
        assertThat(FilterSpec.fromParams(null, null, null, null, null, null).describe()).isNull();
    }

    @Test
    void describeMatchesAskOneWording() {
        FilterSpec ask1 = FilterSpec.fromParams(null, null, null, null, null, "ask1");
        assertThat(ask1.describe()).isEqualTo("Filter: word_count > 5 · Sorted by comment_count desc");
    }

    @Test
    void describeCoversSortOnlyView() {
        assertThat(FilterSpec.fromParams("none", "gt", "5", "title", "asc", null).describe())
                .isEqualTo("Sorted by title asc");
        assertThat(FilterSpec.fromParams("none", "gt", "5", "rank", "desc", null).describe())
                .isEqualTo("Sorted by rank desc");
    }

    @Test
    void paramAccessorsAreLowercaseEnumNames() {
        FilterSpec ask1 = FilterSpec.fromParams(null, null, null, null, null, "ask1");
        assertThat(ask1.fieldParam()).isEqualTo("word");
        assertThat(ask1.opParam()).isEqualTo("gt");
        assertThat(ask1.sortParam()).isEqualTo("comments");
        FilterSpec def = FilterSpec.fromParams(null, null, null, null, null, null);
        assertThat(def.fieldParam()).isEqualTo("none");
        assertThat(def.opParam()).isEqualTo("gt");
        assertThat(def.sortParam()).isEqualTo("rank");
    }
}
