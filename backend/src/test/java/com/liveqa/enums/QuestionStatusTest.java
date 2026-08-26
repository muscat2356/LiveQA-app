package com.liveqa.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * クエリ status から QuestionStatus への復元
 */
class QuestionStatusTest {

    @Test
    void form_nullと空はempty() {
        assertThat(QuestionStatus.form(null)).isEmpty();
        assertThat(QuestionStatus.form("")).isEmpty();
        assertThat(QuestionStatus.form("  ")).isEmpty();
    }

    @Test
    void form_大文字ならその値() {
        assertThat(QuestionStatus.form("OPEN")).contains(QuestionStatus.OPEN);
        assertThat(QuestionStatus.form("ANSWERED")).contains(QuestionStatus.ANSWERED);
        assertThat(QuestionStatus.form("ARCHIVED")).contains(QuestionStatus.ARCHIVED);
    }

    @Test
    void form_不正なら例外() {
        assertThatThrownBy(() -> QuestionStatus.form("open"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OPEN / ANSWERED / ARCHIVED");
    }
}
