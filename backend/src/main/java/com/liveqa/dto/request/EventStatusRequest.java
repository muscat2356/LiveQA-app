package com.liveqa.dto.request;

import com.liveqa.enums.EventStatus;

import jakarta.validation.constraints.NotNull;

/**
 * ルームステータス変更PATCHの入力DTO
 * 遷移可否の検証はService
 * EventStatusRequest
 * @author ichikura
 */
public record EventStatusRequest(@NotNull(message = "statusは必須です。") EventStatus status) {

}
