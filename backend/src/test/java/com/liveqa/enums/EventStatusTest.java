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

    @Test
    void canTransitionTo_DELETEDは終端で開閉に使わない() {
        assertThat(EventStatus.DRAFT.canTransitionTo(EventStatus.DELETED)).isFalse();
        assertThat(EventStatus.OPEN.canTransitionTo(EventStatus.DELETED)).isFalse();
        assertThat(EventStatus.CLOSED.canTransitionTo(EventStatus.DELETED)).isFalse();
        assertThat(EventStatus.DELETED.canTransitionTo(EventStatus.DRAFT)).isFalse();
        assertThat(EventStatus.DELETED.canTransitionTo(EventStatus.OPEN)).isFalse();
        assertThat(EventStatus.DELETED.canTransitionTo(EventStatus.CLOSED)).isFalse();
        assertThat(EventStatus.DELETED.canTransitionTo(EventStatus.DELETED)).isFalse();
    }
}
