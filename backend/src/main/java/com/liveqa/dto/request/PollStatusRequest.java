package com.liveqa.dto.request;

import com.liveqa.enums.PollStatus;

import jakarta.validation.constraints.NotNull;

/**
 * ポールステータス変更PATCHの入力DTO
 * 遷移可否の検証はService
 * PollStatusRequest
 * @author ichikura
 */
public record PollStatusRequest(
        @NotNull(message = "statusは必須です。")
        PollStatus status
) {
}
