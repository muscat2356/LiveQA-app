package com.liveqa.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.liveqa.enums.PollStatus;
import com.liveqa.enums.PollType;

class PollTest {

    @Test
    void create_DRAFTで選択肢の表示順がリスト順() {
        final User host = User.hostOf("sub-1");
        final Event event = Event.create(host, "LT会", "Ab12Cd34");
        final List<String> labels = List.of("AI", "Web", "インフラ");

        final Poll poll = Poll.create(event, "次のテーマは？", PollType.SINGLE, labels);

        assertThat(poll.getStatus()).isEqualTo(PollStatus.DRAFT);
        assertThat(poll.getType()).isEqualTo(PollType.SINGLE);
        assertThat(poll.getQuestionText()).isEqualTo("次のテーマは？");
        assertThat(poll.getEvent()).isSameAs(event);
        assertThat(poll.getOptions()).hasSize(3);
        assertThat(poll.getOptions().get(0).getLabel()).isEqualTo("AI");
        assertThat(poll.getOptions().get(0).getDisplayOrder()).isEqualTo(0);
        assertThat(poll.getOptions().get(1).getDisplayOrder()).isEqualTo(1);
        assertThat(poll.getOptions().get(2).getDisplayOrder()).isEqualTo(2);
        assertThat(poll.getOptions().get(0).getPoll()).isSameAs(poll);
    }

    @Test
    void create_MULTIもそのまま載る() {
        final User host = User.hostOf("sub-1");
        final Event event = Event.create(host, "LT会", "Ab12Cd34");

        final Poll poll = Poll.create(
                event,
                "好きな分野は？",
                PollType.MULTI,
                List.of("AI", "Web")
        );

        assertThat(poll.getType()).isEqualTo(PollType.MULTI);
        assertThat(poll.getStatus()).isEqualTo(PollStatus.DRAFT);
    }
}
