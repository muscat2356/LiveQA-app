package com.liveqa.dto.request;

import com.liveqa.enums.EventStatus;

import jakarta.validation.constraints.NotNull;

public record EventStatusRequest(@NotNull(message = "statusは必須です。") EventStatus status) {

}
