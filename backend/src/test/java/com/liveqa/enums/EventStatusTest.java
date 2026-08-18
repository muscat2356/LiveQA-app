package com.liveqa.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EventStatusTest {

    @Test
    void canTransitionTo_許可される遷移だけtrue() {
        assertThat(EventStatus.DRAFT.canTransitionTo(EventStatus.OPEN)).isTrue();
        assertThat(EventStatus.DRAFT.canTransitionTo(EventStatus.CLOSED)).isFalse();
        assertThat(EventStatus.OPEN.canTransitionTo(EventStatus.CLOSED)).isTrue();
        assertThat(EventStatus.OPEN.canTransitionTo(EventStatus.DRAFT)).isFalse();
        assertThat(EventStatus.CLOSED.canTransitionTo(EventStatus.OPEN)).isTrue();
        assertThat(EventStatus.CLOSED.canTransitionTo(EventStatus.DRAFT)).isFalse();
    }
}
