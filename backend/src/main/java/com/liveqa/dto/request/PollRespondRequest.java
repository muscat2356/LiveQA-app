package com.liveqa.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * ポール回答POSTの入力DTO
 * SINGLE/MULTIの件数と所属はService
 * PollRespondRequest
 * @author ichikura
 */
public record PollRespondRequest(
        @NotNull(message = "選択肢は1つ以上選んでください。")
        @NotEmpty(message = "選択肢は1つ以上選んでください。")
        @Size(max = 10, message = "選択肢は1〜10個にしてください。")
        List<@NotNull(message = "選択肢は1つ以上選んでください。") Long> optionIds
) {
}
