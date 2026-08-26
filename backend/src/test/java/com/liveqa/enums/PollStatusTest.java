package com.liveqa.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PollStatusTest {

    @Test
    void canTransitionTo_許可される遷移だけtrue() {
        assertThat(PollStatus.DRAFT.canTransitionTo(PollStatus.OPEN)).isTrue();
        assertThat(PollStatus.OPEN.canTransitionTo(PollStatus.CLOSED)).isTrue();
    }

    @Test
    void canTransitionTo_CLOSEDは終端で再開しない() {
        assertThat(PollStatus.DRAFT.canTransitionTo(PollStatus.CLOSED)).isFalse();
        assertThat(PollStatus.DRAFT.canTransitionTo(PollStatus.DRAFT)).isFalse();
        assertThat(PollStatus.OPEN.canTransitionTo(PollStatus.DRAFT)).isFalse();
        assertThat(PollStatus.OPEN.canTransitionTo(PollStatus.OPEN)).isFalse();
        assertThat(PollStatus.CLOSED.canTransitionTo(PollStatus.OPEN)).isFalse();
        assertThat(PollStatus.CLOSED.canTransitionTo(PollStatus.DRAFT)).isFalse();
        assertThat(PollStatus.CLOSED.canTransitionTo(PollStatus.CLOSED)).isFalse();
    }
}
