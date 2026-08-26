package com.liveqa.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.liveqa.enums.PollType;

class PollVoteTest {

    @Test
    void cast_所属ポールとvoterKeyが載る() {
        final User host = User.hostOf("sub-1");
        final Event event = Event.create(host, "LT会", "Ab12Cd34");
        final Poll poll = Poll.create(
                event,
                "次のテーマは？",
                PollType.SINGLE,
                List.of("AI", "Web")
        );
        final PollOption option = poll.getOptions().get(0);

        final PollVote vote = PollVote.cast(option, "g:guest-1");

        assertThat(vote.getPoll()).isSameAs(poll);
        assertThat(vote.getPollOption()).isSameAs(option);
        assertThat(vote.getVoterKey()).isEqualTo("g:guest-1");
    }
}
