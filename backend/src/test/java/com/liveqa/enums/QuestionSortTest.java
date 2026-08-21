package com.liveqa.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * クエリ sort から QuestionSort への復元
 */
class QuestionSortTest {

    @Test
    void form_nullと空はpopular() {
        assertThat(QuestionSort.form(null)).isEqualTo(QuestionSort.POPULAR);
        assertThat(QuestionSort.form("")).isEqualTo(QuestionSort.POPULAR);
        assertThat(QuestionSort.form("  ")).isEqualTo(QuestionSort.POPULAR);
    }

    @Test
    void form_popularとnewest() {
        assertThat(QuestionSort.form("popular")).isEqualTo(QuestionSort.POPULAR);
        assertThat(QuestionSort.form("newest")).isEqualTo(QuestionSort.NEWEST);
    }

    @Test
    void form_不正なら例外() {
        assertThatThrownBy(() -> QuestionSort.form("oldest"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("popular / newest");
    }
}
