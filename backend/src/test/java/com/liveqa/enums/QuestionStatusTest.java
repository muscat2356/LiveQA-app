package com.liveqa.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuestionStatusTest {

    @Test
    void canTransitionTo_許可される遷移だけtrue() {
        assertThat(QuestionStatus.OPEN.canTransitionTo(QuestionStatus.ANSWERED)).isTrue();
        assertThat(QuestionStatus.OPEN.canTransitionTo(QuestionStatus.ARCHIVED)).isTrue();
        assertThat(QuestionStatus.ANSWERED.canTransitionTo(QuestionStatus.ARCHIVED)).isTrue();
        assertThat(QuestionStatus.ANSWERED.canTransitionTo(QuestionStatus.OPEN)).isFalse();
        assertThat(QuestionStatus.ARCHIVED.canTransitionTo(QuestionStatus.OPEN)).isFalse();
        assertThat(QuestionStatus.ARCHIVED.canTransitionTo(QuestionStatus.ANSWERED)).isFalse();
        assertThat(QuestionStatus.OPEN.canTransitionTo(QuestionStatus.OPEN)).isFalse();
    }
}
