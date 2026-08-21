package com.liveqa.dto.request;

import com.liveqa.enums.QuestionStatus;

import jakarta.validation.constraints.NotNull;

public record QuestionStatusRequest(
    @NotNull(message = "Statusは必須です。")
    QuestionStatus status
) {

}
