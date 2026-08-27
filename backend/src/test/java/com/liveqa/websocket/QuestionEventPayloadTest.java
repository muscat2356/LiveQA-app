package com.liveqa.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liveqa.dto.response.QuestionResponse;
import com.liveqa.enums.QuestionStatus;

/**
 * {@link QuestionEventPayload}のJSON変換テスト。
 *
 * @author mizumura
 */
class QuestionEventPayloadTest {

    @Test
    void from_ARCHIVEDなら公開不可項目をJSONへ含めない()
            throws JsonProcessingException {

        final QuestionResponse response = new QuestionResponse(
                42L,
                10L,
                "外部へ配信してはいけない本文",
                "Member C",
                3,
                QuestionStatus.ARCHIVED,
                false,
                Instant.parse("2026-08-27T00:00:00Z"));

        final QuestionEventPayload payload =
                QuestionEventPayload.from(response);

        final String json =
                new ObjectMapper().writeValueAsString(payload);

        assertThat(json)
                .contains(
                        "\"id\":42",
                        "\"eventId\":10",
                        "\"status\":\"ARCHIVED\"")
                .doesNotContain(
                        "\"body\"",
                        "\"displayName\"",
                        "\"upvoteCount\"",
                        "\"createdAt\"");
    }
}
